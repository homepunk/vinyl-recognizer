package com.homepunk.github.vinylrecognizer.custom.texture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.homepunk.github.vinylrecognizer.util.UnitMeasureUtil;

import timber.log.Timber;

/**
 * Created by Homepunk on 16.01.2018.
 **/

public class SquaredSurfaceView extends SurfaceView {
    private float mSquareMargin;

    public SquaredSurfaceView(Context context) {
        super(context);
        init();
    }

    public SquaredSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquaredSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mSquareMargin = UnitMeasureUtil.convertDpToPixel(getContext(), 20);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        Timber.i("onDraw: w: " + width + " h: " + height);
        float squareLength = width - mSquareMargin;

        RectF rectF = new RectF(mSquareMargin, mSquareMargin, squareLength, squareLength);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        canvas.drawRect(rectF, paint);
    }
}
