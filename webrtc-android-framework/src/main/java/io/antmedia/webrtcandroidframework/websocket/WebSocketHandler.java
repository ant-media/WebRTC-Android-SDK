package io.antmedia.webrtcandroidframework.websocket;

import android.os.Build;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import io.antmedia.webrtcandroidframework.core.StreamInfo;

import static io.antmedia.webrtcandroidframework.websocket.WebSocketConstants.DEFINITION;
import static io.antmedia.webrtcandroidframework.websocket.WebSocketConstants.NOTIFICATION_COMMAND;
import static io.antmedia.webrtcandroidframework.websocket.WebSocketConstants.WEBSOCKET_CONNECTION_TIMEOUT;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
public class WebSocketHandler implements WebSocket.WebSocketConnectionObserver {
    private static final String TAG = "WebSocketHandler";
    private static final int CLOSE_TIMEOUT = 1000;

    private WebSocketConnection ws;
    private final Handler handler;
    private String wsServerUrl;
    private final Object closeEventLock = new Object();
    private boolean closeEvent;
    private AntMediaSignallingEvents signallingListener;
    public ScheduledExecutorService pingPongExecutor;
    private int pingPongTimoutCount = 0;
    public static final  long TIMER_DELAY  = 3000L;
    public static final  long TIMER_PERIOD = 2000L;

    public static final long WEBSOCKET_RECONNECTION_CONTROL_PERIOD_MS = 5000;

    private Runnable wsReconnectorRunnable;

    private Handler wsReconnectionHandler = new Handler();


    Gson gson;


    public WebSocketHandler(AntMediaSignallingEvents signallingListener, Handler handler) {
        this.handler = handler;
        this.signallingListener = signallingListener;

        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();
    }

    public WebSocketConnection creteWebSocket(){
        return new WebSocketConnection();
    }
    public void connect(final String wsUrl) {
        checkIfCalledOnValidThread();
        if(wsUrl==null || wsUrl.isBlank())
            return;
        wsServerUrl = wsUrl;

        ws = creteWebSocket();
        Thread connectorThread = new Thread(() -> {
            try {
                ws.connect(new URI(wsServerUrl), this);
            } catch (WebSocketException e) {
                e.printStackTrace();
                disconnect(false);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                disconnect(false);
            }
        });
        connectorThread.start();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (connectorThread.isAlive()) {
                    connectorThread.interrupt();
                    Log.e(TAG, "exception occurred while waiting for websocket");
                }
            }
        },WEBSOCKET_CONNECTION_TIMEOUT);
    }

    public void sendTextMessage(String message) {
        if (ws.isConnected()) {
            ws.sendTextMessage(message);
            Log.e(TAG, "sent websocket message:" + message);
        } else {
            Log.d(TAG, "Web Socket is not connected");
        }
    }

    public void disconnect(boolean waitForComplete) {
        checkIfCalledOnValidThread();
        Log.d(TAG, "Disconnect WebSocket.");
        ws.disconnect();
        // Wait for websocket close event to prevent websocket library from
        // sending any pending messages to deleted looper thread.
        if (waitForComplete) {
            synchronized (closeEventLock) {
                while (!closeEvent) {
                    try {
                        closeEventLock.wait(CLOSE_TIMEOUT);
                        break;
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Wait error: " + e.toString());
                    }
                }
            }
        }

        Log.d(TAG, "Disconnecting WebSocket done.");
    }

    public void checkIfCalledOnValidThread() {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalStateException("WebSocket method is not called on valid thread");
        }
    }

    @Override
    public void onOpen() {
        Log.d(TAG, "WebSocket connection opened.");
        signallingListener.onWebSocketConnected();
    }

    @Override
    public void onClose(WebSocketCloseNotification webSocketCloseNotification, String s) {
        Log.d(TAG, "WebSocket connection closed.");
        signallingListener.onWebSocketDisconnected();
        synchronized (closeEventLock) {
            closeEvent = true;
            closeEventLock.notify();
            stopPingPongTimer();
        }
    }

    @Override
    public void onTextMessage(String msg) {
        Log.e(TAG, "onTextMessage: "+msg);
        if (!isConnected()) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }
        try {
            JSONObject json = new JSONObject(msg);

            String commandText = json.getString(WebSocketConstants.COMMAND);
            String streamId = null;
            if (json.has(WebSocketConstants.STREAM_ID)) {
                streamId = json.getString(WebSocketConstants.STREAM_ID);
            }
            if (commandText.equals(WebSocketConstants.START_COMMAND)) {
                signallingListener.onStartStreaming(streamId);
            }
            else if (commandText.equals(WebSocketConstants.TAKE_CONFIGURATION_COMMAND)) {
                String description = json.getString(WebSocketConstants.SDP);
                String type = json.getString(WebSocketConstants.TYPE);
                SessionDescription.Type sdpType = SessionDescription.Type.fromCanonicalForm(type);
                SessionDescription sdp = new SessionDescription(sdpType, description);

                signallingListener.onTakeConfiguration(streamId, sdp);
            }
            else if (commandText.equals(WebSocketConstants.TAKE_CANDIDATE_COMMAND)) {
                String id = json.getString(WebSocketConstants.CANDIDATE_ID);
                int label = json.getInt(WebSocketConstants.CANDIDATE_LABEL);
                String sdp = json.getString(WebSocketConstants.CANDIDATE_SDP);

                IceCandidate candidate = new IceCandidate(id, label, sdp);
                signallingListener.onRemoteIceCandidate(streamId, candidate);
            }
            else if (commandText.equals(WebSocketConstants.ROOM_INFORMATION_NOTIFICATION)) {
                String[] streams = null;
                if (json.has(WebSocketConstants.STREAMS_IN_ROOM) && !json.isNull(WebSocketConstants.STREAMS_IN_ROOM)) {
                    JSONArray streamsArray = json.getJSONArray(WebSocketConstants.STREAMS_IN_ROOM);
                    streams = new String[streamsArray.length()];
                    for (int i = 0; i < streamsArray.length(); i++) {
                        streams[i] = streamsArray.getString(i);
                    }
                }
                signallingListener.onRoomInformation(streams);
            }
            else if (commandText.equals(WebSocketConstants.STREAM_INFORMATION_NOTIFICATION)) {
                JSONArray jsonArray = json.getJSONArray(WebSocketConstants.STREAM_INFO);
                ArrayList<StreamInfo> streamInfos = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject streamJSON = (JSONObject) jsonArray.get(i);
                    StreamInfo streamInfo = new StreamInfo();
                    streamInfo.setWidth(streamJSON.getInt(WebSocketConstants.STREAM_WIDTH));
                    streamInfo.setHeight(streamJSON.getInt(WebSocketConstants.STREAM_HEIGHT));
                    streamInfo.setVideoBitrate(streamJSON.getInt(WebSocketConstants.VIDEO_BITRATE));
                    streamInfo.setAudioBitrate(streamJSON.getInt(WebSocketConstants.AUDIO_BITRATE));
                    streamInfo.setCodec(streamJSON.getString(WebSocketConstants.VIDEO_CODEC));

                    streamInfos.add(streamInfo);
                }
                signallingListener.onStreamInfoList(streamId, streamInfos);
            }
            else if (commandText.equals(NOTIFICATION_COMMAND)) {

                String definition = json.getString(DEFINITION);

                Log.d(TAG, "notification:   " + definition);
                if (definition.equals(WebSocketConstants.PUBLISH_STARTED)) {
                    signallingListener.onPublishStarted(streamId);
                    startPingPongTimer();
                }
                else if (definition.equals(WebSocketConstants.PUBLISH_FINISHED)) {
                    signallingListener.onPublishFinished(streamId);
                    stopPingPongTimer();
                }
                else if (definition.equals(WebSocketConstants.PLAY_STARTED)) {
                    signallingListener.onPlayStarted(streamId);

                }
                else if (definition.equals(WebSocketConstants.PLAY_FINISHED)) {
                    signallingListener.onPlayFinished(streamId);
                }
                else if (definition.equals(WebSocketConstants.SESSION_RESTORED_DESCRIPTION)) {
                    signallingListener.onSessionRestored(streamId);
                }
                else if (definition.equals(WebSocketConstants.JOINED_THE_ROOM)) {
                    String[] streams = null;
                    if(json.has(WebSocketConstants.STREAMS_IN_ROOM) && !json.isNull(WebSocketConstants.STREAMS_IN_ROOM)) {
                        JSONArray streamsArray = json.getJSONArray(WebSocketConstants.STREAMS_IN_ROOM);
                        streams = new String[streamsArray.length()];
                        for (int i = 0; i < streamsArray.length(); i++) {
                            streams[i] = streamsArray.getString(i);
                        }
                    }
                    signallingListener.onJoinedTheRoom(streamId, streams);
                }
                else if (definition.equals(WebSocketConstants.LEAVED_THE_ROOM)) {
                    String roomId = null;
                    if (json.has(WebSocketConstants.ATTR_ROOM_NAME)) {
                        roomId = json.getString(WebSocketConstants.ATTR_ROOM_NAME);
                    }
                    signallingListener.onLeftTheRoom(roomId);
                }
                else if (definition.equals(WebSocketConstants.LEAVED_STREAM)) { // p2p
                    String roomId = null;
                    if (json.has(WebSocketConstants.STREAM_ID)) {
                        roomId = json.getString(WebSocketConstants.STREAM_ID);
                    }
                    signallingListener.onLeft(roomId);
                }
                else if (definition.equals(WebSocketConstants.JOINED_THE_STREAM)) { // p2p
                    String roomId = null;
                    if (json.has(WebSocketConstants.STREAM_ID)) {
                        roomId = json.getString(WebSocketConstants.STREAM_ID);
                    }
                    signallingListener.onJoined(roomId);
                }
                else if (definition.equals(WebSocketConstants.BITRATE_MEASUREMENT)) {
                    int targetBitrate = json.getInt(WebSocketConstants.TARGET_BITRATE);
                    int videoBitrate = json.getInt(WebSocketConstants.VIDEO_BITRATE);
                    int audioBitrate = json.getInt(WebSocketConstants.AUDIO_BITRATE);

                    signallingListener.onBitrateMeasurement(streamId, targetBitrate, videoBitrate, audioBitrate);
                }
                else if (definition.equals(WebSocketConstants.BROADCAST_OBJECT_NOTIFICATION)) {
                    String broadcastJson = json.getString(WebSocketConstants.BROADCAST);
                    Broadcast broadcast = gson.fromJson(broadcastJson, Broadcast.class);
                    signallingListener.onBroadcastObject(broadcast);
                }else if(definition.equals(WebSocketConstants.RESOLUTION_CHANGE_INFO_COMMAND)){
                    int resolution = json.getInt(WebSocketConstants.STREAM_HEIGHT);
                    signallingListener.onResolutionChange(streamId, resolution);

                }
            }
            else if (commandText.equals(WebSocketConstants.TRACK_LIST)) {
                JSONArray trackList = json.getJSONArray(WebSocketConstants.TRACK_LIST);
                String[] tracks = new String[trackList.length()];
                for (int i = 0; i < trackList.length(); i++) {
                    tracks[i] = trackList.getString(i);
                }
                signallingListener.onTrackList(tracks);

            }
            else if (commandText.equals(WebSocketConstants.ERROR_COMMAND))
            {

                String definition= json.getString(DEFINITION);
                Log.d(TAG, "error command received: "+ definition);
                //stopPingPongTimer();

                signallingListener.onError(streamId, definition);

                if (definition.equals(WebSocketConstants.NO_STREAM_EXIST))
                {
                    signallingListener.noStreamExistsToPlay(streamId);
                }
                if(definition.equals(WebSocketConstants.STREAM_ID_IN_USE)){
                    signallingListener.streamIdInUse(streamId);
                }
            }
            else if (commandText.equals(WebSocketConstants.STOP_COMMAND)) {
                disconnect(true);
            }
            else if (commandText.equals(WebSocketConstants.PONG_COMMAND))
            {
                pingPongTimoutCount = 0;
                Log.i(TAG, "pong reply is received");
            }
            else {
                Log.e(TAG, "Received offer for call receiver: " + msg);
            }


        } catch (JSONException e) {
            Log.e(TAG, "WebSocket message JSON parsing error: " + e.toString());
        }

    }

    @Override
    public void onRawTextMessage(byte[] bytes) {

    }

    @Override
    public void onBinaryMessage(byte[] bytes) {

    }

    public void startPublish(String streamId, String token, boolean videoEnabled, boolean audioEnabled, String subscriberId, String subscriberCode, String streamName, String mainTrackId){
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCode);
            json.put(WebSocketConstants.STREAM_NAME, streamName);
            json.put(WebSocketConstants.VIDEO, videoEnabled);
            json.put(WebSocketConstants.AUDIO, audioEnabled);
            json.put(WebSocketConstants.MAIN_TRACK, mainTrackId);

            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void startPlay(String streamId, String token, String[] tracks, String subscriberId, String subscriberCode, String viewerInfo){
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);

            JSONArray jsonArray = new JSONArray();
            if (tracks != null) {
                for (String trackId : tracks) {
                    jsonArray.put(trackId);
                }
            }

            json.put(WebSocketConstants.TRACK_LIST, jsonArray);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCode);
            json.put(WebSocketConstants.VIEWER_INFO, viewerInfo);

            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void registerPushNotificationToken(String subscriberId, String authToken, String pushNotificationToken, String tokenType) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.REGISTER_PUSH_NOTIFICATION_TOKEN_COMMAND);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.TOKEN, authToken);
            json.put(WebSocketConstants.PNS_REGISTRATION_TOKEN, pushNotificationToken);
            json.put(WebSocketConstants.PNS_TYPE, tokenType);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendPushNotification(String subscriberId, String authToken, JSONObject pushNotificationContent, JSONArray receiverSubscriberIdArray) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.SEND_PUSH_NOTIFICATION_COMMAND);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.TOKEN, authToken);
            json.put(WebSocketConstants.PUSH_NOTIFICATION_CONTENT, pushNotificationContent);
            json.put(WebSocketConstants.SUBSCRIBER_IDS_TO_NOTIFY, receiverSubscriberIdArray);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void joinToPeer(String streamId, String token) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void stop(String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.STOP_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendConfiguration(String streamId, final SessionDescription sdp, String type) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TYPE, type);
            json.put(WebSocketConstants.SDP, sdp.description);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendLocalIceCandidate(String streamId, final IceCandidate candidate) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.CANDIDATE_LABEL, candidate.sdpMLineIndex);
            json.put(WebSocketConstants.CANDIDATE_ID, candidate.sdpMid);
            json.put(WebSocketConstants.CANDIDATE_SDP, candidate.sdp);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    public void getTrackList(String streamId, String token) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_TRACK_LIST);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void enableTrack(String streamId, String trackId, boolean enabled) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.ENABLE_TRACK);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TRACK_ID, trackId);
            json.put(WebSocketConstants.ENABLED, enabled);
            sendTextMessage(json.toString());

        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public void startPingPongTimer(){
        Log.d(TAG, "Ping Pong timer is started");

        Runnable timerTask = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Ping Pong timer is executed");
                sendPingPongMessage();
                if (pingPongTimoutCount == 2){
                    Log.d(TAG, "Ping Pong websocket response not received for 4 seconds");
                    stopPingPongTimer();
                    disconnect(true);
                }
                pingPongTimoutCount++;

            }
        };

        pingPongExecutor = Executors.newSingleThreadScheduledExecutor();
        pingPongExecutor.scheduleAtFixedRate(timerTask, TIMER_DELAY, TIMER_PERIOD, TimeUnit.MILLISECONDS);

    }

    public void stopPingPongTimer(){

        Log.d(TAG, "Ping Pong timer stop called");

        if (pingPongExecutor != null) {
            pingPongExecutor.shutdown();
            pingPongExecutor = null;
            pingPongTimoutCount = 0;
        }

    }

    public void sendPingPongMessage() {
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PING_COMMAND);
            sendTextMessage(json.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Ping/Pong message error " + json.toString());
        }
    }

    public void joinToConferenceRoom(String roomName, String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_ROOM_COMMAND);
            json.put(WebSocketConstants.ROOM, roomName);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Connect to conference room JSON error: " + e.getMessage());
        }
    }

    public void leaveFromTheConferenceRoom(String roomName) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.LEAVE_THE_ROOM);
            json.put(WebSocketConstants.ROOM, roomName);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Leave from conference room JSON error: " + e.getMessage());
        }
    }

    public void leaveFromP2P(String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.LEAVE_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Leave from P2P JSON error: " + e.getMessage());
        }
    }

    public void getRoomInfo(String roomName, String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_ROOM_INFO_COMMAND);
            json.put(WebSocketConstants.ROOM, roomName);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Room Info conference room JSON error: " + e.getMessage());
        }
    }

    public void getStreamInfoList(String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_STREAM_INFO_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public AntMediaSignallingEvents getSignallingListener() {
        return signallingListener;
    }

    public boolean isConnected() {
        return ws !=null && ws.isConnected();
    }

    public void forceStreamQuality(String mainTrackStreamId, String subTrackStreamId, int height) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.FORCE_STREAM_QUALITY);
            json.put(WebSocketConstants.STREAM_ID, mainTrackStreamId);
            json.put(WebSocketConstants.TRACK_ID, subTrackStreamId);
            json.put(WebSocketConstants.STREAM_HEIGHT, height);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getBroadcastObject(String streamId) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_BROADCAST_OBJECT_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setupWsReconnection() {
        if(wsReconnectorRunnable != null){
            return;
        }
        wsReconnectorRunnable = () -> {
            wsReconnectionHandler.postDelayed(wsReconnectorRunnable, WEBSOCKET_RECONNECTION_CONTROL_PERIOD_MS);
            if (!isConnected()) {
                connect(wsServerUrl);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!wsReconnectionHandler.hasCallbacks(wsReconnectorRunnable)) {
                wsReconnectionHandler.postDelayed(wsReconnectorRunnable, WEBSOCKET_RECONNECTION_CONTROL_PERIOD_MS);
            }
        } else {
            wsReconnectionHandler.postDelayed(wsReconnectorRunnable, WEBSOCKET_RECONNECTION_CONTROL_PERIOD_MS);
        }


    }

    public void stopReconnector(){
        if(wsReconnectionHandler == null){
            return;
        }
        wsReconnectionHandler.removeCallbacksAndMessages(null);
        wsReconnectionHandler = null;
    }

    public Runnable getWsReconnectorRunnable() {
        return wsReconnectorRunnable;
    }

    public Handler getWsReconnectionHandler() {
        return wsReconnectionHandler;
    }

    public void setWsReconnectionHandler(Handler wsReconnectionHandler) {
        this.wsReconnectionHandler = wsReconnectionHandler;
    }

}
