package io.antmedia.webrtcandroidframework;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import io.antmedia.webrtcandroidframework.apprtc.IDataChannelMessageSender;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

public class ConferenceManager implements AntMediaSignallingEvents, IDataChannelMessageSender {
    private final Context context;
    private final Intent intent;
    private final String serverUrl;
    private final String roomName;
    private String streamId;
    private HashMap<String, WebRTCClient> peers = new HashMap<>();
    private LinkedHashMap<SurfaceViewRenderer, WebRTCClient> playRendererAllocationMap = new LinkedHashMap<>();
    private SurfaceViewRenderer publishViewRenderer;
    private final IWebRTCListener webRTCListener;
    private final IDataChannelObserver dataChannelObserver;
    private WebSocketHandler wsHandler;
    private Handler handler = new Handler();
    private boolean joined = false;

    private boolean openFrontCamera = false;

    private int ROOM_INFO_POLLING_MILLIS = 5000;
    private Runnable getRoomInfoRunnable = new Runnable() {

        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };
    private boolean playOnlyMode = false;


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

    public void setPlayOnlyMode(boolean playOnlyMode) {
        this.playOnlyMode = playOnlyMode;
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

        // remove periodic room information polling
        clearGetRoomInfoSchedule();

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

    public String getStreamId() {
        return streamId;
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

        streamLeft(streamId);
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        peers.get(streamId).noStreamExistsToPlay(streamId);
    }

    @Override
    public void streamIdInUse(String streamId){
        peers.get(streamId).streamIdInUse(streamId);
    }

    @Override
    public void onStartStreaming(String streamId) {
        peers.get(streamId).onStartStreaming(streamId);
    }


    public void setOpenFrontCamera(boolean openFrontCamera) {
        this.openFrontCamera = openFrontCamera;
    }


    public void publishStream(String streamId) {
        if (!this.playOnlyMode) {
            WebRTCClient publisher = createPeer(streamId, IWebRTCClient.MODE_PUBLISH);
            this.streamId = streamId;
            peers.put(streamId, publisher);
            publisher.startStream();
        }
        else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");
        publishStream(streamId);

        if (streams != null)
        {
            for (String id : streams) {
                WebRTCClient player = createPeer(id, IWebRTCClient.MODE_PLAY);
                peers.put(id, player);
                player.startStream();
            }
        }

        joined = true;
        // start periodic polling of room info
        scheduleGetRoomInfo();
    }

    @Override
    public void onRoomInformation(String[] streams) {
        Set<String> streamSet = new HashSet<>();
        Collections.addAll(streamSet, streams);
        Set<String> oldStreams = new HashSet<>(peers.keySet());
        // remove publisher stream id
        oldStreams.remove(streamId);

        // find newly removed streams
        ArrayList<String> streamsLeft = new ArrayList<>();
        for (String oldStream : oldStreams) {
            // old stream has left now
            if (!streamSet.contains(oldStream)) {
                streamsLeft.add(oldStream);
            }
        }

        // find newly added streams
        ArrayList<String> streamsJoined = new ArrayList<>();
        for (String stream : streams) {
            // a new stream joined now
            if (!oldStreams.contains(stream)) {
                streamsJoined.add(stream);
            }
        }

        // remove them
        for (String leftStream : streamsLeft) {
            streamLeft(leftStream);
            Log.i("ConferenceManager", "left stream: " + leftStream);
        }
        // add them
        for (String joinedStream : streamsJoined) {
            streamJoined(joinedStream);
            Log.i("ConferenceManager", "joined stream: " + joinedStream);
        }

        WebRTCClient publisherClient = peers.get(streamId);
        if (publisherClient != null && !publisherClient.isStreaming()) {
            publishStream(streamId);
        }
    }


    private void streamJoined(String streamId) {
        WebRTCClient player = createPeer(streamId, IWebRTCClient.MODE_PLAY);
        peers.put(streamId, player);
        player.startStream();
    }

    private void streamLeft(String streamId) {
        WebRTCClient peer = peers.remove(streamId);
        if (peer != null) {
            deallocateRenderer(peer);
            peer.stopStream();
            Log.i(ConferenceManager.class.getSimpleName(), "Stream left: " + streamId);
        }
        else {
            Log.w(ConferenceManager.class.getSimpleName(), "Stream left (" + streamId +") but there is no associated peer ");
        }
    }

    @Override
    public void onDisconnected() {
        clearGetRoomInfoSchedule();

    }

    @Override
    public void onTrackList(String[] tracks) {

    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {

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

    private void getRoomInfo() {
        // call getRoomInfo in web socket handler
        if (wsHandler.isConnected()) {
            wsHandler.getRoomInfo(roomName, streamId);
        }
    }

    public HashMap<String, WebRTCClient> getPeers() {
        return peers;
    }
}
