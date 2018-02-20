package com.homepunk.github.vinylrecognizer.custom.striptransformer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.homepunk.github.vinylrecognizer.R;
import com.homepunk.github.vinylrecognizer.custom.interpolator.ResizeInterpolator;

/**
 * Created by Homepunk on 15.02.2018.
 **/

public class TabNavigationStripTransformer extends View {
    private final static int HIGH_QUALITY_FLAGS = Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG;
    private final static int INVALID_INDEX = -1;
    private final static String PREVIEW_TITLE = "Title";

    // Default variables
    private final static float DEFAULT_STRIP_FACTOR = 2.5F;
    private final static float DEFAULT_STRIP_HEIGHT = 5.0F;
    private final static float DEFAULT_CORNER_RADIUS = 5.0F;
    private final static int DEFAULT_ANIMATION_DURATION = 350;
    private final static int DEFAULT_STRIP_COLOR = Color.GREEN;

    // Max and min fraction
    private final static float MIN_FRACTION = 0.0F;
    private final static float MAX_FRACTION = 1.0F;

    private final ValueAnimator mAnimator = new ValueAnimator();
    private final ResizeInterpolator mResizeInterpolator = new ResizeInterpolator();
    private final Paint mStripPaint = new Paint(HIGH_QUALITY_FLAGS);

    private final RectF mViewBounds = new RectF();
    private final RectF mStripBounds = new RectF();

    private float mTabWidth;
    private float mStripHeight;
    private int mAnimationDuration;

    private float mStartStripX;
    private float mEndStripX;
    // Values during animation
    private float mStripLeft;
    private float mStripRight;

    private Float mFraction;

    private int mLastTabIndex;
    private int mCurrentTabIndex;

    private boolean mIsResizeIn;
    private String[] mTitles;

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
            setTitles(new String[3]);

            mAnimator.setFloatValues(MIN_FRACTION, MAX_FRACTION);
            mAnimator.setInterpolator(new LinearInterpolator());
            mAnimator.addUpdateListener(animation -> updateStripPosition((Float) animation.getAnimatedValue()));
        } finally {
            typedArray.recycle();
        }
    }

    private void setTitles(String[] titles) {
        this.mTitles = titles;
    }

    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        mAnimationDuration = animationDuration;
        mAnimator.setDuration(mAnimationDuration);
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

    public void setCurrentTab(int tabIndex) {
        if (mAnimator.isRunning() || mTitles.length == 0
                || mCurrentTabIndex == tabIndex) {
            return;
        }

//        int index = Math.max(0, Math.min(tabIndex, mTitles.length - 1));
        mIsResizeIn = tabIndex < mCurrentTabIndex;
        mLastTabIndex = mCurrentTabIndex;
        mCurrentTabIndex = tabIndex;

        mStartStripX = mStripLeft;
        mEndStripX = mCurrentTabIndex * mTabWidth;

        mAnimator.start();
    }

    public void onPageScrolled(int position, float positionOffset) {
        mIsResizeIn = position < mCurrentTabIndex;
        mLastTabIndex = mCurrentTabIndex;
        mCurrentTabIndex = position;

        mStartStripX = position * mTabWidth;
        mEndStripX = mStartStripX + mTabWidth;

        updateStripPosition(positionOffset);
    }

    private void updateStripPosition(Float fraction) {
        // Update general fraction
        mFraction = fraction;

        // Set the strip left side coordinate
        mStripLeft = mStartStripX +
                (mResizeInterpolator.getResizeInterpolation(fraction, mIsResizeIn) *
                        (mEndStripX - mStartStripX));
        // Set the strip right side coordinate
        mStripRight = mStartStripX + mTabWidth +
                (mResizeInterpolator.getResizeInterpolation(fraction, !mIsResizeIn) *
                        (mEndStripX - mStartStripX));

        // Update NTS
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mStripBounds.set(
                mStripLeft,
                mViewBounds.height() - mStripHeight,
                mStripRight,
                mViewBounds.height());

        canvas.drawRect(mStripBounds, mStripPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final float width = MeasureSpec.getSize(widthMeasureSpec);
        final float height = MeasureSpec.getSize(heightMeasureSpec);
        mViewBounds.set(0.0F, 0.0F, width, height);
        mTabWidth = width / (float) mTitles.length;
        mStartStripX = mCurrentTabIndex * mTabWidth;
        mEndStripX = mStartStripX;
        updateStripPosition(MAX_FRACTION);
    }
}
