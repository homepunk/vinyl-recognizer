package com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support;

import android.os.HandlerThread;
import android.view.TextureView;

import com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support.listener.CaptureListener;

/**
 * Created by Homepunk on 03.01.2018.
 **/

public interface CameraSupport {
    void setPreview(TextureView view);

    void open();

    void onCameraResume();

    void onCameraPause();

    void capture(CaptureListener listener);

    void setRunningHandlerThread(HandlerThread handlerThread);
}
