package io.antmedia.webrtc_android_sample_app.chat;

import android.graphics.Bitmap;

public class Message {
    private String id; // message id
    private boolean belongsToCurrentUser; // is this message sent by us?

    public Message(String id, boolean belongsToCurrentUser) {
        this.id = id;
        this.belongsToCurrentUser = belongsToCurrentUser;
    }

    public String getId() {
        return id;
    }

    public boolean isBelongsToCurrentUser() {
        return belongsToCurrentUser;
    }
}

