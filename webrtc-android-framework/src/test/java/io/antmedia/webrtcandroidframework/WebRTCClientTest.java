package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.webrtc.ScreenCapturerAndroid;

import io.antmedia.webrtcandroidframework.apprtc.AppRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class WebRTCClientTest {

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

        webRTCClient.setStreamId(streamId);

        webRTCClient.startStream();

        verify(wsHandler, times(1)).startPublish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, null);

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

        webRTCClient.stopStream();

        verify(wsHandler, times(1)).stop(streamId);
        verify(wsHandler, times(2)).sendTextMessage(jsonCaptor.capture());
        json = new JSONObject();
        try {
            json.put(WebSocketConstants.COMMAND, WebSocketConstants.STOP_COMMAND);
            json.put(WebSocketConstants.STREAM_ID, streamId);
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

        webRTCClient.setStreamId(streamId);

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

        webRTCClient.startStream();
        verify(wsHandler, times(1)).joinToPeer(streamId, token);
        
        webRTCClient.startStream();
        verify(wsHandler, times(1)).getTrackList(streamId, token);

    }

    @Test
    public void testCreateScreenCapturer() {

        IWebRTCListener listener = mock(IWebRTCListener.class);
        Context context = mock(Context.class);
        WebRTCClient webRTCClient = spy(new WebRTCClient(listener, context));

        ScreenCapturerAndroid screenCapturer = (ScreenCapturerAndroid) webRTCClient.createScreenCapturer();
        assertNull(screenCapturer);

        webRTCClient.setMediaProjectionParams(Activity.RESULT_OK, null);
        screenCapturer = (ScreenCapturerAndroid) webRTCClient.createScreenCapturer();
        assertNotNull(screenCapturer);

        MediaProjection.Callback callback = Mockito.spy(screenCapturer.getMediaProjectionCallback());
        callback.onStop();

        Mockito.verify(webRTCClient).reportError("USER_REVOKED_CAPTURE_SCREEN_PERMISSION");
    }

    @Test
    public void testOnActivityResult() {
        IWebRTCListener listener = mock(IWebRTCListener.class);
        Context context = mock(Context.class);
        WebRTCClient webRTCClient = spy(new WebRTCClient(listener, context));

        webRTCClient.changeVideoSource(WebRTCClient.SOURCE_SCREEN);
        Mockito.verify(webRTCClient).startScreenCapture();

        webRTCClient.onActivityResult(0, Activity.RESULT_OK, null);
        assertNotEquals(Activity.RESULT_OK, webRTCClient.getMediaProjectionPermissionResultCode());

        Mockito.doReturn(new DisplayMetrics()).when(webRTCClient).getDisplayMetrics();
        webRTCClient.onActivityResult(CallActivity.CAPTURE_PERMISSION_REQUEST_CODE, Activity.RESULT_OK, null);
        assertEquals(Activity.RESULT_OK, webRTCClient.getMediaProjectionPermissionResultCode());

        Mockito.verify(webRTCClient).createVideoCapturer(WebRTCClient.SOURCE_SCREEN);
    }

    @Test
    public void testReleaseCallback() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(listener, context));

        webRTCClient.handleOnPublishFinished("streamId");

        Mockito.verify(webRTCClient).release(false);

        webRTCClient.handleOnPlayFinished("streamId");
        Mockito.verify(webRTCClient, times(2)).release(false);

        webRTCClient.disconnectWithErrorMessage("error");
        Mockito.verify(webRTCClient, times(1)).release(true);

        webRTCClient.handleOnIceDisconnected();
        Mockito.verify(webRTCClient, times(3)).release(false);

    }




}