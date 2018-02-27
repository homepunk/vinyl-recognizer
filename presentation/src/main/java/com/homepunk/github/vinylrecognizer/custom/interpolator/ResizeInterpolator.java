package com.homepunk.github.vinylrecognizer.custom.interpolator;

import android.view.animation.Interpolator;

/**
 * Created by Homepunk on 20.02.2018.
 **/

// Resize interpolator to create smooth effect on strip according to inspiration design
// This is like improved accelerated and decelerated interpolator
public class ResizeInterpolator implements Interpolator {

    // Spring factor
    private float mFactor;
    // Check whether side we move
    private boolean mIsLeftDirection;

    public float getFactor() {
        return mFactor;
    }

    public void setFactor(final float factor) {
        mFactor = factor;
    }

    @Override
    public float getInterpolation(final float input) {
        return (float) (mIsLeftDirection ?
                (1.0F - Math.pow((1.0F - input), 2.0F * mFactor)) : (Math.pow(input, 2.0F * mFactor)));
    }

    public float getInterpolation(final float input, final boolean resizeIn) {
        mIsLeftDirection = resizeIn;
//        Timber.i("mIsLeftDirection = " + mIsLeftDirection + " interpolator value = " + getInterpolation(input));
        return getInterpolation(input);
    }
}