package io.antmedia.webrtc_android_sample_app;

import android.content.Context;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.WebSocketConstants;
import io.antmedia.webrtcandroidframework.WebSocketHandler;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.PeerConnectionClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }


    /**
     * Write test scenarios and implement it.
     * Until that time, manuel test may be used
     */
    public void testStopStartStream() {
        //1. start stream and check that it's playing

        //2. stop stream and check that it's stopped

        //3. start stream again and check it's is started
    }

    @Test
    public void testStreamPublishParameters() {
        boolean videoCallEnabled = RandomUtils.nextBoolean();
        boolean audioCallEnabled = RandomUtils.nextBoolean();
        String streamId = "stream" + RandomStringUtils.random(5);
        String mode = IWebRTCClient.MODE_PUBLISH;
        String token = "token" + RandomStringUtils.random(5);
        String subscriberId = "mySubscriber" + RandomStringUtils.random(5);
        String subscriberCode = "code" + RandomStringUtils.random(5);
        String streamName = "stream" + RandomStringUtils.random(5);

        IWebRTCListener listener = mock(IWebRTCListener.class);
        Context context = mock(Context.class);
        WebRTCClient webRTCClient = spy(new WebRTCClient(listener, context));
        WebSocketHandler wsHandler = spy(new WebSocketHandler(null, null));
        webRTCClient.setWsHandler(wsHandler);

        webRTCClient.setSubscriberParams(subscriberId, subscriberCode);
        webRTCClient.setStreamName(streamName);

        when(context.getString(anyInt(), Matchers.<Object>anyVararg())).thenReturn("asas");
        doNothing().when(webRTCClient).init(anyString(), anyString(), anyString(), anyString(), any());
        doReturn(true).when(wsHandler).isConnected();
        doNothing().when(wsHandler).checkIfCalledOnValidThread();
        doNothing().when(wsHandler).sendTextMessage(anyString());

        AppRTCClient.RoomConnectionParameters roomConnectionParameters =
                new AppRTCClient.RoomConnectionParameters("", streamId, false, "", mode, token);
        webRTCClient.setRoomConnectionParametersForTest(roomConnectionParameters);


        PeerConnectionClient.PeerConnectionParameters peerConnectionParameters
                = new PeerConnectionClient.PeerConnectionParameters(videoCallEnabled, false, false, 0, 0, 0,
                0, "", false, false, 0, "",
                false, false, false, false, false, false, false, false,
                false, null, audioCallEnabled);
        webRTCClient.setPeerConnectionParametersForTest(peerConnectionParameters);


        webRTCClient.startStream();

        verify(wsHandler, times(1)).startPublish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(wsHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PUBLISH_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCode);
            json.put(WebSocketConstants.STREAM_NAME, streamName);
            json.put(WebSocketConstants.VIDEO, videoCallEnabled);
            json.put(WebSocketConstants.AUDIO, audioCallEnabled);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assertEquals(json.toString(), jsonCaptor.getValue());
    }


    @Test
    public void testStreamPlayParameters() {
        boolean videoCallEnabled = RandomUtils.nextBoolean();
        boolean audioCallEnabled = RandomUtils.nextBoolean();
        String streamId = "stream" + RandomStringUtils.random(5);
        String mode = IWebRTCClient.MODE_PLAY;
        String token = "token" + RandomStringUtils.random(5);
        String subscriberId = "mySubscriber" + RandomStringUtils.random(5);
        String subscriberCode = "code" + RandomStringUtils.random(5);
        String viewerInfo = "info" + RandomStringUtils.random(5);
        String [] tracks = null;


        IWebRTCListener listener = mock(IWebRTCListener.class);
        Context context = mock(Context.class);
        WebRTCClient webRTCClient = spy(new WebRTCClient(listener, context));
        WebSocketHandler wsHandler = spy(new WebSocketHandler(null, null));
        webRTCClient.setWsHandler(wsHandler);

        webRTCClient.setSubscriberParams(subscriberId, subscriberCode);
        webRTCClient.setViewerInfo(viewerInfo);

        when(context.getString(anyInt(), Matchers.<Object>anyVararg())).thenReturn("asas");
        doNothing().when(webRTCClient).init(anyString(), anyString(), anyString(), anyString(), any());
        doReturn(true).when(wsHandler).isConnected();
        doNothing().when(wsHandler).checkIfCalledOnValidThread();
        doNothing().when(wsHandler).sendTextMessage(anyString());

        AppRTCClient.RoomConnectionParameters roomConnectionParameters =
                new AppRTCClient.RoomConnectionParameters("", streamId, false, "", mode, token);
        webRTCClient.setRoomConnectionParametersForTest(roomConnectionParameters);


        PeerConnectionClient.PeerConnectionParameters peerConnectionParameters
                = new PeerConnectionClient.PeerConnectionParameters(videoCallEnabled, false, false, 0, 0, 0,
                0, "", false, false, 0, "",
                false, false, false, false, false, false, false, false,
                false, null, audioCallEnabled);
        webRTCClient.setPeerConnectionParametersForTest(peerConnectionParameters);


        webRTCClient.startStream();

        verify(wsHandler, times(1)).startPlay(streamId, token, tracks, subscriberId, subscriberCode, viewerInfo);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(wsHandler, times(1)).sendTextMessage(jsonCaptor.capture());

        JSONObject json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.PLAY_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
            json.put(WebSocketConstants.TOKEN, token);
            json.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
            json.put(WebSocketConstants.SUBSCRIBER_CODE, subscriberCode);
            json.put(WebSocketConstants.VIEWER_INFO, viewerInfo);
            json.put(WebSocketConstants.TRACK_LIST, new JSONArray());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        assertEquals(json.toString(), jsonCaptor.getValue());
    }
}