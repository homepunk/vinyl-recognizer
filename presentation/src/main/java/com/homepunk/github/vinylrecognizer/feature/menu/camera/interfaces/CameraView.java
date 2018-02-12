package com.homepunk.github.vinylrecognizer.feature.menu.camera.interfaces;

import android.support.v4.app.FragmentActivity;

import com.homepunk.github.vinylrecognizer.common.interfaces.View;
import com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support.CameraSupport;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public interface CameraView extends View {
    void setCameraPreview(CameraSupport camera);

    FragmentActivity getFragmentActivity();
}
