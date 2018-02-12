package com.homepunk.github.vinylrecognizer.feature.menu.history;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.homepunk.github.vinylrecognizer.R;

import butterknife.ButterKnife;

/**
 * Created by Homepunk on 12.02.2018.
 **/

public class HistoryFragment extends Fragment {
    public static HistoryFragment newInstance() {
        Bundle args = new Bundle();
        HistoryFragment fragment = new HistoryFragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, root);
        return root;
    }
}
