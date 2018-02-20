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
    private boolean mResizeIn;

    public float getFactor() {
        return mFactor;
    }

    public void setFactor(final float factor) {
        mFactor = factor;
    }

    @Override
    public float getInterpolation(final float input) {
        if (mResizeIn) return (float) (1.0F - Math.pow((1.0F - input), 2.0F * mFactor));
        else return (float) (Math.pow(input, 2.0F * mFactor));
    }

    public float getResizeInterpolation(final float input, final boolean resizeIn) {
        mResizeIn = resizeIn;
        return getInterpolation(input);
    }
}