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
    private final RectF mStripBounds = new RectF();
    private final RectF mFullContainerBounds = new RectF();

    private float mTabWidth;
    private float mStripHeight;
    private float mCurrentFraction;
    private int mAnimationDuration;

    private float mStripStartX;
    private float mStripEndX;
    // Values during animation
    private float mStripTop;
    private float mStripLeft;
    private float mStripRight;
    private float mStripBottom;
    private float mSquareMargin;

    private String[] mTitles;

    private final TabNavigationResolver mTabNavigationResolver = new TabNavigationResolver();

//    private int mLastTabIndex;
//    private int mCurrentTabIndex;
//    private int mStripTransformationTabIndex;

//    private boolean mIsStripMovementInLeftDirection;
    private float mSquareLeftStripTopX;
    private float mSquareLeftStripTopY;
    private float mSquareLeftStripBottomY;

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
        mTabNavigationResolver.setCurrentTab(tabIndex);

        mStripStartX = mStripLeft;
        mStripEndX = mTabNavigationResolver.getCurrentTabPosition() * mTabWidth;

        mAnimator.start();
    }

    public void onPageScrolled(int newPosition, float positionOffset) {
        mTabNavigationResolver.onTabScrolled(newPosition, positionOffset);
        if (mTabNavigationResolver.isNavigateOnNewTab()) {
            updateStripBorders(newPosition, mTabNavigationResolver.isTransformationTab());
        }
        updateStripPosition(positionOffset);
    }


    private void updateStripBorders(int position, boolean isSquareDrawing) {
        boolean isSquareDrawn = mTabNavigationResolver.isTransformationTab()
                && !isSquareDrawing;
        if (isSquareDrawing) {
            float stripY = mFullContainerBounds.height();
            mStripTop = stripY - mStripHeight;
            mStripBottom = stripY;
            mStripStartX = mSquareMargin;
            mStripEndX = mTabWidth * mTitles.length - mSquareMargin;
        } else if (isSquareDrawn) {
            mStripStartX = 0;
            mStripEndX = mStripEndX + mSquareMargin;
        } else {
            mStripStartX = mTabWidth * position;
            mStripEndX = mTabWidth + mStripStartX;
        }
        Timber.i("Determined start x = " + mStripStartX + " and end x = " + mStripEndX);
    }

    private void updateStripPosition(float fraction) {
//        Timber.i("Fraction = %s", fraction);
        mCurrentFraction = fraction;
        if (mTabNavigationResolver.isTransformationTab()) {
            final float interpolation = mResizeInterpolator.getInterpolation(fraction, true);
            final float step = mTabWidth * mTabNavigationResolver.getLastTabPosition() * interpolation;
            mStripLeft = mStripStartX + step;
            mStripRight = mStripEndX - step;
            final int alpha = (int) ((MAX_ALPHA_VALUE * interpolation));
            setBackgroundAlpha(alpha);
            if (interpolation == 0) {
                updateStripBorders(mTabNavigationResolver.getCurrentTabPosition(), false);
                return;
            }
        } else {
            mStripLeft = mStripStartX +
                    (mResizeInterpolator.getInterpolation(fraction, mTabNavigationResolver.isNavigateToLeft()) *
                            (mStripEndX - mStripStartX));
            mStripRight = mStripStartX + mTabWidth +
                    (mResizeInterpolator.getInterpolation(fraction, !mTabNavigationResolver.isNavigateToLeft()) *
                            (mStripEndX - mStripStartX));
        }

        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mStripBounds.set(
                mStripLeft,
                mStripTop,
                mStripRight,
                mStripBottom);

        canvas.drawRect(mTabBackgroundBounds, mTabBackgroundPaint);
        canvas.drawRect(mStripBounds, mStripPaint);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final float width = MeasureSpec.getSize(widthMeasureSpec);
        final float height = MeasureSpec.getSize(heightMeasureSpec);
        mFullContainerBounds.set(0.0F, 0.0F, width, height);
        mTabBackgroundBounds.set(0.0F, 0.0F, width, height - mStripHeight);
        mTabWidth = width / (float) mTitles.length;
        mStripTop = mFullContainerBounds.height() - mStripHeight;
        mStripBottom = mFullContainerBounds.height();
    }

    public void setStripTransformationTab(int tabIndex) {
        mTabNavigationResolver.setTransformationTabPosition(tabIndex);
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
        Timber.i("Alpha = %s", alpha);
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
