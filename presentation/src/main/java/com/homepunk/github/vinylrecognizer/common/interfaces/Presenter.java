package com.homepunk.github.vinylrecognizer.common.interfaces;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public interface Presenter<V extends View> {
    void bind(V view);

    void terminate();
}
