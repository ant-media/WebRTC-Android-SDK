package io.antmedia.webrtc_android_sample_app.chat;

import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONObject;

public class ImageMessage extends Message {
    private Bitmap imageBitmap;


    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    @Override
    public void parseJson(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            super.getValuesFromJson(json);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage() );
        }
    }


}
