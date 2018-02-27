package com.homepunk.github.vinylrecognizer.custom.striptransformer;

import timber.log.Timber;

/**
 * Created by Homepunk on 27.02.2018.
 **/

public class TabNavigationIndexResolver {
    private static final float OFFSET_BOUNDARY_VALUE = 0.5f;

    private int mLastPosition;
    private int mCurrentPosition;
    private int mTransformationTabPosition;

    private float mLastOffset;

    private boolean mIsNavigateToLeft;
    private boolean mIsScrollToLeft;
    private boolean mIsNewPosition;

    public void setCurrentTab(int position) {
        mIsNavigateToLeft = position < mCurrentPosition;
        if (isNavigateOnNewTab(position)) {
            Timber.i("Update tab position = %s", position);
            mLastPosition = mCurrentPosition;
            mCurrentPosition = position;
        }
    }

    public void onTabScrolled(int position, float offset) {
        setCurrentTab(position);
        if (offset != 0) {
            if (mLastOffset != 0.0f) {
                mIsScrollToLeft = mLastOffset > offset;
            } else {
                mIsScrollToLeft = offset > OFFSET_BOUNDARY_VALUE;
            }
        } else {
            mIsScrollToLeft = position < mCurrentPosition;
        }
//        if (!mIsScrollToLeft
//                && mCurrentPosition == position
//                && offset != 0) {
//            setCurrentTab(position + 1);
//        }
        mLastOffset = offset;
    }

    public int getLastTabPosition() {
        return mLastPosition;
    }

    public int getCurrentTabPosition() {
        return mCurrentPosition;
    }

    public int getTransformationTabPosition() {
        return mTransformationTabPosition;
    }

    public void setTransformationTabPosition(int position) {
        mTransformationTabPosition = position;
    }

    public boolean isNavigateToLeft() {
        return mIsNavigateToLeft;
    }

    public boolean isTransformationTab() {
        return mTransformationTabPosition == mCurrentPosition;
    }

    public boolean isScrollToLeft() {
        return mIsScrollToLeft;
    }

    public boolean isNavigateOnNewTab() {
        return mIsNewPosition || !mIsNavigateToLeft;
    }

    private boolean isNavigateOnNewTab(int position) {
        return mIsNewPosition = mCurrentPosition != position;
    }

}
