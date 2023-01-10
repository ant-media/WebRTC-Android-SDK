package io.antmedia.webrtc_android_sample_app;

import android.content.Intent;

public class ActivityLink {
    private final String label;
    private final Intent intent;

    public ActivityLink(Intent intent, String label) {
        this.intent = intent;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public Intent getIntent() {
        return intent;
    }

}
