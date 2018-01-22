package com.homepunk.github.vinylrecognizer.feature.camera.helper.support.listener.wrapper;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

/**
 * Created by Homepunk on 22.01.2018.
 **/

public class SurfaceTextureListenerWrapper implements TextureView.SurfaceTextureListener {
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
