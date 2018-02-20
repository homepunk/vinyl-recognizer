package com.homepunk.github.vinylrecognizer.feature.menu;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.homepunk.github.vinylrecognizer.R;
import com.homepunk.github.vinylrecognizer.custom.striptransformer.TabNavigationStripTransformer;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Homepunk on 12.02.2018.
 **/

public class MenuActivity extends AppCompatActivity {
    @BindView(R.id.activity_menu_view_pager)
    ViewPager vPager;
    @BindView(R.id.activity_menu_tab_indicator)
    TabNavigationStripTransformer vStripTransformerPageNavigation;

    private MenuPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        ButterKnife.bind(this);
        mPagerAdapter = new MenuPagerAdapter(getSupportFragmentManager());
        vPager.setAdapter(mPagerAdapter);
        vPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
//                vStripTransformerPageNavigation.setCurrentTab(position);
                if (position == MenuPagerAdapter.FRAGMENT_CAMERA) {
                    startFullscreenMode();
                } else {
                    exitFullscreenMode();
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                vStripTransformerPageNavigation.onPageScrolled(position, positionOffset);
            }
        });
        vPager.setCurrentItem(MenuPagerAdapter.FRAGMENT_VINYL_DETAIL);
    }

    void startFullscreenMode() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;

//        flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
//                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public void exitFullscreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

    }
}
