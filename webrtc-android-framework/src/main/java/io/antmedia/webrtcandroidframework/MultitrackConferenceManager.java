package io.antmedia.webrtcandroidframework;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

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
import java.util.Arrays;
import java.util.LinkedHashMap;

/*
 * This class manages the multitrack conference with 2 WebRTCClient;one for publishing the participants video,
 * the other one for playing the main track which includes all participants streams as subtrack.
 *
 */
public class MultitrackConferenceManager implements AntMediaSignallingEvents, IDataChannelMessageSender {
    private final Context context;
    private final Intent intent;
    private final String serverUrl;
    private final String roomName;
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

    private boolean reconnectionEnabled = false;

    private LinkedHashMap<SurfaceViewRenderer, String> playRendererAllocationMap = new LinkedHashMap<>();

    private Runnable getRoomInfoRunnable = new Runnable() {
        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };
    private boolean playOnlyMode = false;
    private boolean playMessageSent = false;


    public MultitrackConferenceManager(Context context, IWebRTCListener webRTCListener, Intent intent, String serverUrl, String roomName, SurfaceViewRenderer publishViewRenderer, ArrayList<SurfaceViewRenderer> playViewRenderers, String streamId, IDataChannelObserver dataChannelObserver) {
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

    public boolean isPlayOnlyMode() {
        return playOnlyMode;
    }

    public boolean isJoined() {
        return joined;
    }

    public void joinTheConference() {
        wsHandler.joinToConferenceRoom(roomName, streamId);
    }

    private void initWebSocketHandler() {
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
            publishWebRTCClient = new WebRTCClient(webRTCListener, context);
            publishWebRTCClient.setWsHandler(wsHandler);
            publishWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
            if (dataChannelObserver != null) {
                publishWebRTCClient.setDataChannelObserver(dataChannelObserver);
            }

            String tokenId = "";

            publishWebRTCClient.setOpenFrontCamera(openFrontCamera);
            publishWebRTCClient.setVideoRenderers(null, publishViewRenderer);

            publishWebRTCClient.init(serverUrl, streamId, IWebRTCClient.MODE_PUBLISH, tokenId, intent);
            publishWebRTCClient.setMainTrackId(roomName);


            this.streamId = streamId;
            publishWebRTCClient.startStream();
        }
        else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");
        //is first join ie. not rejoin
        if(publishWebRTCClient == null) {
            publishStream(streamId);
        }
        joined = true;
        // start periodic polling of room info
        scheduleGetRoomInfo();
    }

    @Override
    public void onRoomInformation(String[] streams) {
        if(!playMessageSent) {
           playWebRTCClient = new WebRTCClient(webRTCListener, context);
           playWebRTCClient.setWsHandler(wsHandler);
           playWebRTCClient.setReconnectionEnabled(reconnectionEnabled);
           playWebRTCClient.setRemoteRendererList(new ArrayList<>(playRendererAllocationMap.keySet()));
           playWebRTCClient.setAutoPlayTracks(true);
           playWebRTCClient.setMainTrackId(roomName);
           String tokenId = "";

           if (dataChannelObserver != null) {
               playWebRTCClient.setDataChannelObserver(dataChannelObserver);
           }

           playWebRTCClient.init(serverUrl, roomName, IWebRTCClient.MODE_MULTI_TRACK_PLAY, tokenId, intent);

           playWebRTCClient.startStream();
           playMessageSent = true;
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
        boolean onwTrackInTheList = false;
        for (int i = 0; i < tracks.length; i++) {
            if(tracks[i].equals(streamId)) {
                onwTrackInTheList = true;
                break;
            }
        }

        String[] tracksBePlayed;
        if(onwTrackInTheList) {
            //find and add !
            tracksBePlayed = Arrays.copyOf(tracks, tracks.length);
            for (int i = 0; i < tracksBePlayed.length; i++) {
                if(tracksBePlayed[i].equals(streamId)) {
                    tracksBePlayed[i] =  "!"+streamId;
                    break;
                }
            }
        }
        else {
            //add the list as !+streamId
            tracksBePlayed = Arrays.copyOf(tracks, tracks.length+1);
            tracksBePlayed[tracksBePlayed.length] = "!"+streamId;
        }

        playWebRTCClient.onTrackList(tracksBePlayed);
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {

    }

    @Override
    public void onError(String streamId, String definition) {

    }

    @Override
    public void sendMessageViaDataChannel(DataChannel.Buffer buffer) {
        if (publishWebRTCClient != null) {
            publishWebRTCClient.sendMessageViaDataChannel(buffer);
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
        if (publishWebRTCClient != null) {
            if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.disableVideo();
            }

            sendNotificationEvent("CAM_TURNED_OFF");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableVideo() {
        if (publishWebRTCClient != null) {
            if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.enableVideo();
            }
            sendNotificationEvent("CAM_TURNED_ON");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void disableAudio() {
        if (publishWebRTCClient != null) {
            publishWebRTCClient.disableAudio();

            sendNotificationEvent("MIC_MUTED");
        } else {
            Log.w(this.getClass().getSimpleName(), "It did not joined to the conference room yet ");
        }
    }

    public void enableAudio() {
        if (publishWebRTCClient != null) {
            if (publishWebRTCClient.isStreaming()) {
                publishWebRTCClient.enableAudio();
            }
            sendNotificationEvent("MIC_UNMUTED");
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
            json.put("eventType", "UPDATE_AUDIO_LEVEL");
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
    }
}
