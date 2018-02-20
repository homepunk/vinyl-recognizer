package com.homepunk.github.vinylrecognizer.common;

import com.homepunk.github.vinylrecognizer.common.interfaces.Presenter;
import com.homepunk.github.vinylrecognizer.common.interfaces.View;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public abstract class BasePresenter<V extends View> implements Presenter<V> {
    protected V view;
    protected List<Disposable> subscriptions = new ArrayList<>();

    protected abstract void onPresenterBound();

    @Override
    public void bind(V view) {
        this.view = view;
        onPresenterBound();
    }

    @Override
    public void terminate() {
        if (this.view != null) {
            this.view = null;
        }
    }
}
