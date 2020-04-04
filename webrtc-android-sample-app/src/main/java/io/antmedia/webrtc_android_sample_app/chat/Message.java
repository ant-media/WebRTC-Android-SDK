package io.antmedia.webrtc_android_sample_app.chat;

public class Message {
    private String text; // message body
    private boolean belongsToCurrentUser; // is this message sent by us?

    public Message(String text, boolean belongsToCurrentUser) {
        this.text = text;
        this.belongsToCurrentUser = belongsToCurrentUser;
    }

    public String getText() {
        return text;
    }

    public boolean isBelongsToCurrentUser() {
        return belongsToCurrentUser;
    }
}
