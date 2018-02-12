package com.homepunk.github.vinylrecognizer.feature.menu.camera;

import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.homepunk.github.vinylrecognizer.R;
import com.homepunk.github.vinylrecognizer.feature.menu.camera.helper.support.CameraSupport;
import com.homepunk.github.vinylrecognizer.feature.menu.camera.interfaces.CameraPresenter;
import com.homepunk.github.vinylrecognizer.feature.menu.camera.interfaces.CameraView;
import com.homepunk.github.vinylrecognizer.util.DimensionUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraFragment extends Fragment implements CameraView {
    @BindView(R.id.activity_camera_texture_view) TextureView textureView;
    @BindView(R.id.activity_camera_rectangle) View rectangleView;

    private CameraPresenter mPresenter;

    public static CameraFragment newInstance() {
        Bundle args = new Bundle();
        CameraFragment fragment = new CameraFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_camera, container, false);
        ButterKnife.bind(this, root);
        mPresenter = new CameraFragmentPresenter();
        mPresenter.bind(this);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
        ViewGroup.LayoutParams layoutParams = rectangleView.getLayoutParams();
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        int margins = (int) DimensionUtil.convertDpToPixel(getContext(), 32);
        layoutParams.width = displaySize.x - margins;
        layoutParams.height = displaySize.x - margins;
        rectangleView.setLayoutParams(layoutParams);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.onCameraResume();
    }

    @Override
    public void onPause() {
        mPresenter.onCameraPause();
        mPresenter.terminate();
        super.onPause();
    }

    @OnClick(R.id.activity_camera_photo_button)
    public void onCaptureImageClick() {
        mPresenter.onCaptureClick();
    }

    @Override
    public void setCameraPreview(CameraSupport camera) {
        camera.setPreview(textureView);
    }

    @Override
    public FragmentActivity getFragmentActivity() {
        return getActivity();
    }
}
