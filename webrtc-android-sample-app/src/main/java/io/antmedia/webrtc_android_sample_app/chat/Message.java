package io.antmedia.webrtc_android_sample_app.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Message {

    //private static String DATE_PATTERN = " HH:mm:ss MM-dd-yyyy";
    private static String DATE_PATTERN = " HH:mm";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN);
    private String messageId; // message id
    private boolean belongsToCurrentUser; // is this message sent by us?
    private String messageDate;

    public String getMessageDate() {
        return messageDate;
    }

    public void setBelongsToCurrentUser(boolean belongsToCurrentUser) {
        this.belongsToCurrentUser = belongsToCurrentUser;
    }

    public boolean isBelongsToCurrentUser() {
        return belongsToCurrentUser;
    }

    protected void getValuesFromJson(JSONObject object) {
        try {
            messageId =  object.getString("messageId");
            long time = object.getLong("messageDate");
            Date date = new Date(time);
            messageDate = simpleDateFormat.format(date);

        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "JSON parse error");
        }
    }

    public abstract void parseJson(String jsonText);

    public static JSONObject createMessageJsonObj(String messageId, Date messageDate) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("messageId", messageId);
            jsonObject.put("messageDate", messageDate.getTime());

        } catch (JSONException e) {
            Log.e(Message.class.getSimpleName(), "JSON write error");
        }

        return jsonObject;
    }

    public static String createJsonMessage(String messageId, Date messageDate) {
        JSONObject jsonObject = createMessageJsonObj(messageId, messageDate);

        return jsonObject.toString();
    }

    public static String createJsonTextMessage(String messageId, Date messageDate, String messageBody) {
        JSONObject jsonObject = createMessageJsonObj(messageId, messageDate);
        try {
            jsonObject.put("messageBody", messageBody);
        } catch (JSONException e) {
            Log.e(Message.class.getSimpleName(), "JSON Text Message write error");
        }

        return jsonObject.toString();
    }
}

