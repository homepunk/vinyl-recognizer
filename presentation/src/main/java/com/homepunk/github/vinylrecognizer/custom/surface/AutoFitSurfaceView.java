package com.homepunk.github.vinylrecognizer.custom.surface;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

import timber.log.Timber;

/**
 * Created by Homepunk on 10.01.2018.
 **/

public class AutoFitSurfaceView extends SurfaceView {

    private static final String TAG = "AutoFitSurfaceView";

    private int mRatioWidth = 0;
    private double mRatioHeight = 0;

    public AutoFitSurfaceView(Context context) {
        this(context, null);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, double height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            Timber.i(String.format("aspect ratio is 0 x 0 (uninitialized), setting measured" + " dimension to: %d x %d", width, height));
            Timber.i("Measured w: " + getMeasuredWidth() + " h: " + getMeasuredHeight());
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                Timber.i(String.format("setting measured dimension to %d x %d", width, height));
                setMeasuredDimension(width, (int) (width * mRatioHeight / mRatioWidth));
                Timber.i("After setMeasuredDimension w: " + getMeasuredWidth() + " h: " + getMeasuredHeight());
            } else {
                Timber.i(String.format("setting measured dimension to %d x %d", width, height));
                setMeasuredDimension((int) (height * mRatioWidth / mRatioHeight), height);
                Timber.i("After setMeasuredDimension w: " + getMeasuredWidth() + " h: " + getMeasuredHeight());
            }
        }
    }

}
