package com.homepunk.github.vinylrecognizer.feature.camera;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import com.homepunk.github.vinylrecognizer.R;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraSupport;
import com.homepunk.github.vinylrecognizer.feature.camera.interfaces.CameraPresenter;
import com.homepunk.github.vinylrecognizer.feature.camera.interfaces.CameraView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends AppCompatActivity implements CameraView {
    @BindView(R.id.texture_view) TextureView textureView;

    private CameraPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        mPresenter = new CameraActivityPresenter();
    }

    @OnClick(R.id.vinyl)
    public void onCaptureImageClick() {
        mPresenter.onCaptureClick();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.bind(this);
        mPresenter.onCameraResume();
    }

    @Override
    protected void onPause() {
        mPresenter.onCameraPause();
        mPresenter.terminate();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onCameraInit(CameraSupport camera) {
        camera.setPreview(textureView);
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Activity getActivity() {
        return this;
    }
}
