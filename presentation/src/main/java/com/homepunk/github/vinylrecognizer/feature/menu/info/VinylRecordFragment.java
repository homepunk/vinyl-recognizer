package com.homepunk.github.vinylrecognizer.feature.menu.info;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.homepunk.github.vinylrecognizer.R;

import butterknife.ButterKnife;

public class VinylRecordFragment extends Fragment {
    public static VinylRecordFragment newInstance() {
        VinylRecordFragment fragment = new VinylRecordFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_vinyl_detail, container, false);
        ButterKnife.bind(this, root);
        return root;
    }
}
