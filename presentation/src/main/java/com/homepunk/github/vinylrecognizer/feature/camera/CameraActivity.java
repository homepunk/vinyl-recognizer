package com.homepunk.github.vinylrecognizer.feature.camera;

import android.Manifest;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.homepunk.github.vinylrecognizer.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;

public class CameraActivity extends AppCompatActivity {
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

    @BindView(R.id.surfaceView)
    SurfaceView mSurfaceView;

    private int mCaptureState = STATE_PREVIEW;
    private int mTotalRotation;

    private RxPermissions mRxPermissions;

    private String mCameraId;
    private String mPhotoFileName;
    private File mPhotoFolder;

    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundHandlerThread;

    private Size mPreviewSize;
    private Size mImageSize;

    private ImageReader mImageReader;
    private boolean mIsPreviewStarted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        mRxPermissions = new RxPermissions(this);
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    }

    @OnClick(R.id.vinyl)
    public void onCaptureImageClick() {
        checkWriteExternalStoragePermission();
        createPhotoFolder();
        lockFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(android.Manifest.permission.CAMERA)
                .subscribe(isGranted -> {
                    if (isGranted) {
                        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                                Timber.i("surfaceCreated");

                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                Timber.i("surfaceChanged w: " + width + " h: " + height);
                                setupCamera(width, height);
                                closeCamera();
                                openCamera();
                            }

                            @Override
                            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                                Timber.i("surfaceDestroyed");
                            }
                        });

                    }
                });
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @SuppressLint({"NewApi", "MissingPermission"})
    private void openCamera() {
        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    Timber.i("CameraDevice.StateCallback onOpened " + cameraDevice.getId());
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Timber.i("CameraDevice.StateCallback onDisconnected");
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Timber.i("CameraDevice.StateCallback onError");
                    cameraDevice.close();
                    mCameraDevice = null;
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    @SuppressLint("NewApi")
    private void setupCamera(int width, int height) {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
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
                    mBackgroundHandler.post(new ImageSaver(this, image, mPhotoFileName));
                }, mBackgroundHandler);
                Timber.i("big enough: w: " + mPreviewSize.getWidth() + " h: " + mPreviewSize.getHeight());
                mSurfaceView.getHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                this.mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void startPreview() {
        try {
            Surface surface = mSurfaceView.getHolder().getSurface();
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
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Timber.e("onConfigureFailed");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
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
                    createPhotoFileName();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
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
                    }                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundHandlerThread = new HandlerThread("VinylRecognizerCameraBackgroundThread");
        mBackgroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackgroundHandlerThread.getLooper());
        Timber.i("Background thread started");
    }

    private void stopBackgroundThread() {
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread = null;
            mBackgroundHandler = null;
            Timber.i("Background thread stopped");
        } catch (InterruptedException e) {
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
    @SuppressLint("NewApi")
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
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Timber.i("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @SuppressLint("NewApi")
    private int getTotalDeviceRotation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private void checkWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!mRxPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mRxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(isGranted -> {
                            if (isGranted) {
                                Timber.i("WRITE_EXTERNAL_STORAGE permission granted");
                            }
                        });
            }
        } else {
            Timber.i("WRITE_EXTERNAL_STORAGE permission granted");
        }
    }

    private File createPhotoFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "Vinyl_cover_" + timestamp + "_";
        File photoFile = null;
        try {
            if (mPhotoFolder != null) {
                photoFile = File.createTempFile(prepend, ".jpg", mPhotoFolder);
            } else {
                Toast.makeText(this, "Folder doesn't exists", Toast.LENGTH_SHORT).show();
            }
            mPhotoFileName = photoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return photoFile;
    }

    private void createPhotoFolder() {
        File photoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mPhotoFolder = new File(photoFile, "vinyl_recognizer");
        if (!mPhotoFolder.exists()) {
            mPhotoFolder.mkdirs();
        }
    }

    @SuppressLint("NewApi")
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
