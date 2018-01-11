package com.homepunk.github.vinylrecognizer.custom.surface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Size;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import timber.log.Timber;

/**
 * Created by Homepunk on 09.01.2018.
 **/

public class MeasuredSurfaceView extends SurfaceView {
    private Size mPreviewSize;
    private Size[] mSupportedPreviewSizes;

    public MeasuredSurfaceView(Context context) {
        super(context);
    }

    public MeasuredSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MeasuredSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MeasuredSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = chooseBigEnoughSize(mSupportedPreviewSizes, width, height);
        } else {
            setMeasuredDimension(width, height);
        }

        if (mPreviewSize != null) {
            float ratio;
            if (mPreviewSize.getHeight() >= mPreviewSize.getWidth())
                ratio = (float) mPreviewSize.getHeight() / (float) mPreviewSize.getWidth();
            else
                ratio = (float) mPreviewSize.getWidth() / (float) mPreviewSize.getHeight();

            // One of these methods should be used, second method squishes preview slightly
            setMeasuredDimension(width, (int) (width * ratio));
            //        setMeasuredDimension((int) (width * ratio), height);
        }
    }

    @SuppressLint("NewApi")
    static Size chooseBigEnoughSize(Size[] choices, int width, int height) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option : choices) {
            int optionWidth = option.getWidth();
            int optionHeight = option.getHeight();
            if (optionWidth >= width && optionHeight >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Timber.i("Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @SuppressLint("NewApi")
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
    public void setSupportedPreviewSizes(Size[] mSupportedPreviewSizes) {
        this.mSupportedPreviewSizes = mSupportedPreviewSizes;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private  Size getOptimalSize(Size[] choices, int width, int height) {
 /*       List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width &&
                    option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new ComapreSizeByArea());
        } else {
            return choices[0];
        }*/
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double) height / width;

        if (choices == null) {
            return null;
        }

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Size size : choices) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : choices) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
