package com.homepunk.github.vinylrecognizer.feature.camera.helper;

import android.app.Activity;
import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraNew;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraOld;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraSupport;

/**
 * Created by Homepunk on 15.01.2018.
 **/

public class CameraHelper {
    public static CameraSupport getCamera(Activity activity) {
        CameraSupport camera;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera = new CameraNew((CameraManager) activity.getSystemService(Context.CAMERA_SERVICE), activity.getWindowManager().getDefaultDisplay());
        } else {
            camera = new CameraOld();
        }
        return camera;
    }
}
