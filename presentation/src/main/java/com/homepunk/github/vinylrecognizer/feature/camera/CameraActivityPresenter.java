package com.homepunk.github.vinylrecognizer.feature.camera;

import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.os.HandlerThread;

import com.homepunk.github.vinylrecognizer.base.BasePresenter;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.CameraHelper;
import com.homepunk.github.vinylrecognizer.feature.camera.interfaces.CameraPresenter;
import com.homepunk.github.vinylrecognizer.feature.camera.interfaces.CameraView;
import com.homepunk.github.vinylrecognizer.feature.camera.helper.support.CameraSupport;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by Homepunk on 02.01.2018.
 **/

public class CameraActivityPresenter extends BasePresenter<CameraView> implements CameraPresenter {
    private File mPhotoFolder;
    private String mPhotoFileName;

    private CameraSupport mCamera;

    private RxPermissions mRxPermissions;

    @Override
    protected void init() {
        if (view != null) {
            mCamera = CameraHelper.getCamera(view.getActivity());
            view.onCameraInit(mCamera);
            mRxPermissions = new RxPermissions(view.getActivity());
        }
    }

    @Override
    public void onCameraResume() {
        mCamera.setRunningHandlerThread(new HandlerThread("BackgroundCameraThread"));
        mCamera.onCameraResume();
        mRxPermissions.request(android.Manifest.permission.CAMERA)
                .subscribe(isGranted -> {
                    if (isGranted) {
                        mCamera.open();
                    }
                });
    }

    @Override
    public void onCameraPause() {
        mCamera.onCameraPause();
    }

    @Override
    public void onCaptureClick() {
        checkWriteExternalStoragePermission();
        createPhotoFolder();
        createPhotoFileName();
        mCamera.capture(image -> {
            Completable.fromAction(() -> new ImageSaver(view.getContext(), image, mPhotoFileName).run())
                    .subscribeOn(Schedulers.newThread())
                    .subscribe(() -> {
                        Timber.i("ok");
                    }, Throwable::printStackTrace);
        });
    }

    private void checkWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!mRxPermissions.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                mRxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(isGranted -> {
                            if (isGranted) {
                                Timber.i("WRITE_EXTERNAL_STORAGE permission granted");
                            }
                        });
            }
        } else {
            Timber.i("WRITE_EXTERNAL_STORAGE permission granted");
        }
    }

    private File createPhotoFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "Vinyl_cover_" + timestamp + "_";
        File photoFile = null;
        try {
            if (mPhotoFolder != null) {
                photoFile = File.createTempFile(prepend, ".jpg", mPhotoFolder);
            } else {
                Timber.i("Folder doesn't exists");
            }
            mPhotoFileName = photoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return photoFile;
    }

    private void createPhotoFolder() {
        File photoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mPhotoFolder = new File(photoFile, "vinyl_recognizer");
        if (!mPhotoFolder.exists()) {
            mPhotoFolder.mkdirs();
        }
    }

}
