package io.antmedia.webrtc_android_sample_app.chat;

public class TextMessage extends Message {
    private String text; // message body
    public TextMessage(String id, boolean belongsToCurrentUser, String text) {
        super(id, belongsToCurrentUser);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
