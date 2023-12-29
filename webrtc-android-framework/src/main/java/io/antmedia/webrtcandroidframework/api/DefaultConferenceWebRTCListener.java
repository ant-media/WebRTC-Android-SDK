package io.antmedia.webrtcandroidframework.api;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.antmedia.webrtcandroidframework.websocket.Broadcast;

/**
 * Default implementation of {@link IWebRTCListener} for conference applications
 * You may extend this class and override methods that you need
 */
public class DefaultConferenceWebRTCListener extends DefaultWebRTCListener {
    private final String roomId;
    private final String streamId;
    private boolean playOnlyMode = false;
    private boolean publishReconnected;
    private boolean playReconnected;
    private boolean reconnecting;

    public DefaultConferenceWebRTCListener(String roomId, String streamId) {
        super();
        this.roomId = roomId;
        this.streamId = streamId;
    }

    @Override
    public void onPublishStarted(String streamId) {
        super.onPublishStarted(streamId);

        if (reconnecting) {
            webRTCClient.getBroadcastObject(roomId); // FIXME: maybe this is not needed, check it
            publishReconnected = true;
            reconnecting = !(publishReconnected && playReconnected);
        }
    }

    @Override
    public void onPlayStarted(String streamId) {
        super.onPlayStarted(streamId);
        webRTCClient.getBroadcastObject(roomId);

        if (reconnecting) {
            playReconnected = true;
            reconnecting = !(publishReconnected && playReconnected);
        }
    }

    @Override
    public void onSessionRestored(String streamId) {
        super.onSessionRestored(streamId);

        if (reconnecting) {
            publishReconnected = true;
            reconnecting = !(publishReconnected && playReconnected);
        }
    }

    @Override
    public void onPublishFinished(String streamId) {
        super.onPublishFinished(streamId);
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
    }

    public void setPlayOnly(boolean b) {
        playOnlyMode = b;
    }

    @Override
    public void onReconnectionAttempt(String streamId) {
        super.onReconnectionAttempt(streamId);
        reconnecting = true;
    }

    public boolean isReconnectingForTest() {
        return reconnecting;
    }
}