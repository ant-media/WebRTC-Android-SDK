package io.antmedia.webrtcandroidframework;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStreamTrack;
import org.webrtc.NetworkChangeDetector;
import org.webrtc.NetworkMonitorAutoDetect;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

/*
 * This class manages the multitrack conference with 2 WebRTCClient;one for publishing the participants video,
 * the other one for playing the main track which includes all participants streams as subtrack.
 *
 */
public class MultitrackConferenceManager implements AntMediaSignallingEvents, IDataChannelMessageSender {
    public static final String TAG = "Multitrack Conf";
    public static final int MAX_BITRATE = 2000;
    public static final int MIN_BITRATE = 500;
    public static final String MIC_UNMUTED = "MIC_UNMUTED";
    public static final String MIC_MUTED = "MIC_MUTED";
    public static final String CAM_TURNED_ON = "CAM_TURNED_ON";
    public static final String CAM_TURNED_OFF = "CAM_TURNED_OFF";
    public static final String EVENT_TYPE = "eventType";
    public static final String STREAM_ID = "streamId";
    public static final String UPDATE_STATUS = "UPDATE_STATUS";
    public static final String MIC_STATUS = "mic";
    public static final String CAMERA_STATUS = "camera";
    private final Context context;
    private final Intent intent;
    private final String serverUrl;
    private final String roomName;
    private final ArrayList<SurfaceViewRenderer> playViewRenderers;
    private WebRTCClient publishWebRTCClient;
    private WebRTCClient playWebRTCClient;
    private String streamId;
    private SurfaceViewRenderer publishViewRenderer;
    private final IWebRTCListener webRTCListener;
    private IDataChannelObserver dataChannelObserver;
    private WebSocketHandler wsHandler;
    private Handler handler = new Handler();
    private boolean joined = false;
    private boolean audioOnly = false;


    private boolean openFrontCamera = false;

    private int ROOM_INFO_POLLING_MILLIS = 5000;

    private int STATUS_SEND_PERIOD_MILLIS = 3000;

    private boolean reconnectionEnabled = false;

    private LinkedHashMap<SurfaceViewRenderer, String> playRendererAllocationMap = new LinkedHashMap<>();

    private Runnable getRoomInfoRunnable = new Runnable() {
        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };

    private Runnable sendStatusRunnable = new Runnable() {
        @Override
        public void run() {
            sendStatusMessage();
            handler.postDelayed(this, STATUS_SEND_PERIOD_MILLIS);
        }
    };

    private boolean playOnlyMode = false;

    private boolean playMessageSent = false;

    private NetworkMonitorAutoDetect networkDetector;
    private int minABRResolution;

    public MultitrackConferenceManager(Context context, IWebRTCListener webRTCListener, Intent intent, String serverUrl, String roomName, SurfaceViewRenderer publishViewRenderer, ArrayList<SurfaceViewRenderer> playViewRenderers, String streamId, IDataChannelObserver dataChannelObserver) {
        this.context = context;
        this.intent = intent;
        this.publishViewRenderer = publishViewRenderer;
        this.playViewRenderers = playViewRenderers;
        if (playViewRenderers != null) {
            for (SurfaceViewRenderer svr : playViewRenderers) {
                this.playRendererAllocationMap.put(svr, null);
            }
        }
        this.serverUrl = serverUrl;
        this.roomName = roomName;
        this.webRTCListener = webRTCListener;
        this.streamId = streamId == null ? "stream"+new Random().nextInt(99999) : streamId;
        this.dataChannelObserver = dataChannelObserver;
        if (dataChannelObserver != null) {
            this.intent.putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);
        }
    }

    public void init() {
        initWebSocketHandler();
        //createNetworkChangeObserver();
        if (!this.playOnlyMode) {
            initPublishWebRTCClient();
        }

        initPlayWebRTCClient();
        scheduleSendStatusTimer();
    }

    private void scheduleSendStatusTimer() {
        handler.postDelayed(sendStatusRunnable, STATUS_SEND_PERIOD_MILLIS);
    }

    private void clearSendStatusSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(sendStatusRunnable)) {
                handler.removeCallbacks(sendStatusRunnable);
            }
        } else {
            handler.removeCallbacks(sendStatusRunnable);
        }
    }

    public void sendStatusMessage() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(MIC_STATUS, publishWebRTCClient.isAudioOn());
            jsonObject.put(CAMERA_STATUS, publishWebRTCClient.isVideoOn());
            sendNotificationEvent(UPDATE_STATUS, jsonObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPublishWebRTCClient() {
        publishWebRTCClient = new WebRTCClient(webRTCListener, context);
        publishWebRTCClient.setWsHandler(wsHandler);
        publishWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
        publishWebRTCClient.setCheckStreamIdValidity(false);
        if (dataChannelObserver != null) {
            publishWebRTCClient.setDataChannelObserver(dataChannelObserver);
        }

        String tokenId = "";

        //publishWebRTCClient.setOpenFrontCamera(openFrontCamera);
        publishWebRTCClient.setVideoRenderers(null, publishViewRenderer);

        publishWebRTCClient.setMainTrackId(roomName);
        publishWebRTCClient.init(serverUrl, streamId, IWebRTCClient.MODE_PUBLISH, tokenId, intent);
    }

    private void initPlayWebRTCClient() {
        playWebRTCClient = new WebRTCClient(webRTCListener, context);
        playWebRTCClient.setWsHandler(wsHandler);
        playWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
        playWebRTCClient.setRemoteRendererList(playViewRenderers);
        playWebRTCClient.setAutoPlayTracks(true);
        playWebRTCClient.setMainTrackId(roomName);
        playWebRTCClient.setSelfStreamId(streamId);
        String tokenId = "";

        if (dataChannelObserver != null) {
            playWebRTCClient.setDataChannelObserver(dataChannelObserver);
        }

        playWebRTCClient.init(serverUrl, roomName, IWebRTCClient.MODE_MULTI_TRACK_PLAY, tokenId, intent);
    }

    public void setPlayOnlyMode(boolean playOnlyMode) {
        this.playOnlyMode = playOnlyMode;
    }

    public boolean isPlayOnlyMode() {
        return playOnlyMode;
    }

    public boolean isJoined() {
        return joined;
    }

    public void joinTheConference() {
        wsHandler.joinToConferenceRoom(roomName, streamId);
    }

    public void initWebSocketHandler() {
        if (wsHandler == null) {
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(serverUrl);
        }
    }

    public void leaveFromConference() {

        for (SurfaceViewRenderer peer : playRendererAllocationMap.keySet()) {
        }

        wsHandler.leaveFromTheConferenceRoom(roomName);
        joined = false;
        playMessageSent = false;

        // remove periodic room information polling
        clearGetRoomInfoSchedule();
        clearSendStatusSchedule();

    }

    private SurfaceViewRenderer allocateRenderer(WebRTCClient peer) {
        return null;
    }

    private void deallocateRenderer(WebRTCClient peer) {

    }


    //AntMediaSignallingEvents
    @Override
    public void onPublishStarted(String streamId) {

        if(publishWebRTCClient.isReconnectionInProgress()) {
            //this is a trick to add participant id to conference
            //structure again after reconnection
            joinTheConference();
        }

        publishWebRTCClient.onPublishStarted(streamId);
    }

    @Override
    public void onRemoteIceCandidate(String streamId, IceCandidate candidate) {
        if(streamId.equals(this.streamId)) {
            publishWebRTCClient.onRemoteIceCandidate(streamId, candidate);
        }
        else if(streamId.equals(this.roomName)) {
            playWebRTCClient.onRemoteIceCandidate(streamId, candidate);
        }
    }

    @Override
    public void onTakeConfiguration(String streamId, SessionDescription sdp) {
        if(streamId.equals(this.streamId)) {
            publishWebRTCClient.onTakeConfiguration(streamId, sdp);
        }
        else if(streamId.equals(this.roomName)) {
            playWebRTCClient.onTakeConfiguration(streamId, sdp);
        }
    }

    @Override
    public void onPublishFinished(String streamId) {
        publishWebRTCClient.onPublishFinished(streamId);
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public void onPlayStarted(String streamId) {
        playWebRTCClient.onPlayStarted(streamId);
        //playStarted = true;
    }

    @Override
    public void onPlayFinished(String streamId) {
        playWebRTCClient.onPlayFinished(streamId);
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        playWebRTCClient.noStreamExistsToPlay(streamId);
    }

    @Override
    public void streamIdInUse(String streamId){
        publishWebRTCClient.streamIdInUse(streamId);
    }

    @Override
    public void onStartStreaming(String streamId) {
        if(streamId.equals(this.streamId)) {
            publishWebRTCClient.onStartStreaming(streamId);
        }
        else if(streamId.equals(this.roomName)) {
            playWebRTCClient.onStartStreaming(streamId);
        }
    }


    public void setOpenFrontCamera(boolean openFrontCamera) {
        this.openFrontCamera = openFrontCamera;
    }


    public void publishStream(String streamId) {
        if (!this.playOnlyMode) {
            publishWebRTCClient.startStream();
            //setPublishBitrate(networkDetector.getCurrentConnectionType());
        }
        else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");

        if(!publishWebRTCClient.isReconnectionInProgress() && !playOnlyMode) {
            publishStream(streamId);
        }

        if(playOnlyMode) {
            startPlaying();
        }

        joined = true;
        // start periodic polling of room info
        scheduleGetRoomInfo();
        if(streams.length > 0) {
            //on track list triggers start playing
            onTrackList(streams);
        }
    }

    private void startPlaying() {
        if(!playMessageSent) {
            playWebRTCClient.startStream();
            playMessageSent = true;
        }
    }

    @Override
    public void onRoomInformation(String[] streams) {
        if (playWebRTCClient != null && !playWebRTCClient.isStreamStarted()) {
            playWebRTCClient.startStream();
        }
    }

    public void switchCamera()
    {
        if (publishWebRTCClient != null) {
            publishWebRTCClient.switchCamera();
        }
    }


    private void streamJoined(String streamId) {

    }

    private void trackLeft(String streamId) {
        /*
        WebRTCClient peer = peers.remove(streamId);
        if (peer != null) {
            deallocateRenderer(peer);
            peer.stopStream();
            Log.i(MultitrackConferenceManager.class.getSimpleName(), "Stream left: " + streamId);
        }
        else {
            Log.w(MultitrackConferenceManager.class.getSimpleName(), "Stream left (" + streamId +") but there is no associated peer ");
        }

         */
    }

    @Override
    public void onDisconnected() {
        clearGetRoomInfoSchedule();

    }

    @Override
    public void onTrackList(String[] tracks) {
        //add own stream id  to the list as !+streamId
        ArrayList<String> trackList = new ArrayList<>();
        trackList.addAll(Arrays.asList(tracks));
        trackList.remove(streamId);
        trackList.add("!"+streamId);

        playWebRTCClient.onTrackList(trackList.toArray(new String[0]));
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {
        String[] stringArray = new String[streamInfoList.size()];
        minABRResolution = 0; //automatic abr
        int i = 0;
        for (StreamInfo si : streamInfoList) {
            minABRResolution = minABRResolution > si.getHeight() || minABRResolution == 0 ? si.getHeight() : minABRResolution;
        }

    }

    @Override
    public void onError(String streamId, String definition) {
        if(streamId != null && streamId.equals(this.streamId)) {
            publishWebRTCClient.onError(streamId, definition);
        }
        else if(streamId != null && streamId.equals(this.roomName)) {
            playWebRTCClient.onError(streamId, definition);
        }
    }

    @Override
    public void sendMessageViaDataChannel(DataChannel.Buffer buffer) {
        if (publishWebRTCClient != null) {
            publishWebRTCClient.sendMessageViaDataChannel(buffer);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void sendNotificationEvent(String eventType, JSONObject data) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(STREAM_ID, streamId);
            jsonObject.put(EVENT_TYPE, eventType);
            if(data != null) {
                jsonObject.put("info", data);
            }

            String notificationEventText = jsonObject.toString();

            final ByteBuffer buffer = ByteBuffer.wrap(notificationEventText.getBytes(StandardCharsets.UTF_8));
            DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
            sendMessageViaDataChannel(buf);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "JSON write error when creating notification event");
        }
    }

    public void onMessage(final DataChannel.Buffer buffer, String dataChannelLabel) {
        if (!buffer.binary) {
            ByteBuffer data = buffer.data;
            try {
                String strDataJson = new String(data.array(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(strDataJson);
                String eventType = jsonObject.getString(EVENT_TYPE);
                String streamId = jsonObject.getString(STREAM_ID);
                if(eventType.equals(MIC_MUTED)) {
                    webRTCListener.onMutedFor(streamId);
                }
                else if(eventType.equals(MIC_UNMUTED)) {
                    webRTCListener.onUnmutedFor(streamId);
                }
                else if(eventType.equals(CAM_TURNED_ON)) {
                    webRTCListener.onCameraTurnOnFor(streamId);
                }
                else if(eventType.equals(CAM_TURNED_OFF)) {
                    webRTCListener.onCameraTurnOffFor(streamId);
                }
                else if(eventType.equals(UPDATE_STATUS)) {
                    boolean micStatus = jsonObject.getBoolean(MIC_STATUS);
                    boolean cameraStatus = jsonObject.getBoolean(CAMERA_STATUS);
                    webRTCListener.onSatatusUpdateFor(streamId, micStatus, cameraStatus);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public void disableVideo() {
        if (publishWebRTCClient != null) {
            //if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.disableVideo();
           // }

            sendNotificationEvent(CAM_TURNED_OFF, null);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableVideo() {
        if (publishWebRTCClient != null) {
            //if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.enableVideo();
            //}
            sendNotificationEvent(CAM_TURNED_ON, null);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void disableAudio() {
        if (publishWebRTCClient != null) {
            publishWebRTCClient.disableAudio();

            sendNotificationEvent(MIC_MUTED, null);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableAudio() {
        if (publishWebRTCClient != null) {
            //if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.enableAudio();
            //}
            sendNotificationEvent(MIC_UNMUTED, null);
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public boolean isPublisherAudioOn() {
        if (publishWebRTCClient != null) {
            return publishWebRTCClient.isAudioOn();
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
            return false;
        }
    }

    public boolean isPublisherVideoOn() {
        if (publishWebRTCClient != null) {
            return publishWebRTCClient.isVideoOn();
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

    public void updateAudioLevel(int level) {
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(EVENT_TYPE, "UPDATE_AUDIO_LEVEL");
            json.put("audioLevel", level);

            final ByteBuffer buffer = ByteBuffer.wrap(json.toString().getBytes(StandardCharsets.UTF_8));
            DataChannel.Buffer buf= new DataChannel.Buffer(buffer,false);
            publishWebRTCClient.sendMessageViaDataChannel(buf);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Connect to conference room JSON error: " + e.getMessage());
        }
    }

    public boolean isReconnectionEnabled() {
        return reconnectionEnabled;
    }

    public void setReconnectionEnabled(boolean reconnectionEnabled) {
        this.reconnectionEnabled = reconnectionEnabled;
        if(publishWebRTCClient != null) {
            publishWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
        }
        if(playWebRTCClient != null) {
            playWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
        }
    }

    public void setPublishBitrate(NetworkChangeDetector.ConnectionType newConnectionType) {
        if (newConnectionType.equals(NetworkChangeDetector.ConnectionType.CONNECTION_WIFI)) {
            Log.d(TAG, "Network Wifi");
            if(publishWebRTCClient != null) {
                publishWebRTCClient.setBitrate(MAX_BITRATE);
            }
            if(playWebRTCClient != null && playWebRTCClient.isStreaming()) {
                playWebRTCClient.forceStreamQuality(-1); //unlimited
            }
        } else {
            Log.d(TAG, "newConnectionType:" + newConnectionType);
            if(publishWebRTCClient != null) {
                publishWebRTCClient.setBitrate(MIN_BITRATE);
            }
            if(playWebRTCClient != null) {
                playWebRTCClient.forceStreamQuality(minABRResolution);
            }
        }
    }

    private void createNetworkChangeObserver() {
        networkDetector = new NetworkMonitorAutoDetect(new NetworkChangeDetector.Observer() {
            @Override
            public void onConnectionTypeChanged(NetworkChangeDetector.ConnectionType newConnectionType) {
                setPublishBitrate(newConnectionType);
            }

            @Override
            public void onNetworkConnect(NetworkChangeDetector.NetworkInformation networkInfo) {

            }

            @Override
            public void onNetworkDisconnect(long networkHandle) {

            }

            @Override
            public void onNetworkPreference(List<NetworkChangeDetector.ConnectionType> types, int preference) {

            }
        }, context);

    }

    public void addTrackToRenderer(VideoTrack track, SurfaceViewRenderer renderer) {
        playWebRTCClient.addTrackToRenderer(track, renderer);
    }

    public void setWsHandler(WebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    public boolean isPlayMessageSent() {
        return playMessageSent;
    }

    public void setPublishWebRTCClient(WebRTCClient publishWebRTCClient) {
        this.publishWebRTCClient = publishWebRTCClient;
    }
}
