package io.antmedia.webrtcandroidframework;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.antmedia.webrtcandroidframework.apprtc.IDataChannelMessageSender;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

public class ConferenceManager implements AntMediaSignallingEvents, IDataChannelMessageSender {
    private final Context context;
    private final Intent intent;
    private final String serverUrl;
    private final String roomName;
    private String streamId;
    private HashMap<String, WebRTCClient> peers = new HashMap<>();
    private HashMap<SurfaceViewRenderer, WebRTCClient> playRendererAllocationMap = new HashMap<>();
    private SurfaceViewRenderer publishViewRenderer;
    private final IWebRTCListener webRTCListener;
    private final IDataChannelObserver dataChannelObserver;
    private WebSocketHandler wsHandler;
    private Handler handler = new Handler();
    private boolean joined = false;

    private int index = 0;
    private boolean openFrontCamera = false;

    public ConferenceManager(Context context, IWebRTCListener webRTCListener, Intent intent, String serverUrl, String roomName, SurfaceViewRenderer publishViewRenderer, ArrayList<SurfaceViewRenderer> playViewRenderers, String streamId, IDataChannelObserver dataChannelObserver) {
        this.context = context;
        this.intent = intent;
        this.publishViewRenderer = publishViewRenderer;
        if (playViewRenderers != null) {
            for (SurfaceViewRenderer svr : playViewRenderers) {
                this.playRendererAllocationMap.put(svr, null);
            }
        }
        this.serverUrl = serverUrl;
        this.roomName = roomName;
        this.webRTCListener = webRTCListener;
        this.streamId = streamId;
        this.dataChannelObserver = dataChannelObserver;
        if (dataChannelObserver != null) {
            this.intent.putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);
        }
        initWebSocketHandler();
    }

    public boolean isJoined() {
        return joined;
    }

    public void joinTheConference() {
        initWebSocketHandler();
        wsHandler.joinToConferenceRoom(roomName, streamId);
    }

    private void initWebSocketHandler() {
        if (wsHandler == null) {
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(serverUrl);
        }
    }

    public void leaveFromConference() {
        for (WebRTCClient peer : peers.values()) {
            peer.stopStream();
            deallocateRenderer(peer);
        }

        wsHandler.leaveFromTheConferenceRoom(roomName);

        joined = false;
    }

    private WebRTCClient createPeer(String streamId, String mode) {
        WebRTCClient webRTCClient = new WebRTCClient(webRTCListener, context);

        webRTCClient.setWsHandler(wsHandler);

        String tokenId = "";

        if(mode == IWebRTCClient.MODE_PUBLISH) {
            webRTCClient.setOpenFrontCamera(openFrontCamera);
            webRTCClient.setVideoRenderers(null, publishViewRenderer);
        }
        else {
            webRTCClient.setVideoRenderers(null, allocateRenderer(webRTCClient));
        }

        if (dataChannelObserver != null) {
            webRTCClient.setDataChannelObserver(dataChannelObserver);
        }

        webRTCClient.init(serverUrl, streamId, mode, tokenId, intent);

        return webRTCClient;
    }

    private SurfaceViewRenderer allocateRenderer(WebRTCClient peer) {
        for (Map.Entry<SurfaceViewRenderer, WebRTCClient> entry : playRendererAllocationMap.entrySet()) {
            if(entry.getValue() == null) {
                entry.setValue(peer);
                return entry.getKey();
            }
        }
        return null;
    }

    private void deallocateRenderer(WebRTCClient peer) {
        for (Map.Entry<SurfaceViewRenderer, WebRTCClient> entry : playRendererAllocationMap.entrySet()) {
            if(entry.getValue() == peer) {
                entry.setValue(null);
            }
        }
    }


    //AntMediaSignallingEvents
    @Override
    public void onPublishStarted(String streamId) {
        peers.get(streamId).onPublishStarted(streamId);
    }

    @Override
    public void onRemoteIceCandidate(String streamId, IceCandidate candidate) {
        peers.get(streamId).onRemoteIceCandidate(streamId, candidate);
    }

    @Override
    public void onTakeConfiguration(String streamId, SessionDescription sdp) {
        peers.get(streamId).onTakeConfiguration(streamId, sdp);
    }

    @Override
    public void onPublishFinished(String streamId) {
        peers.get(streamId).onPublishFinished(streamId);
    }

    @Override
    public void onPlayStarted(String streamId) {
        peers.get(streamId).onPlayStarted(streamId);
    }

    @Override
    public void onPlayFinished(String streamId) {
        //it has been deleted because of stream leaved message
        if(peers.containsKey(streamId)) {
            peers.get(streamId).onPlayFinished(streamId);
        }
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        peers.get(streamId).noStreamExistsToPlay(streamId);
    }

    @Override
    public void onStartStreaming(String streamId) {
        peers.get(streamId).onStartStreaming(streamId);
    }


    public void setOpenFrontCamera(boolean openFrontCamera) {
        this.openFrontCamera = openFrontCamera;
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");
        WebRTCClient publisher = createPeer(streamId, IWebRTCClient.MODE_PUBLISH);
        this.streamId = streamId;
        peers.put(streamId, publisher);
        publisher.startStream();

        if(streams != null) {
            for (String id : streams) {
                WebRTCClient player = createPeer(id, IWebRTCClient.MODE_PLAY);
                peers.put(id, player);
                player.startStream();
            }
        }

        joined = true;
    }

    @Override
    public void onStreamJoined(String streamId) {
        WebRTCClient player = createPeer(streamId, IWebRTCClient.MODE_PLAY);
        peers.put(streamId, player);
        player.startStream();
    }

    @Override
    public void onStreamLeaved(String streamId) {
        WebRTCClient peer = peers.remove(streamId);
        deallocateRenderer(peer);
        peer.stopStream();
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onTrackList(String[] tracks) {

    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void sendMessageViaDataChannel(DataChannel.Buffer buffer) {
        WebRTCClient publishStream = peers.get(streamId);

        if (publishStream != null) {
            publishStream.sendMessageViaDataChannel(buffer);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    private void sendNotificationEvent(String eventType) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("streamId", streamId);
            jsonObject.put("eventType", eventType);

            String notificationEventText = jsonObject.toString();

            final ByteBuffer buffer = ByteBuffer.wrap(notificationEventText.getBytes(StandardCharsets.UTF_8));
            DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
            sendMessageViaDataChannel(buf);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "JSON write error when creating notification event");
        }
    }

    public void disableVideo() {
        WebRTCClient publishStream = peers.get(streamId);

        if (publishStream != null) {
            if (publishStream.isStreaming()) {
                publishStream.disableVideo();
            }

            sendNotificationEvent("CAM_TURNED_OFF");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableVideo() {
        WebRTCClient publishStream = peers.get(streamId);

        if (publishStream != null) {
            if (publishStream.isStreaming()) {
                publishStream.enableVideo();
            }
            sendNotificationEvent("CAM_TURNED_ON");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void disableAudio() {
        WebRTCClient publishStream = peers.get(streamId);

        if (publishStream != null) {
            if (publishStream.isStreaming()) {
                publishStream.disableAudio();
            }

            sendNotificationEvent("MIC_MUTED");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableAudio() {
        WebRTCClient publishStream = peers.get(streamId);

        if (publishStream != null) {
            if (publishStream.isStreaming()) {
                publishStream.enableAudio();
            }
            sendNotificationEvent("MIC_UNMUTED");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public boolean isPublisherAudioOn() {
        WebRTCClient publishStream = peers.get(streamId);
        if (publishStream != null) {
            return publishStream.isAudioOn();
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
            return false;
        }
    }

    public boolean isPublisherVideoOn() {
        WebRTCClient publishStream = peers.get(streamId);
        if (publishStream != null) {
            return publishStream.isVideoOn();
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
            return false;
        }
    }
}
