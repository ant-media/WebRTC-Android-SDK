package io.antmedia.webrtc_android_sample_app.chat;

import android.graphics.Bitmap;

public class ImageMessage extends Message {
    private Bitmap imageBitmap;

    public ImageMessage(String text, boolean belongsToCurrentUser, Bitmap imageBitmap) {
        super(text, belongsToCurrentUser);
        this.imageBitmap = imageBitmap;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }
}
