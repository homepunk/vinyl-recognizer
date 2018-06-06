package com.homepunk.github.vinylrecognizer.custom.striptransformer;

import timber.log.Timber;

/**
 * Created by Homepunk on 27.02.2018.
 **/

public class TabNavigationIndexResolver {
    private static final float OFFSET_BOUNDARY_VALUE = 0.5f;
    private static final int MAX_TAB_POSITION = 2;

    private int mNewPosition;
    private int mLastPosition;
    private int mCurrentPosition;
    private int mTransformationTabPosition;

    private float mLastOffset;

    private boolean mIsNavigateToLeft;
    private boolean mIsPreviousNavigateToLeft;
    private boolean mIsScrollToLeft;
    private boolean mIsPreviousScrollToLeft;
    private boolean mIsNewPosition;

    private boolean mIsNavigateToTransformationTab;
    private boolean mIsScrolling;

    public void setCurrentTab(int position) {
//        mIsNavigateToLeft = position <= mCurrentPosition;
//        if (isNavigateOnNewLeftTab(position)) {
            Timber.w("Update tab position = %s", position);
            mLastPosition = mCurrentPosition;
            mCurrentPosition = position;
//        }
    }

    public void onTabScrolled(int position, float offset) {
//        setCurrentTab(position);
        mIsNavigateToTransformationTab = mCurrentPosition != position
                && mTransformationTabPosition == position;
        mIsPreviousScrollToLeft = mIsScrollToLeft;
        mIsScrolling = true;
        if (offset != 0) {
            if (mLastOffset != 0.0f) {
                mIsScrollToLeft = mLastOffset > offset;
//                Timber.i("Determine is scroll to left by offset comparison " + mIsScrollToLeft);
            } else {
                mIsScrollToLeft = offset > OFFSET_BOUNDARY_VALUE;
//                Timber.i("Determine is scroll to left by offset boundary value " + mIsScrollToLeft);
            }
        } else {
            mIsScrollToLeft = position < mCurrentPosition;
//            Timber.i("Determine is scroll to left by current position " + mIsScrollToLeft
//                    + " new position = " + position + " last position = " + mCurrentPosition);
        }
//        if (!mIsScrollToLeft
//                && mCurrentPosition == position
//                && offset != 0) {
//            setCurrentTab(position + 1);
//        }
        if (offset == 0.0f
                && mCurrentPosition != position) {
            setCurrentTab(position);
            mIsScrolling = false;
        }
        if (mCurrentPosition != position) {
            mNewPosition = position;
        } else if (!mIsScrollToLeft
                && mCurrentPosition < MAX_TAB_POSITION) {
            mNewPosition = mCurrentPosition + 1;
        }
        mLastOffset = offset;
        Timber.i("Last offset = " + mLastOffset);
    }

    public boolean isNavigateToTransformationTab() {
        return mIsNavigateToTransformationTab;
    }

    public boolean isScrolling() {
        return mIsScrolling;
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

    public boolean isNavigateToRight() {
        return !mIsScrollToLeft;
    }

    public boolean isPreviousNavigationInTheOppositeDirection() {
        return mIsPreviousScrollToLeft != mIsScrollToLeft;
    }

    public boolean isTransformationTab() {
        return mTransformationTabPosition == mCurrentPosition;
    }

    public boolean isScrollToLeft() {
        return mIsScrollToLeft;
    }

    public boolean isNavigateOnNewLeftTab() {
        return mIsNewPosition;
    }

    private boolean isNavigateOnNewLeftTab(int position) {
        return mIsNewPosition = mCurrentPosition != position;
    }

    /**
     * @return the index of the element to which we are moving
     * */
    public float getNewPosition() {
        return mNewPosition;
    }
}
