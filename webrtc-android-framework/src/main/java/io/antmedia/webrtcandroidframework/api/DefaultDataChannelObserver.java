package io.antmedia.webrtcandroidframework.api;

import android.util.Log;

import androidx.annotation.NonNull;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Default implementation of {@link IDataChannelObserver}
 * You may extend this class and override methods that you need
 */
public class DefaultDataChannelObserver implements IDataChannelObserver {
    @Override
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {
        String logText = "Data channel buffered amount changed: " + dataChannelLabel + ": " + previousAmount;
        Log.d(DefaultDataChannelObserver.class.getName(), logText);
    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {
        String logText = "Data channel state changed: " + dataChannelLabel + ": " + state;
        Log.d(DefaultDataChannelObserver.class.getName(), logText);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        String messageText = toTextMessage(buffer);
        textMessageReceived(messageText);
    }

    @NonNull
    protected String toTextMessage(DataChannel.Buffer buffer) {
        ByteBuffer data = buffer.data;
        String messageText = new String(data.array(), StandardCharsets.UTF_8);
        return messageText;
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (successful) {
            String messageText = toTextMessage(buffer);
            Log.i(DefaultDataChannelObserver.class.getSimpleName(), "Message is sent");
        } else {
            Log.e(DefaultDataChannelObserver.class.getSimpleName(),"Could not send the text message");
        }
    }

    public void textMessageReceived(String messageText) {
        Log.i(DefaultDataChannelObserver.class.getSimpleName(), "Text message received: " + messageText);
    }
}

