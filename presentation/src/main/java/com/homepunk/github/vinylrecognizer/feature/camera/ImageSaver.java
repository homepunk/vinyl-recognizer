package com.homepunk.github.vinylrecognizer.feature.camera;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * Created by Homepunk on 11.01.2018.
 **/

public class ImageSaver implements Runnable {
    private Context mContext;
    private final Image mImage;
    private String mImageFileName;

    public ImageSaver(Context context, Image image, String imageFileName) {
        mContext = context;
        mImage = image;
        mImageFileName = imageFileName;
    }

    @Override
    public void run() {
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(mImageFileName);
            fileOutputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();

            Intent mediaStoreUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaStoreUpdateIntent.setData(Uri.fromFile(new File(mImageFileName)));
            if (mContext != null) {
                mContext.sendBroadcast(mediaStoreUpdateIntent);
            }

            if (fileOutputStream != null) {
                try {
                    Timber.i("Image saved successfully");
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
