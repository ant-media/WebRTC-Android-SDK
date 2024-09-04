package io.antmedia.webrtcandroidframework.core;

import android.media.projection.MediaProjection;
import android.util.Log;

public abstract class CustomMediaProjectionCallback extends MediaProjection.Callback {

    private static final String TAG = "CustomCallback";

    // Constructor
    public CustomMediaProjectionCallback() {
        super();
    }

    // Custom method to handle MediaProjection
    public abstract void onMediaProjection(MediaProjection mediaProjection);

    // Other methods from the superclass are inherited as is

    @Override
    public void onStop() {
        super.onStop(); // or provide a custom implementation if needed
    }

    @Override
    public void onCapturedContentResize(int width, int height) {
        super.onCapturedContentResize(width, height); // or provide a custom implementation if needed
    }

    @Override
    public void onCapturedContentVisibilityChanged(boolean isVisible) {
        super.onCapturedContentVisibilityChanged(isVisible); // or provide a custom implementation if needed
    }
}