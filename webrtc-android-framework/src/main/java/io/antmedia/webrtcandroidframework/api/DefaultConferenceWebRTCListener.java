package io.antmedia.webrtcandroidframework.api;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

/**
 * Default implementation of {@link IWebRTCListener} for conference applications
 * You may extend this class and override methods that you need
 */
public class DefaultConferenceWebRTCListener extends DefaultWebRTCListener {
    private final String roomId;
    private final String streamId;

    private boolean playOnlyMode = false;
    private boolean playMessageSent;

    private final int ROOM_INFO_POLLING_MILLIS = 5000;

    private final Handler handler = new Handler();
    private final Runnable getRoomInfoRunnable = new Runnable() {
        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };

    public DefaultConferenceWebRTCListener(String roomId, String streamId) {
        super();
        this.roomId = roomId;
        this.streamId = streamId;
    }

    private void getRoomInfo() {
        webRTCClient.getRoomInfo(roomId, streamId);
    }

    @Override
    public void onPublishStarted(String streamId) {
        super.onPublishStarted(streamId);
    }

    @Override
    public void onPublishFinished(String streamId) {
        super.onPublishFinished(streamId);
    }


    @Override
    public void onDisconnected() {
        super.onDisconnected();
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        super.onJoinedTheRoom(streamId, streams);

        if (!webRTCClient.isReconnectionInProgress() && !playOnlyMode) {
            publishStream(streamId);
        }

        if (playOnlyMode) {
            startPlaying(streams);
        }

        // start periodic polling of room info
        scheduleGetRoomInfo();
        if (streams.length > 0) {
            //on track list triggers start playing
            onTrackList(streams);
        }
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        super.onLeftTheRoom(roomId);
        clearGetRoomInfoSchedule();
        playMessageSent = false;
    }

    @Override
    public void onRoomInformation(String[] streams) {
        super.onRoomInformation(streams);
        if (webRTCClient != null) {
            startPlaying(streams);
        }
    }

    private void scheduleGetRoomInfo() {
        handler.postDelayed(getRoomInfoRunnable, ROOM_INFO_POLLING_MILLIS);
    }

    private void clearGetRoomInfoSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(getRoomInfoRunnable)) {
                handler.removeCallbacks(getRoomInfoRunnable);
            }
        } else {
            handler.removeCallbacks(getRoomInfoRunnable);
        }
    }

    public void publishStream(String streamId) {
        if (!playOnlyMode) {
            webRTCClient.publish(streamId, "", webRTCClient.getConfig().videoCallEnabled, webRTCClient.getConfig().audioCallEnabled, "", "",
                    streamId, roomId);
        } else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    private void startPlaying(String[] streams) {
        if (!playMessageSent) {
            webRTCClient.play(roomId, streams);
            playMessageSent = true;
        }
    }

    public void setPlayOnly(boolean b) {
        playOnlyMode = b;
    }
}

