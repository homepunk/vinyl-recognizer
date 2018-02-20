package com.homepunk.github.vinylrecognizer.custom.square;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.homepunk.github.vinylrecognizer.R;

import timber.log.Timber;

/**
 * Created by Homepunk on 13.02.2018.
 **/

public class SquareView extends View {
    public static final int RECTANGLE_SIDE_COUNT = 4;

    public int mCurrentRectangleSide = 0;

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mMargin;

    private float mStartX = 0;
    private float mStartY = 0;
    private float mEndX = 0;
    private float mEndY = 0;
    private float mCurrentDrawPosition;
    private float mEndPosition;

    private boolean isHorizontalLine;

    private boolean mAnimate;
    private int mAnimationDuration;
    private long mAnimationStartTime = 0;
    private TimeInterpolator mAnimationInterpolator;

    private Path mSquarePath;
    private int mMaxWidth;

    public SquareView(Context context) {
        super(context);
        init(context, null);
    }

    public SquareView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        mAnimationInterpolator = new AccelerateDecelerateInterpolator();

        readAttributesAndSetupFields(context, attrs);

        isHorizontalLine = true;
        setupPaint();
        mSquarePath = new Path();
    }

    @NonNull
    private void readAttributesAndSetupFields(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SquareView, 0, 0);

        try {
            mMargin = typedArray.getDimension(R.styleable.SquareView_margin, 0);
            mAnimate = typedArray.getBoolean(R.styleable.SquareView_animate, false);
//            mAnimationDuration = typedArray.getInt(R.styleable.SquareView_animationDuration, 0);
        } finally {
            typedArray.recycle();
        }
    }

    private void setupPaint() {
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Timber.i("OnDraw");
        if (mAnimationStartTime == 0) {
            mAnimationStartTime = System.currentTimeMillis();
        }
        if (mCurrentRectangleSide == 0) {
            mSquarePath.moveTo(mMargin, mMargin);
            setupNextLine();
        }

        updatePath();
        canvas.drawPath(mSquarePath, mPaint);
//        if (isHorizontalLine) {
//            mSquarePath.moveTo(mStartX, mStartY);
//            mSquarePath.lineTo(mAnimate ? getCurrentDrawPosition() : mEndX, mEndY);
//            canvas.drawPath(mSquarePath, mPaint);
//            canvas.drawLine(
//                    mStartX,
//                    mStartY,
//                    mAnimate ? getCurrentDrawPosition() : mEndX,
//                    mEndY,
//                    mPaint);
//        } else {
//            canvas.drawLine(
//                    mStartX,
//                    mStartY,
//                    mEndX,
//                    mAnimate ? getCurrentDrawPosition() : mEndY,
//                    mPaint);
//        }


        if (mAnimate) {
            if (mCurrentDrawPosition < mEndPosition) {
                invalidate();
                Timber.i("OnDraw invalidate");
            } else if (mCurrentRectangleSide != RECTANGLE_SIDE_COUNT) {
                setupNextLine();
                invalidate();
            }
        }
    }

    private void updatePath() {
        if (isHorizontalLine) {
            mSquarePath.lineTo(mAnimate ? getCurrentDrawPosition() : mEndX, mEndY);
        } else {
            mSquarePath.lineTo(mEndX, mAnimate ? getCurrentDrawPosition() : mEndY);
        }
    }

    private void setupNextLine() {
        switch (mCurrentRectangleSide) {
            case 0: {
                isHorizontalLine = true;
                mEndY = mMargin;
                mEndX = mMaxWidth - 2 * mMargin;
                mEndPosition = mEndX;
                break;
            }
            case 1: {
                isHorizontalLine = false;
                mEndY = mEndX;
                mEndPosition = mEndY;
                mCurrentDrawPosition = mMargin;
                break;
            }
            case 2: {
                isHorizontalLine = true;
//                mStartY = mEndY;
//                mCurrentDrawPosition = mEndX;
                mEndX = mMargin;
                mEndPosition = mEndX;
                mCurrentDrawPosition = mMaxWidth - 2 * mMargin;
                break;
            }
            case 3: {
                isHorizontalLine = false;
//                mCurrentDrawPosition = mMargin;
                mEndY = mMargin;
                mEndPosition = mEndY;
                mCurrentDrawPosition = mMaxWidth - 2 * mMargin;
                break;
            }
        }
        mCurrentRectangleSide++;
        Timber.i("Setup next line " + mCurrentRectangleSide
                + " startX = " + mStartX + " startY = " + mStartY
                + " endX = " + mEndX + " endY = " + mEndY);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        Timber.i("width: " + displayMetrics.widthPixels + " \n height: " + displayMetrics.heightPixels + " \nMargin " + mMargin);
        mMaxWidth = displayMetrics.widthPixels;
        int width = (int) (mMaxWidth - mMargin * 2);
        mStartX = mMargin;
        mStartY = mMargin;
        mEndY = mMargin;
        mEndX = mMaxWidth - mMargin;
        mEndPosition = mEndX;

        setMeasuredDimension(width, width);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }

    public void showAnimation() {
        mAnimate = true;
        mAnimationStartTime = 0;
        invalidate();
    }

    public float getCurrentDrawPosition() {
        long now = System.currentTimeMillis();
        float pathGone = ((float) (now - mAnimationStartTime) / (mAnimationDuration));
        float interpolatedPathGone = mAnimationInterpolator.getInterpolation(pathGone);

        if (pathGone < 1.0f) {
            mCurrentDrawPosition = mEndX * interpolatedPathGone;
        } else {
            mCurrentDrawPosition = mEndX;
        }

        return mCurrentDrawPosition;
    }
}
