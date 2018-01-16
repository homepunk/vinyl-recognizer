package com.homepunk.github.vinylrecognizer.feature.camera.helper.support;

import android.os.HandlerThread;
import android.view.SurfaceHolder;

import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.listener.CaptureListener;

/**
 * Created by Homepunk on 03.01.2018.
 **/

public interface CameraSupport {
    void init(SurfaceHolder holder);

    void open();

    void onCameraResume();

    void onCameraPause();

    void capture(CaptureListener listener);

    void setRunningHandlerThread(HandlerThread handlerThread);
}
