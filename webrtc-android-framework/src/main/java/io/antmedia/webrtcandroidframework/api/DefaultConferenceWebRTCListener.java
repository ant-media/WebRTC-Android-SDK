package io.antmedia.webrtcandroidframework.api;

import org.webrtc.VideoTrack;

import io.antmedia.webrtcandroidframework.websocket.Broadcast;

/**
 * Default implementation of {@link IWebRTCListener} for conference applications
 * You may extend this class and override methods that you need
 */
public class DefaultConferenceWebRTCListener extends DefaultWebRTCListener {
    private final String roomId;
    private final String streamId;

    @Override
    public void onNewVideoTrack(VideoTrack track, String streamId) {
        super.onNewVideoTrack(track, streamId);

    }

    /*
     * This flag is used to check if the play is started or not
     * if play is not started, it will start when publish is started
     * for example, we may join the room as play only, then we will start publishing later
     */
    private boolean playStarted = false;

    /*
     * This flag is used to check if the publish is reconnecting or not
     * if publish is reconnecting, we shouldn't start playing because WebRTCClient handles it
     */
    private boolean publishReconnecting;

    public DefaultConferenceWebRTCListener(String roomId, String streamId) {
        super();
        this.roomId = roomId;
        this.streamId = streamId;
    }

    @Override
    public void onBroadcastObject(Broadcast broadcast) {
        super.onBroadcastObject(broadcast);


    }

    @Override
    public void onPublishStarted(String streamId) {
        super.onPublishStarted(streamId);

        if (publishReconnecting) {
            publishReconnecting = false;
        }

        webRTCClient.play(roomId);

    }

    @Override
    public void onPlayStarted(String streamId) {
       super.onPlayStarted(streamId);
       webRTCClient.getBroadcastObject(roomId);
       playStarted = true;
    }

    @Override
    public void onSessionRestored(String streamId) {
        super.onSessionRestored(streamId);

        if (publishReconnecting) {
            publishReconnecting = false;
        }
    }

    @Override
    public void onPublishFinished(String streamId) {
        super.onPublishFinished(streamId);
    }

    @Override
    public void onPlayFinished(String streamId) {
        super.onPlayFinished(streamId);
        playStarted = false;
    }

    @Override
    public void onReconnectionAttempt(String streamId) {
        super.onReconnectionAttempt(streamId);
        if(streamId.equals(this.streamId)) {
            publishReconnecting = true;
        }
    }

    public boolean isPublishReconnectingForTest() {
        return publishReconnecting;
    }
}