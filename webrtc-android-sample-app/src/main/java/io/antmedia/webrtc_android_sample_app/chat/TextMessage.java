package io.antmedia.webrtc_android_sample_app.chat;

import android.util.Log;

import org.json.JSONObject;

public class TextMessage extends Message {
    private String messageBody; // message body


    public String getMessageBody() {
        return messageBody;
    }

    @Override
    public void parseJson(String jsonText) {
        try {
            JSONObject json = new JSONObject(jsonText);
            super.getValuesFromJson(json);
            messageBody = json.getString("messageBody");

        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage() );
        }
    }

}
