package io.antmedia.webrtcandroidframework.api;

import android.util.Log;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.core.StreamInfo;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;

/**
 * Default implementation of {@link IWebRTCListener}
 * You may extend this class and override methods that you need
 */
public class DefaultWebRTCListener implements IWebRTCListener {
    protected IWebRTCClient webRTCClient;

    public void setWebRTCClient(IWebRTCClient webRTCClient) {
        this.webRTCClient = webRTCClient;
    }

    @Override
    public void onDisconnected() {
        String messageText = "Disconnected";
        callbackCalled(messageText);
    }

    @Override
    public void onPublishFinished(String streamId) {
        String messageText = "Publish finished for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onPlayFinished(String streamId) {
        String messageText = "Play finished for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onPublishStarted(String streamId) {
        String messageText = "Publish started for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onPlayStarted(String streamId) {
        String messageText = "Play started for " + streamId;
        callbackCalled(messageText);
        
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        String messageText = "No stream exists to play for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onError(String description, String streamId) {
        String messageText = "Error for " + streamId + " : " + description;
        callbackCalled(messageText);
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        String messageText = "Signal channel closed for " + streamId + " : " + code;
        callbackCalled(messageText);
    }

    @Override
    public void streamIdInUse(String streamId) {
        String messageText = "Stream id is already in use " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onIceConnected(String streamId) {
        String messageText = "Ice connected for " + streamId;
        callbackCalled(messageText);
    }
    @Override
    public void onPeerConnectionCreated(String streamId) {
        String messageText = "Peer Connection created for StreamId " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onIceDisconnected(String streamId) {
        String messageText = "Ice disconnected for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onTrackList(String[] tracks) {
        String messageText = "Track list received";
        callbackCalled(messageText);
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {
        String messageText = "Bitrate measurement received";
        callbackCalled(messageText);
    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {
        String messageText = "Stream info list received";
        callbackCalled(messageText);
    }

    @Override
    public void onNewVideoTrack(VideoTrack track) {
        String messageText = "New video track received";
        callbackCalled(messageText);

        for (SurfaceViewRenderer r : webRTCClient.getConfig().remoteVideoRenderers) {
            if (r.getTag() == null) {
                r.setTag(track);
                webRTCClient.setRendererForVideoTrack(r, track);
                break;
            }
        }
    }

    @Override
    public void onVideoTrackEnded(VideoTrack track) {
        String messageText = "Video track ended";
        callbackCalled(messageText);
        for (SurfaceViewRenderer r : webRTCClient.getConfig().remoteVideoRenderers) {
            VideoTrack videoTrack = (VideoTrack) r.getTag();
            if (videoTrack !=null && videoTrack.id().equals(track.id())) {
                webRTCClient.releaseRenderer(r);
                return;
            }
        }
    }

    @Override
    public void onReconnectionAttempt(String streamId) {
        String messageText = "Reconnection attempt for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        String messageText = "Joined the room for " + streamId;
        callbackCalled(messageText);
    }


    @Override
    public void onRoomInformation(String[] streams) {
        String messageText = "Room information received";
        callbackCalled(messageText);
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        String messageText = "Left the room for " + roomId;
        callbackCalled(messageText);
    }

    @Override
    public void onMutedFor(String streamId) {
        String messageText = "Microphone is muted for " + streamId;
        callbackCalled(messageText);
        
    }
    @Override
    public void onUnmutedFor(String streamId) {
        String messageText = "Microphone is unmuted for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onCameraTurnOnFor(String streamId) {
        String messageText = "Camera is turned on for " + streamId;
        callbackCalled(messageText);
        
    }

    @Override
    public void onCameraTurnOffFor(String streamId) {
        String messageText = "Camera is turned off for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onSatatusUpdateFor(String streamId, boolean micStatus, boolean cameraStatus) {
        String messageText = "Status update for " + streamId + " mic: " + micStatus + " camera: " + cameraStatus;
        callbackCalled(messageText);
    }

    @Override
    public void onSessionRestored(String streamId) {
        String messageText = "Session restored for " + streamId;
        callbackCalled(messageText);
    }

    @Override
    public void onBroadcastObject(Broadcast broadcast) {
        String messageText = "Broadcast object received";
        callbackCalled(messageText);
    }


    protected void callbackCalled(String messageText) {
        Log.d(DefaultWebRTCListener.class.getName(), messageText);
    }


}

