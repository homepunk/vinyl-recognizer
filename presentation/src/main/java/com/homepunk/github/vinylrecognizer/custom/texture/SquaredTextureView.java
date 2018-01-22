package com.homepunk.github.vinylrecognizer.custom.texture;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import com.homepunk.github.vinylrecognizer.util.UnitMeasureUtil;

/**
 * Created by Homepunk on 22.01.2018.
 **/

public class SquaredTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private SurfaceTextureListener mSurfaceTextureListener;
    private Surface mSurface;
    private float mSquareMargin;

    public SquaredTextureView(Context context) {
        super(context);
        init();
    }


    public SquaredTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SquaredTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        mSquareMargin = UnitMeasureUtil.convertDpToPixel(getContext(), 20);
    }

    @Override
    public void setSurfaceTextureListener(SurfaceTextureListener listener) {
        super.setSurfaceTextureListener(this);
        mSurfaceTextureListener = listener;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureAvailable(surfaceTexture, width, height);
        }

//        mSurface = new Surface(surfaceTexture);
//        float squareLength = width - mSquareMargin;
//
//        RectF rectF = new RectF(mSquareMargin, mSquareMargin, squareLength, squareLength);
//        Paint paint = new Paint();
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(Color.GREEN);
//        try {
//            Canvas canvas = mSurface.lockCanvas(null);
//            canvas.drawRect(rectF, paint);
//            mSurface.unlockCanvasAndPost(canvas);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureSizeChanged(surfaceTexture, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }

}
