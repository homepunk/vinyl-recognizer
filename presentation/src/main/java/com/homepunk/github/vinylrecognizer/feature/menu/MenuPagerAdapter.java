package com.homepunk.github.vinylrecognizer.feature.menu;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.homepunk.github.vinylrecognizer.feature.menu.camera.CameraFragment;
import com.homepunk.github.vinylrecognizer.feature.menu.collection.RecordCollectionFragment;
import com.homepunk.github.vinylrecognizer.feature.menu.history.HistoryFragment;

/**
 * Created by Homepunk on 12.02.2018.
 **/

public class MenuPagerAdapter extends FragmentPagerAdapter {
    public static final int MENU_ITEM_COUNT = 3;

    public static final int FRAGMENT_CAMERA = 0;
    public static final int FRAGMENT_RECORD_COLLECTION = 1;
    public static final int FRAGMENT_RECOGNIZED_VINYL_HISTORY = 2;

    public MenuPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case FRAGMENT_CAMERA: {
                return CameraFragment.newInstance();
            }
            case FRAGMENT_RECORD_COLLECTION: {
                return RecordCollectionFragment.newInstance();
            }
            case FRAGMENT_RECOGNIZED_VINYL_HISTORY: {
                return HistoryFragment.newInstance();
            }
            default:
                throw new RuntimeException("Item at position " + position + " not found");
        }
    }

    @Override
    public int getCount() {
        return MENU_ITEM_COUNT;
    }

}
