package com.homepunk.github.vinylrecognizer.feature.camera;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
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
import android.view.Window;
import android.view.WindowManager;

import com.homepunk.github.vinylrecognizer.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

import static android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP;

public class CameraActivity extends AppCompatActivity {
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private String cameraId;

    private CaptureRequest previewCaptureRequest;
    private CaptureRequest.Builder previewCaptureRequestBuilder;

    private CameraCaptureSession cameraCaptureSession;

    private CameraDevice cameraDevice;
    private CameraManager cameraManager;

    private SurfaceView surfaceView;

    private Handler cameraBackgroundHandler;
    private HandlerThread cameraBackgroundHandlerThread;
    private Size mPreviewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraBackgroundThread();
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(android.Manifest.permission.CAMERA)
                .subscribe(isGranted -> {
                    if (isGranted) {
                        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                            @Override
                            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                                Timber.i("surfaceCreated");

                            }

                            @Override
                            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                                Timber.i("surfaceChanged w: " + width + " h: " + height);
                                Rect frame = holder.getSurfaceFrame();
                                Timber.i("size of frame w: " + frame.width() + " h: " + frame.height());
                                setupCamera(width, height);
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
        stopCameraBackgroundThread();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocas) {
        super.onWindowFocusChanged(hasFocas);
        View decorView = getWindow().getDecorView();
        if(hasFocas) {
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
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @SuppressLint("NewApi")
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraActivity.this.cameraDevice = cameraDevice;
                    Timber.i("CameraDevice.StateCallback onOpened " + cameraDevice.getId());
                    createCameraPreviewSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Timber.i("CameraDevice.StateCallback onDisconnected");
                    cameraDevice.close();
                    CameraActivity.this.cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Timber.i("CameraDevice.StateCallback onError");
                    cameraDevice.close();
                    CameraActivity.this.cameraDevice = null;
                }
            }, cameraBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @SuppressLint("NewApi")
    private void setupCamera(int width, int height) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = map.getOutputSizes(SurfaceHolder.class);

                int displayRotation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = getDeviceRottation(characteristics, displayRotation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (mPreviewSize == null && swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }

                Timber.i("w: " + rotatedWidth + " h: " + rotatedHeight);
                mPreviewSize = chooseBigEnough(sizes, rotatedWidth, rotatedHeight);
                Timber.i("big enough: w: " + mPreviewSize.getWidth() + " h: " + mPreviewSize.getHeight());
                surfaceView.getHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                this.cameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void createCameraPreviewSession() {
        try {
            Surface surface = surfaceView.getHolder().getSurface();
            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewCaptureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) {
                        return;
                    }
                    try {
                        previewCaptureRequest = previewCaptureRequestBuilder.build();
                        CameraActivity.this.cameraCaptureSession = cameraCaptureSession;
                        CameraActivity.this.cameraCaptureSession.setRepeatingRequest(previewCaptureRequest, new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber);

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Timber.e("onConfigureFailed");
                }
            }, cameraBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startCameraBackgroundThread() {
        cameraBackgroundHandlerThread = new HandlerThread("VinylRecognizerCameraBackgroundThread");
        cameraBackgroundHandlerThread.start();
        cameraBackgroundHandler = new Handler(cameraBackgroundHandlerThread.getLooper());
    }

    private void stopCameraBackgroundThread() {
        cameraBackgroundHandlerThread.quitSafely();
        try {
            cameraBackgroundHandlerThread.join();
            cameraBackgroundHandlerThread = null;
            cameraBackgroundHandler = null;
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
    private int getDeviceRottation(CameraCharacteristics characteristics, int deviceOrientation) {
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
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
