package com.homepunk.github.vinylrecognizer.feature.menu.collection;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.homepunk.github.vinylrecognizer.R;

import butterknife.ButterKnife;

/**
 * Created by Homepunk on 20.02.2018.
 **/

public class RecordCollectionFragment extends Fragment {
    public static RecordCollectionFragment newInstance() {
        RecordCollectionFragment fragment = new RecordCollectionFragment();
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
