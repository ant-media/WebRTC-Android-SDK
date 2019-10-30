package io.antmedia.webrtcandroidframework;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;

import static io.antmedia.webrtcandroidframework.WebSocketConstants.DEFINITION;
import static io.antmedia.webrtcandroidframework.WebSocketConstants.NOTIFICATION_COMMAND;

public class WebSocketHandler implements WebSocket.WebSocketConnectionObserver {
    private static final String TAG = "WSChannelRTCClient";
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


    public WebSocketHandler(AntMediaSignallingEvents signallingListener, Handler handler) {
        this.handler = handler;
        this.signallingListener = signallingListener;
    }

    public void connect(final String wsUrl) {
        checkIfCalledOnValidThread();
        wsServerUrl = wsUrl;
        Log.d(TAG, "Connecting WebSocket to: " + wsUrl);
        ws = new WebSocketConnection();
        try {
            ws.connect(new URI(wsServerUrl), this);
        } catch (WebSocketException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void sendTextMessage(String message) {
        Log.e(TAG, "sent websocket message:" + message);
        ws.sendTextMessage(message);
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
        signallingListener.onDisconnected();
        Log.d(TAG, "Disconnecting WebSocket done.");
    }

    private void checkIfCalledOnValidThread() {
        if (Thread.currentThread() != handler.getLooper().getThread()) {
            throw new IllegalStateException("WebSocket method is not called on valid thread");
        }
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onClose(WebSocketCloseNotification webSocketCloseNotification, String s) {
        Log.d(TAG, "WebSocket connection closed.");
        synchronized (closeEventLock) {
            closeEvent = true;
            closeEventLock.notify();
        }
    }

    @Override
    public void onTextMessage(String msg) {
        Log.e(TAG, "onTextMessage: "+msg);
        if (!ws.isConnected()) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }
        try {
            JSONObject json = new JSONObject(msg);

            String commandText = json.getString(WebSocketConstants.COMMAND);
            String streamId = json.getString(WebSocketConstants.STREAM_ID);
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
            else if (commandText.equals(NOTIFICATION_COMMAND)) {

                String definition= json.getString(DEFINITION);

                Log.d(TAG, "notification:   "+ definition);
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
                else if (definition.equals(WebSocketConstants.STREAM_JOINED)) {
                    signallingListener.onStreamJoined(streamId);
                }
                else if (definition.equals(WebSocketConstants.STREAM_LEAVED)) {
                    signallingListener.onStreamLeaved(streamId);
                }
            }
            else if (commandText.equals(WebSocketConstants.ERROR_COMMAND))
            {

                String definition= json.getString(DEFINITION);
                Log.d(TAG, "error command received: "+ definition);
                stopPingPongTimer();

                if (definition.equals(WebSocketConstants.NO_STREAM_EXIST))
                {
                    signallingListener.noStreamExistsToPlay(streamId);
                    disconnect(true);
                }
            }

            else if (commandText.equals(WebSocketConstants.PONG_COMMAND))
            {
                pingPongTimoutCount = 0;
                Log.d(TAG, "pong reply is received");
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

    public void startPublish(String streamId, String token, boolean videoEnabled){
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketRTCAntMediaClient.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
            json.put(WebSocketRTCAntMediaClient.STREAM_ID, streamId);
            json.put(WebSocketRTCAntMediaClient.TOKEN_ID, token);
            json.put(WebSocketRTCAntMediaClient.VIDEO, videoEnabled);
            json.put(WebSocketRTCAntMediaClient.AUDIO, true);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void startPlay(String streamId, String token){
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
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

    public void sendConfiguration(String streamId, final SessionDescription sdp, String type) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketRTCAntMediaClient.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
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
            json.put(WebSocketRTCAntMediaClient.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.CANDIDATE_LABEL, candidate.sdpMLineIndex);
            json.put(WebSocketConstants.CANDIDATE_ID, candidate.sdpMid);
            json.put(WebSocketConstants.CANDIDATE_SDP, candidate.sdp);
            sendTextMessage(json.toString());
        } catch (JSONException e) {
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
            ws.sendTextMessage(json.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Ping/Pong message error " + json.toString());
        }
    }

    public void joinToConferenceRoom(String roomName) {
        checkIfCalledOnValidThread();
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_ROOM_COMMAND);
            json.put(WebSocketConstants.ROOM, roomName);
            json.put(WebSocketConstants.STREAM_ID, null);
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
            Log.e(TAG, "Connect to conference room JSON error: " + e.getMessage());
        }
    }

    public AntMediaSignallingEvents getSignallingListener() {
        return signallingListener;
    }
}
