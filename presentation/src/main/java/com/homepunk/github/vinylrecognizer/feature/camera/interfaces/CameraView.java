package com.homepunk.github.vinylrecognizer.feature.camera.interfaces;

import com.homepunk.github.vinylrecognizer.base.interfaces.View;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraSupport;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public interface CameraView extends View {
    void onCameraInit(CameraSupport camera);
}
