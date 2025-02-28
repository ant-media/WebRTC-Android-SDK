package io.antmedia.webrtcandroidframework;

import android.os.Handler;

import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import io.antmedia.webrtcandroidframework.websocket.AntMediaSignallingEvents;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;
import io.antmedia.webrtcandroidframework.websocket.WebSocketConstants;
import io.antmedia.webrtcandroidframework.websocket.WebSocketHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class WebSocketHandlerTest {

    @Mock
    private AntMediaSignallingEvents signallingListener;

    @Mock
    private Handler handler;

    @Mock
    private WebSocketConnection ws;

    @Mock
    private IceCandidate iceCandidate;

    private WebSocketHandler webSocketHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webSocketHandler = spy(new WebSocketHandler(signallingListener, handler));
        webSocketHandler.pingPongExecutor = Executors.newSingleThreadScheduledExecutor();
        doNothing().when(webSocketHandler).sendTextMessage(anyString());
        doNothing().when(webSocketHandler).checkIfCalledOnValidThread();

    }

    @Test
    public void testOnTextMessageStartCommand() {
        String message = "{\"command\": \"start\", \"streamId\": \"stream123\"}";
        doReturn(true).when(webSocketHandler).isConnected();

        webSocketHandler.onTextMessage(message);

        verify(signallingListener).onStartStreaming("stream123");
        
    }

    @Test
    public void testOnTextMessageTakeConfigurationCommand() {
        doReturn(true).when(webSocketHandler).isConnected();

        String streamId = "stream123";

        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.SDP, "testSDP");
            json.put(WebSocketConstants.TYPE, "offer");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();
        webSocketHandler.onTextMessage(message);

        verify(signallingListener).onTakeConfiguration(eq(streamId), any(SessionDescription.class));
    }

    @Test
    public void testOnTextMessageTakeCandidateCommand() {
        doReturn(true).when(webSocketHandler).isConnected();
        String streamId = "stream123";

        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.CANDIDATE_ID, "id123");
            json.put(WebSocketConstants.CANDIDATE_LABEL, 0);
            json.put(WebSocketConstants.CANDIDATE_SDP, "candidateSDP");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();

        webSocketHandler.onTextMessage(message);

        verify(signallingListener).onRemoteIceCandidate(eq(streamId), any(IceCandidate.class));
        
    }

    @Test
    public void testOnTextMessageLeavedTheRoomNotification() {
        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
            json.put(WebSocketConstants.DEFINITION, WebSocketConstants.LEAVED_THE_ROOM);
            json.put(WebSocketConstants.ROOM, "room");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        doReturn(true).when(webSocketHandler).isConnected();
        String message = json.toString();
        webSocketHandler.onTextMessage(message);

        verify(signallingListener).onLeftTheRoom(null);
    }

    @Test
    public void testStartPublish() {
        String streamId = "stream123";
        String token = "token123";
        boolean videoEnabled = true;
        boolean audioEnabled = false;
        String subscriberId = "subscriber123";
        String subscriberCode = "code123";
        String streamName = "testStream";
        String mainTrackId = "track123";

        webSocketHandler.startPublish(streamId, token, videoEnabled, audioEnabled, subscriberId, subscriberCode, streamName, mainTrackId);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

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
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assertEquals(json.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testStartPlay() throws JSONException {
        String streamId = "stream123";
        String token = "token123";
        String[] tracks = {"track1", "track2"};
        String subscriberId = "subscriber123";
        String subscriberCode = "code123";
        String viewerInfo = "viewerInfo";

        webSocketHandler.startPlay(streamId, token, tracks, subscriberId, subscriberCode, viewerInfo);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.TOKEN, token);
        expectedJson.put(WebSocketConstants.TRACK_LIST, new JSONArray(tracks));
        expectedJson.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
        expectedJson.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCode);
        expectedJson.put(WebSocketConstants.VIEWER_INFO, viewerInfo);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testJoinToPeer() throws JSONException {
        String streamId = "stream123";
        String token = "token123";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.joinToPeer(streamId, token);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.TOKEN, token);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testSendConfiguration() throws JSONException {
        String streamId = "stream123";
        String sdp = "testSDP";
        String type = "offer";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.sendConfiguration(streamId, new SessionDescription(SessionDescription.Type.OFFER, sdp), type);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CONFIGURATION_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.SDP, sdp);
        expectedJson.put(WebSocketConstants.TYPE, type);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testSendLocalIceCandidate() throws JSONException {
        String streamId = "stream123";
        int label = 1;
        String id = "candidate123";
        String candidate = "testCandidate";


        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.sendLocalIceCandidate(streamId, new IceCandidate(id, label, candidate));

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.TAKE_CANDIDATE_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.CANDIDATE_LABEL, label);
        expectedJson.put(WebSocketConstants.CANDIDATE_ID, id);
        expectedJson.put(WebSocketConstants.CANDIDATE_SDP, candidate);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testGetTrackList() throws JSONException {
        String streamId = "stream123";
        String token = "token123";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.getTrackList(streamId, token);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_TRACK_LIST);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.TOKEN, token);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testEnableTrack() throws JSONException {
        String streamId = "stream123";
        String trackId = "track456";
        boolean enabled = true;

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.enableTrack(streamId, trackId, enabled);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.ENABLE_TRACK);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.TRACK_ID, trackId);
        expectedJson.put(WebSocketConstants.ENABLED, enabled);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testSendPingPongMessage() throws JSONException {
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.sendPingPongMessage();

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.PING_COMMAND);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testJoinToConferenceRoom() throws JSONException {
        String roomName = "conferenceRoom123";
        String streamId = "stream123";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.joinToConferenceRoom(roomName, streamId);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.JOIN_ROOM_COMMAND);
        expectedJson.put(WebSocketConstants.ROOM, roomName);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testLeaveFromTheConferenceRoom() throws JSONException {
        String roomName = "conferenceRoom123";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.leaveFromTheConferenceRoom(roomName);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.LEAVE_THE_ROOM);
        expectedJson.put(WebSocketConstants.ROOM, roomName);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testGetRoomInfo() throws JSONException {
        String roomName = "conferenceRoom123";
        String streamId = "stream123";

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.getRoomInfo(roomName, streamId);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_ROOM_INFO_COMMAND);
        expectedJson.put(WebSocketConstants.ROOM, roomName);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testGetStreamInfoList() throws JSONException {
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        String streamId = "stream123";

        webSocketHandler.getStreamInfoList(streamId);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_STREAM_INFO_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testForceStreamQuality() throws JSONException {
        String streamId = "stream123";
        int height = 720;

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        webSocketHandler.forceStreamQuality(streamId, "", height);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.FORCE_STREAM_QUALITY);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);
        expectedJson.put(WebSocketConstants.TRACK_ID, "");
        expectedJson.put(WebSocketConstants.STREAM_HEIGHT, height);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testGetBroadcastObject() throws JSONException {
        String streamId = "stream123";
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        webSocketHandler.getBroadcastObject(streamId);

        verify(webSocketHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject expectedJson = new JSONObject();
        expectedJson.put(WebSocketConstants.COMMAND, WebSocketConstants.GET_BROADCAST_OBJECT_COMMAND);
        expectedJson.put(WebSocketConstants.STREAM_ID, streamId);

        assertEquals(expectedJson.toString(), jsonCaptor.getValue());
    }

    @Test
    public void testOnBroadcastObjectNotification() {
        doReturn(true).when(webSocketHandler).isConnected();

        Broadcast broadcast = new Broadcast();
        broadcast.setStreamId("streamId");
        broadcast.setName("name");

        Gson gson = new Gson();
        JsonElement broadcastJson = gson.toJsonTree(broadcast);

        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.NOTIFICATION_COMMAND);
            json.put(WebSocketConstants.DEFINITION, WebSocketConstants.BROADCAST_OBJECT_NOTIFICATION);
            json.put(WebSocketConstants.STREAM_ID, broadcast.getStreamId());
            json.put(WebSocketConstants.BROADCAST, broadcastJson);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String message = json.toString();

        webSocketHandler.onTextMessage(message);

        ArgumentCaptor<Broadcast> captor = ArgumentCaptor.forClass(Broadcast.class);
        verify(signallingListener).onBroadcastObject(captor.capture());

        Broadcast broadcast2 = captor.getValue();

        assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());
        assertEquals(broadcast.getName(), broadcast2.getName());

    }

    @Test
    public void testWsConnect() throws InterruptedException, URISyntaxException, WebSocketException {
        String url = "wss://test.antmedia.io:5443/LiveApp/websocket";
        doReturn(ws).when(webSocketHandler).creteWebSocket();
        webSocketHandler.connect(url);
        Thread.sleep(3000);
        verify(ws,times(1)).connect(new URI(url),webSocketHandler);
    }

    @Test
    public void testWsReconnection(){
        Handler wsReconnectionHandlerMock = mock(Handler.class);

        webSocketHandler.setWsReconnectionHandler(wsReconnectionHandlerMock);

        doReturn(false).when(webSocketHandler).isConnected();
        webSocketHandler.setupWsReconnection();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        verify(wsReconnectionHandlerMock, times(1)).postDelayed(runnableCaptor.capture(), eq(WebSocketHandler.WEBSOCKET_RECONNECTION_CONTROL_PERIOD_MS));

        Runnable capturedRunnable = runnableCaptor.getValue();

        doNothing().when(webSocketHandler).connect(anyString());

        capturedRunnable.run();

        verify(webSocketHandler, times(1)).connect(anyString());

        doReturn(true).when(webSocketHandler).isConnected();

        capturedRunnable.run();

        verify(webSocketHandler, times(1)).connect(anyString());

    }

}
