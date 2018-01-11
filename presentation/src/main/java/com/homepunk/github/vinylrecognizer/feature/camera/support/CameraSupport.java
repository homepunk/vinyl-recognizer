package com.homepunk.github.vinylrecognizer.feature.camera.support;

import android.view.SurfaceView;

/**
 * Created by Homepunk on 03.01.2018.
 **/

public interface CameraSupport {
    CameraSupport open(int cameraId);

    int getOrientation(final int cameraId);

    void setSurfaceView(SurfaceView surfaceView);

    void setDisplayOrientation(int degrees);
}
