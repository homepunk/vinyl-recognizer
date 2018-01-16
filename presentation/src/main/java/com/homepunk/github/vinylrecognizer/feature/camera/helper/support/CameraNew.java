package com.homepunk.github.vinylrecognizer.feature.camera.helper.support;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
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
import android.view.SurfaceHolder;

import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.listener.CaptureListener;

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
    private int mTotalRotation;
    private String mCameraId;

    private boolean isCameraOpened;

    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;

    private Size mPreviewSize;
    private Size mImageSize;

    private ImageReader mImageReader;

    private Handler mRunningHandler;
    private HandlerThread mRunningHandlerThread;

    private Display mDeviceDisplay;
    private SurfaceHolder mSurfaceHolder;

    private CaptureListener mCaptureListener;

    public CameraNew(CameraManager cameraManager, Display display) {
        mCameraManager = cameraManager;
        mDeviceDisplay = display;
        mCaptureState = STATE_PREVIEW;
    }

    @Override
    public void init(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void setRunningHandlerThread(HandlerThread handlerThread) {
        mRunningHandlerThread = handlerThread;
    }

    @Override
    public void open() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    Timber.i("surfaceCreated");

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    Timber.i("surfaceChanged w: " + width + " h: " + height);
                    if (mPreviewSize == null
                            || mPreviewSize.getWidth() != width || mPreviewSize.getHeight() != height) {
                        setupCamera(width, height);
                    } else if (!isCameraOpened) {
                        openCamera();
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    Timber.i("surfaceDestroyed");
                }
            });
        }
    }

    @Override
    public void onCameraResume() {
        startRunningThread();
    }

    @Override
    public void onCameraPause() {
        closeCamera();
        stopRunningThread();
    }

    @Override
    public void capture(CaptureListener listener) {
        mCaptureListener = listener;
        lockFocus();
    }

    private void setupCamera(int width, int height) {
        Timber.i("Setting up camera");
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                int displayRotation = mDeviceDisplay.getRotation();
                mTotalRotation = getTotalDeviceRotation(characteristics, displayRotation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (mPreviewSize == null && swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                StreamConfigurationMap map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = chooseBigEnough(map.getOutputSizes(SurfaceHolder.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseBigEnough(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(imageReader -> {
                    Image image = imageReader.acquireLatestImage();
                    Timber.i("OnImageAvailableListener " + image.getWidth() + " " + image.getHeight());
                    if (mCaptureListener != null) {
                        mCaptureListener.onImageCaptured(image);
                    }
                    //                    mBackgroundHandler.post(new ImageSaver(this, image, mPhotoFileName));
                }, mRunningHandler);
                Timber.i("Setting chosen preview size: w: " + mPreviewSize.getWidth() + " h: " + mPreviewSize.getHeight());
                mSurfaceHolder.setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                this.mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        Timber.i("Opening camera");
        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    isCameraOpened = true;
                    Timber.i("CameraDevice  is opened " + cameraDevice.getId());
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Timber.i("CameraDevice is disconnected");
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Timber.i("CameraDevice opened with error");
                    cameraDevice.close();
                    mCameraDevice = null;
                }
            }, mRunningHandler);
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
            isCameraOpened = false;
        }
    }

    private void startPreview() {
        try {
            Surface surface = mSurfaceHolder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mRunningHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Timber.e("onConfigureFailed");
                }
            }, mRunningHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void startStillCaptureRequest() {
        Timber.i("startStillCaptureRequest");
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mTotalRotation);

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
            }, mRunningHandler);
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
            return Collections.min(bigEnough, (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight()));
        } else {
            Timber.i("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private int getTotalDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void startRunningThread() {
        if (mRunningHandlerThread == null) {
            throw new RuntimeException("Please, set running handler thread firstly");
        }
        mRunningHandlerThread.start();
        mRunningHandler = new Handler(mRunningHandlerThread.getLooper());
        Timber.i("Background thread started");
    }

    private void stopRunningThread() {
        mRunningHandlerThread.quitSafely();
        try {
            mRunningHandlerThread.join();
            mRunningHandlerThread = null;
            mRunningHandler = null;
            Timber.i("Background thread stopped");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
