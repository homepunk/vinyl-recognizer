package com.homepunk.github.vinylrecognizer.custom.surface;

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

public class SquareSurfaceView extends SurfaceView {
    private float mSquareMargin;

    public SquareSurfaceView(Context context) {
        super(context);
        init();
    }

    public SquareSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquareSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        Timber.i("onDraw: w: " + width + " h: " + height);
        float squareLength = width - mSquareMargin;

        RectF rectF = new RectF(mSquareMargin, mSquareMargin, squareLength, squareLength);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        canvas.drawRect(rectF, paint);

    }
}
