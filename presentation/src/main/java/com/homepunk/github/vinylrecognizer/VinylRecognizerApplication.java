package com.homepunk.github.vinylrecognizer;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by Homepunk on 04.01.2018.
 **/

public class VinylRecognizerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
