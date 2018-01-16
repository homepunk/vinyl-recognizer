package com.homepunk.github.vinylrecognizer.base;

import com.homepunk.github.vinylrecognizer.base.interfaces.Presenter;
import com.homepunk.github.vinylrecognizer.base.interfaces.View;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public abstract class BasePresenter<V extends View> implements Presenter<V> {
    protected V view;

    protected abstract void init();

    @Override
    public void bind(V view) {
        this.view = view;
        init();
    }

    @Override
    public void terminate() {
        if (this.view != null) {
            this.view = null;
        }
    }
}
