package com.homepunk.github.vinylrecognizer.feature.menu.camera.interfaces;

import com.homepunk.github.vinylrecognizer.common.interfaces.Presenter;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public interface CameraPresenter extends Presenter<CameraView> {
    void onCameraResume();

    void onCameraPause();

    void onCaptureClick();
}
