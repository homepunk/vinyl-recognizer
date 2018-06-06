package com.homepunk.github.vinylrecognizer.custom.striptransformer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.homepunk.github.vinylrecognizer.R;
import com.homepunk.github.vinylrecognizer.custom.interpolator.ResizeInterpolator;

import timber.log.Timber;

/**
 * Created by Homepunk on 15.02.2018.
 **/

public class TabNavigationStripTransformer extends View {
    private final static int HIGH_QUALITY_FLAGS = Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG;
    private final static int INVALID_INDEX = -1;

    // Default variables
    private final static float DEFAULT_STRIP_FACTOR = 2.5F;
    private final static float DEFAULT_SQUARE_MARGIN = 0;
    private final static float DEFAULT_STRIP_HEIGHT = 5.0F;
    private final static float DEFAULT_CORNER_RADIUS = 5.0F;
    private final static int DEFAULT_ANIMATION_DURATION = 350;
    private final static int DEFAULT_BACKGROUND_COLOR = Color.GRAY;
    private final static int DEFAULT_STRIP_COLOR = Color.GREEN;

    // Max and min fraction
    private final static float MIN_FRACTION = 0.0F;
    private final static float MAX_FRACTION = 1.0F;
    private final static int MAX_ALPHA_VALUE = 255;

    private final ValueAnimator mAnimator = new ValueAnimator();
    private final ValueAnimator mSquareAnimator = new ValueAnimator();
    private final ValueAnimator mVisibilityAnimator = new ValueAnimator();
    private final ResizeInterpolator mResizeInterpolator = new ResizeInterpolator();
    private final ColorDrawable mBackgroundColorDrawable = new ColorDrawable();
    private final Paint mStripPaint = new Paint(HIGH_QUALITY_FLAGS);
    private final Paint mTabBackgroundPaint = new Paint(HIGH_QUALITY_FLAGS);
    private final RectF mTabBackgroundBounds = new RectF();
    private final RectF mSquareBounds = new RectF();
    private final RectF mStripBounds = new RectF();
    private final RectF mFullContainerBounds = new RectF();
    private final TabNavigationIndexResolver mTabNavigationIndexResolver = new TabNavigationIndexResolver();
    private float mTabWidth;
    private float mStripHeight;
    private float mCurrentFraction;
    private int mAnimationDuration;
    private float mStripStartX;
    private float mStripEndX;
    // Values during animation
    private float mStripTop;
    private float mCurrentStripLeft;
    private float mCurrentStripRight;
    private float mStripBottom;
    private float mSquareMargin;
    private String[] mTitles;

    private float mSquareLeftStripTopX;
    private float mSquareLeftStripTopY;
    private float mSquareLeftStripBottomY;
    private boolean mIsBoundsMeasured;
    private boolean mIsLayoutParamsUpdated;
    private float mLastStripStartX;
    private float mLastStripEndX;
    private boolean mIsBordersUpdated;

    public TabNavigationStripTransformer(Context context) {
        this(context, null);
    }

    public TabNavigationStripTransformer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabNavigationStripTransformer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setWillNotDraw(false);
        // Speed and fix for pre 17 API
        ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, null);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabNavigationStripTransformer);
        try {
            setAnimationDuration(typedArray.getInt(R.styleable.TabNavigationStripTransformer_animationDuration, DEFAULT_ANIMATION_DURATION));
            setStripColor(typedArray.getColor(R.styleable.TabNavigationStripTransformer_stripColor, DEFAULT_STRIP_COLOR));
            setStripFactor(typedArray.getFloat(R.styleable.TabNavigationStripTransformer_stripFactor, DEFAULT_STRIP_FACTOR));
            setStripHeight(typedArray.getDimension(R.styleable.TabNavigationStripTransformer_stripHeight, DEFAULT_STRIP_HEIGHT));
            setStripTransformationTab(typedArray.getInt(R.styleable.TabNavigationStripTransformer_stripTransformationTab, INVALID_INDEX));
            setBackgroundColor(typedArray.getColor(R.styleable.TabNavigationStripTransformer_backgroundColor, DEFAULT_BACKGROUND_COLOR));
            setSquareMargin(typedArray.getDimension(R.styleable.TabNavigationStripTransformer_squareMargin, DEFAULT_SQUARE_MARGIN));
            setTitles(new String[3]);

            mAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(animation -> updateStripPosition((Float) animation.getAnimatedValue()));
            mSquareAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION);
            mSquareAnimator.setInterpolator(new LinearInterpolator());
            mSquareAnimator.addUpdateListener(animation -> updateStripPosition((Float) animation.getAnimatedValue()));
            mVisibilityAnimator.setInterpolator(new LinearInterpolator());
            mVisibilityAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setBackground(new ColorDrawable(Color.TRANSPARENT));
    }

    public void setCurrentTab(int tabIndex) {
        if (mAnimator.isRunning()
                || mTitles.length == 0
                /*|| mCurrentTabIndex == tabIndex*/) {
            return;
        }
        mTabNavigationIndexResolver.setCurrentTab(tabIndex);

        mStripStartX = mCurrentStripLeft;
        mStripEndX = mTabNavigationIndexResolver.getCurrentTabPosition() * mTabWidth;

        mAnimator.start();
    }

    public void onPageScrolled(int newPosition, float positionOffset) {
        Timber.i("onPageScrolled newPosition = " + newPosition);
        mTabNavigationIndexResolver.onTabScrolled(newPosition, positionOffset);

//        if (mTabNavigationIndexResolver.isTransformationTab()
//                && mTabNavigationIndexResolver.isPreviousNavigationInTheOppositeDirection()) {
//            mIsLayoutParamsUpdated = false;
//            Timber.w("User changed scroll direction");
//        }

        /*if (!mIsLayoutParamsUpdated) {
            if (mTabNavigationIndexResolver.isNavigatingOnNewTab()
                    && mTabNavigationIndexResolver.isTransformationTab()) {
//                Timber.i("Navigating to left -> update Layout params");
                mIsLayoutParamsUpdated = true;
            } else if (mTabNavigationIndexResolver.isNavigateToRight()
                    && mTabNavigationIndexResolver.isTransformationTab()) {
//                Timber.i("Navigating to right -> update Layout params");
                mIsLayoutParamsUpdated = true;
//                hz temp reshenie
//                mStripStartX = 360;
//                mStripEndX= 720;
            } else {
                mIsLayoutParamsUpdated = false;
            }
        }
*/
        Timber.i(String.format("Prospective position = %1s, current position = = %2s, raw position = = %3s",
                mTabNavigationIndexResolver.getNewPosition(),
                mTabNavigationIndexResolver.getCurrentTabPosition(),
                newPosition));
        if (mTabNavigationIndexResolver.getNewPosition() != mTabNavigationIndexResolver.getCurrentTabPosition()
                && !mIsBordersUpdated) {
            Timber.e("Update Strip Borders");
            updateStripBorders(mTabNavigationIndexResolver.getCurrentTabPosition());
            mIsBordersUpdated = true;
        } else if (mTabNavigationIndexResolver.getNewPosition() == mTabNavigationIndexResolver.getCurrentTabPosition()){
            Timber.w("Reset borders update");
            mIsBordersUpdated = false;
        }
        updateStripPosition(positionOffset);
    }

    private void updateStripBorders(int position) {
//        boolean isSquareDrawn = !isTransformationTab
//                && mTabNavigationIndexResolver.isTransformationTab();
        mLastStripStartX = mStripStartX != 0 ? mStripStartX : mTabNavigationIndexResolver.getCurrentTabPosition() * mTabWidth;
        mLastStripEndX = mStripEndX != 0 ? mStripEndX : mLastStripStartX + mTabWidth;

        //        if navigating on transformation tab make strip as top side of rectangle
        if (mTabNavigationIndexResolver.isNavigateToTransformationTab()) {
            Timber.i("isSquareDrawing borders");

            float stripY = mFullContainerBounds.height();
            mStripTop = stripY - mStripHeight;
            mStripBottom = stripY;
            mStripStartX = mSquareMargin;
            mStripEndX = mTabWidth * mTitles.length - mSquareMargin;
        } /*else if (mTabNavigationIndexResolver.isTransformationTab()) {
            Timber.i("isSquareDrawn borders");
            mStripStartX = 0;
            mStripEndX = mStripEndX + mSquareMargin;
        } */else {
            Timber.i("Default borders");
            mStripStartX = mTabWidth *
                    (mTabNavigationIndexResolver.isScrolling() ? mTabNavigationIndexResolver.getNewPosition() : mTabNavigationIndexResolver.getCurrentTabPosition());
            mStripEndX = mTabWidth + mStripStartX;
        }
//        mIsBordersUpdated = true;
        Timber.e(String.format("Determined resulting strip position [start x = %s and end x = %s], previous strip [start x = %2s and end x = %3s]",
                mStripStartX, mStripEndX, mLastStripStartX, mLastStripEndX));
    }

    private void updateStripPosition(float fraction) {
        Timber.e("Fraction = %s", fraction);
//        Timber.e("Current strip new [start = %1s, end = %2s]", mStripStartX, mStripEndX);
//        Timber.e("Last strip [start = %1s, end = %2s]", mLastStripStartX, mLastStripEndX);
        mCurrentFraction = /*mTabNavigationIndexResolver.isNavigateToRight() ?
                1 - fraction : */fraction;
//        Timber.e("Current fraction = %s", mCurrentFraction);

        if (mTabNavigationIndexResolver.isTransformationTab() ||
                mTabNavigationIndexResolver.isNavigateToTransformationTab()) {
            final float interpolation = mResizeInterpolator.getInterpolation(mCurrentFraction, true);
//            Timber.i("Interpolation = %s, Last Tab = %2s", interpolation, mTabNavigationIndexResolver.getLastTabPosition());
            float stripWidthDelta = Math.abs(mLastStripStartX - mStripStartX);
//            float stripWidthDelta = mTabWidth * mTabNavigationIndexResolver.getLastTabPosition() * interpolation;;
            final float step = stripWidthDelta * interpolation;
//            Timber.i(String.format("Step = %s", step));
                        Timber.i("Step = %s, strip [start = %1s, end = %2s], last strip [start = %3s, end = %4s]", step, mStripStartX, mStripEndX, mLastStripStartX, mLastStripEndX);
            if (mTabNavigationIndexResolver.isNavigateToRight()) {
                mCurrentStripLeft = mLastStripStartX + step;
                mCurrentStripRight = mLastStripEndX - step;
            } else {
                mCurrentStripLeft = mStripStartX + step;
                mCurrentStripRight = mStripEndX - step;
            }
            final int alpha = (int) ((MAX_ALPHA_VALUE * interpolation));
            setBackgroundAlpha(alpha);
            if (interpolation == 0) {
//                updateStripBorders(mTabNavigationIndexResolver.getCurrentTabPosition());
                return;
            }
        } else if (mTabNavigationIndexResolver.isScrolling()){
            mCurrentStripLeft = mStripStartX +
                    (mResizeInterpolator.getInterpolation(fraction, mTabNavigationIndexResolver.isNavigateToLeft()) *
                            (mStripEndX - mStripStartX));
            mCurrentStripRight = mStripStartX + mTabWidth +
                    (mResizeInterpolator.getInterpolation(fraction, !mTabNavigationIndexResolver.isNavigateToLeft()) *
                            (mStripEndX - mStripStartX));
            Timber.i(String.format("Default behaviour mCurrentStripLeft = %1s, mCurrentStripRight = %2s", mCurrentStripLeft, mCurrentStripRight));
        }
//        Timber.i("Updated mCurrentStripLeft = " + mCurrentStripLeft + " mCurrentStripRight = " + mCurrentStripRight);

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mStripBounds.set(
                mCurrentStripLeft,
                mStripTop,
                mCurrentStripRight,
                mStripBottom);
        canvas.drawRect(mFullContainerBounds, mTabBackgroundPaint);
        canvas.drawRect(mStripBounds, mStripPaint);

    }


    private void updateLayoutParams(boolean isTransformationTab) {
        final ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = (int) mFullContainerBounds.width();
        if (isTransformationTab) {
            Timber.i("Square height = " + mSquareBounds.height());
            layoutParams.height = (int) mSquareBounds.height();
        } else {
            Timber.i("Container height = " + mFullContainerBounds.height());
            layoutParams.height = (int) mFullContainerBounds.height();
        }
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!mIsBoundsMeasured) {
            mSquareBounds.set(mSquareMargin, 0.0f, right - mSquareMargin, right + bottom);
            mFullContainerBounds.set(0.0F, 0.0F, right, bottom);
            mTabBackgroundBounds.set(0.0F, 0.0F, right, bottom - mStripHeight);
            mStripTop = mFullContainerBounds.height() - mStripHeight;
            mStripBottom = mFullContainerBounds.height();
            mIsBoundsMeasured = true;
        }
        Timber.w("onlayout width + " + right + " height " + bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        mTabWidth = width / (float) mTitles.length;
        Timber.w("onMeasure: width" + width);
    }

    public void setStripTransformationTab(int tabIndex) {
        mTabNavigationIndexResolver.setTransformationTabPosition(tabIndex);
    }

    public String[] getTitles() {
        return mTitles;
    }

    public void setTitles(String[] titles) {
        this.mTitles = titles;
    }

    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration;
        mAnimator.setDuration(mAnimationDuration);
        mVisibilityAnimator.setDuration(mAnimationDuration);
    }

    public void setStripHeight(float stripHeight) {
        mStripHeight = stripHeight;
    }

    public void setStripFactor(final float factor) {
        mResizeInterpolator.setFactor(factor);
    }

    public int getStripColor() {
        return mStripPaint.getColor();
    }

    public void setStripColor(int stripColor) {
        mStripPaint.setColor(stripColor);
    }

    public void setBackgroundAlpha(int alpha) {
//        Timber.i("Alpha = %s", alpha);
        mTabBackgroundPaint.setAlpha(alpha);
    }

    @Override
    public void setBackgroundColor(int color) {
        mTabBackgroundPaint.setColor(color);
    }

    public void setSquareMargin(float squareMargin) {
        mSquareMargin = squareMargin;
    }
}
