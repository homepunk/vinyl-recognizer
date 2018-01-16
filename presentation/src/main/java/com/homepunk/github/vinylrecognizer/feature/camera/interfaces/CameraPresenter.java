package com.homepunk.github.vinylrecognizer.feature.camera.interfaces;

import com.homepunk.github.vinylrecognizer.base.interfaces.Presenter;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public interface CameraPresenter extends Presenter<CameraView> {
    void onCameraResume();

    void onCameraPause();

    void onCaptureClick();
}
