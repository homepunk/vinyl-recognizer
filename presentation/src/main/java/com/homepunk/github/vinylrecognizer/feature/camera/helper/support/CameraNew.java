package com.homepunk.github.vinylrecognizer.feature.camera.helper.support;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;

import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.listener.CaptureListener;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.listener.wrapper.SurfaceTextureListenerWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;

/**
 * Created by Homepunk on 03.01.2018.
 **/

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("MissingPermission")
public class CameraNew implements CameraSupport {
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAIT_LOCK = 1;
    private static SparseIntArray ORIENTATIONS;

    static {
        ORIENTATIONS = new SparseIntArray();
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private int mCaptureState;
    private String mCameraId;
    private boolean mIsCameraOpened;

    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;

    private Size mPreviewSize;

    private int mDeviceRotation;
    private ImageReader mImageReader;

    private Handler mCameraHandler;
    private HandlerThread mCameraHandlerThread;

    private Display mDefaultDisplay;
    private TextureView mTextureView;
    private CaptureListener mCaptureListener;
    private final TextureView.SurfaceTextureListener mOnTextureAvailableListener = new SurfaceTextureListenerWrapper() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Timber.i("onSurfaceTextureAvailable: w = " + width + " h = " + height);
            startPreview(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Timber.i("onSurfaceTextureSizeChanged: w = " + width + " h = " + height);
            configureTransform(width, height);
        }
    };

    public CameraNew(CameraManager cameraManager, Display display) {
        mCameraManager = cameraManager;
        mDefaultDisplay = display;
        mCaptureState = STATE_PREVIEW;
    }

    @Override
    public void setPreview(TextureView view) {
        mTextureView = view;
    }

    @Override
    public void setRunningHandlerThread(HandlerThread handlerThread) {
        mCameraHandlerThread = handlerThread;
    }

    @Override
    public void open() {
        if (mTextureView.isAvailable()) {
            startPreview(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mOnTextureAvailableListener);
        }
    }

    @Override
    public void onCameraResume() {
        startCameraThread();
    }

    @Override
    public void onCameraPause() {
        closeCamera();
        stopCameraThread();
    }

    @Override
    public void capture(CaptureListener listener) {
        mCaptureListener = listener;
        lockFocus();
    }

    private void startPreview(int width, int height) {
        configureOutputs(width, height);
        configureTransform(width, height);
        openCamera();
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param width  The width of `mTextureView`
     * @param height The height of `mTextureView`
     */
    private void configureTransform(int width, int height) {
        if (mTextureView == null || mPreviewSize == null) {
            return;
        }

        Timber.i("Configuring transformation...");
        int rotation = mDefaultDisplay.getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, width, height);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) height / mPreviewSize.getHeight(),
                    (float) width / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void configureOutputs(int width, int height) {
        Timber.i("Configuring camera preview...");
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                int rotatedWidth = width;
                int rotatedHeight = height;
                mDeviceRotation = getDeviceRotation(characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION), mDefaultDisplay.getRotation());
                boolean swapDimensions = mDeviceRotation == 90 || mDeviceRotation == 270;
                if (swapDimensions) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                StreamConfigurationMap map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP);
                Size imageSize = chooseBigEnough(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                initImageReader(imageSize);
                mPreviewSize = chooseBigEnough(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initImageReader(final Size imageSize) {
        mImageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(imageReader -> {
            Image image = imageReader.acquireLatestImage();
            Timber.i("OnImageAvailableListener " + image.getWidth() + " " + image.getHeight());
            if (mCaptureListener != null) {
                mCaptureListener.onImageCaptured(image);
            }
        }, mCameraHandler);
    }

    private void openCamera() {
        Timber.i("Opening camera..");
        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    mIsCameraOpened = true;
                    Timber.i("CameraDevice  is opened " + cameraDevice.getId());
                    startPreviewCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Timber.i("CameraDevice is disconnected");
                    cameraDevice.close();
                    mIsCameraOpened = false;
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Timber.i("CameraDevice opened with error");
                    cameraDevice.close();
                    mIsCameraOpened = false;
                    mCameraDevice = null;
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            Timber.i("Close camera");
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
            mCameraDevice.close();
            mCameraDevice = null;
            mTextureView = null;
        }
    }


    private void startPreviewCaptureSession() {
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);

            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Timber.e("onConfigureFailed");
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startStillCaptureRequest() {
        Timber.i("startStillCaptureRequest");
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mDeviceRotation);

            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
//                    createPhotoFileName();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void lockFocus() {
        mCaptureState = STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        Timber.e("Focus locked");
        try {
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Timber.i("Capture completed");
                    switch (mCaptureState) {
                        case STATE_PREVIEW: {
                            //  Do nothing
                            break;
                        }
                        case STATE_WAIT_LOCK: {
                            mCaptureState = STATE_PREVIEW;
                            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                            if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                                    afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                                startStillCaptureRequest();
                            }
                            break;
                        }
                    }
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values.
     *
     * @param choices The list of sizes that the camera supports for the intended output class
     * @param width   The minimum desired width
     * @param height  The minimum desired height
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private Size chooseBigEnough(Size[] choices, int width, int height) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            int optionWidth = option.getWidth();
            int optionHeight = option.getHeight();
            if (optionWidth >= width && optionHeight >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            Size optimal = Collections.min(bigEnough, (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight()));
            Timber.i("Chosen preview size: w: " + optimal.getWidth() + " h: " + optimal.getHeight());
            return optimal;
        } else {
            Timber.i("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private int getDeviceRotation(int sensorOrientation, int deviceOrientation) {
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void startCameraThread() {
        if (mCameraHandlerThread == null) {
            throw new RuntimeException("Please, set running handler thread firstly");
        }
        mCameraHandlerThread.start();
        mCameraHandler = new Handler(mCameraHandlerThread.getLooper());
        Timber.i("Background thread started");
    }

    private void stopCameraThread() {
        mCameraHandlerThread.quitSafely();
        try {
            mCameraHandlerThread.join();
            mCameraHandlerThread = null;
            mCameraHandler = null;
            Timber.i("Background thread stopped");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
