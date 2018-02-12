package com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support;

import android.hardware.Camera;
import android.os.HandlerThread;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support.listener.CaptureListener;

import java.io.IOException;

/**
 * Created by Homepunk on 03.01.2018.
 **/

@SuppressWarnings("deprecation")
public class CameraOld implements CameraSupport {
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private SurfaceHolder.Callback surfaceHolderCallback;


    @Override
    public void onCameraResume() {

    }

    @Override
    public void setPreview(TextureView view) {

    }

    @Override
    public void open() {

    }

    @Override
    public void onCameraPause() {

    }

    @Override
    public void capture(CaptureListener listener) {

    }

    @Override
    public void setRunningHandlerThread(HandlerThread handlerThread) {

    }

    public CameraSupport open(final int cameraId) {
        this.camera = Camera.open(cameraId);
        surfaceHolderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (camera != null) {
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
                camera.stopPreview();
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        };
        surfaceHolder.addCallback(surfaceHolderCallback);
        return this;
    }

    public int getOrientation(final int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info.orientation;
    }

    public void init(SurfaceView view) {
        this.surfaceView = view;
    }

    private void setDisplayOrientation(int degrees) {
        if (camera != null) {
            camera.setDisplayOrientation(degrees);
        }
    }

}
