package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.IceCandidateErrorEvent;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager;
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

        webRTCClient.setStreamId(streamId);
        webRTCClient.setStreamMode(mode);
        webRTCClient.setAudioEnabled(audioCallEnabled);
        webRTCClient.setVideoEnabled(videoCallEnabled);
        webRTCClient.setToken(token);

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
    public void testOnAudioManagerDevicesChanged() {
        // Create a mock of the AppRTCAudioManager and AudioManager
        AppRTCAudioManager audioManager = Mockito.mock(AppRTCAudioManager.class);
        AudioManager systemAudioManager = Mockito.mock(AudioManager.class);

        // Create the test instance
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(null, mock(Context.class)));

        // Set up the test data
        AppRTCAudioManager.AudioDevice device = AppRTCAudioManager.AudioDevice.BLUETOOTH;
        Set<AppRTCAudioManager.AudioDevice> availableDevices = new HashSet<>();
        availableDevices.add(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE);
        availableDevices.add(AppRTCAudioManager.AudioDevice.BLUETOOTH);

        webRTCClient.audioManager = null;

        // Invoke the method under test
        webRTCClient.onAudioManagerDevicesChanged(device, availableDevices);

        // Verify that the audio device is not selected using the AudioManager
        Mockito.verify(audioManager, never()).selectAudioDevice(device);

        webRTCClient.audioManager = audioManager;

        // Invoke the method under test
        webRTCClient.onAudioManagerDevicesChanged(device, availableDevices);

        // Verify that the audio device is selected using the AudioManager
        Mockito.verify(audioManager).selectAudioDevice(device);
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
        String[] tracks = null;


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
        webRTCClient.setStreamMode(mode);
        webRTCClient.setAudioEnabled(audioCallEnabled);
        webRTCClient.setVideoEnabled(videoCallEnabled);
        webRTCClient.setToken(token);

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

        webRTCClient.setStreamMode(IWebRTCClient.MODE_JOIN);

        webRTCClient.startStream();
        verify(wsHandler, times(1)).joinToPeer(streamId, token);

        webRTCClient.setStreamMode(IWebRTCClient.MODE_MULTI_TRACK_PLAY);

        webRTCClient.startStream();
        verify(wsHandler, times(1)).getTrackList(streamId, token);

    }

    @Test
    public void testCreateScreenCapturer() {

        IWebRTCListener listener = mock(IWebRTCListener.class);
        Context context = mock(Context.class);
        WebRTCClient webRTCClient = spy(new WebRTCClient(listener, context));
        String streamId = "stream";
        webRTCClient.setStreamId(streamId);

        ScreenCapturerAndroid screenCapturer = (ScreenCapturerAndroid) webRTCClient.createScreenCapturer();
        assertNull(screenCapturer);

        webRTCClient.setMediaProjectionParams(Activity.RESULT_OK, null);
        screenCapturer = (ScreenCapturerAndroid) webRTCClient.createScreenCapturer();
        assertNotNull(screenCapturer);

        MediaProjection.Callback callback = Mockito.spy(screenCapturer.getMediaProjectionCallback());
        callback.onStop();

        Mockito.verify(webRTCClient).reportError(streamId, "USER_REVOKED_CAPTURE_SCREEN_PERMISSION");
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

        Mockito.doNothing().when(webRTCClient).changeVideoCapturer(any());
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
        Mockito.doNothing().when(webRTCClient).release(anyBoolean());
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        webRTCClient.onPublishFinished("streamId");

        Mockito.verify(webRTCClient, timeout(1000)).release(false);

        webRTCClient.onPlayFinished("streamId");
        Mockito.verify(webRTCClient, times(2)).release(false);

        webRTCClient.disconnectWithErrorMessage("error");
        Mockito.verify(webRTCClient, times(1)).release(true);

        webRTCClient.onIceDisconnected("streamId");
        Mockito.verify(webRTCClient, times(2)).release(false);

    }

    @Test
    public void testInitilization() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(listener, context));
        Mockito.doNothing().when(webRTCClient).initializeRenderers();
        Mockito.doNothing().when(webRTCClient).initializePeerConnectionFactory();
        Mockito.doNothing().when(webRTCClient).initializeVideoCapturer();
        Mockito.doNothing().when(webRTCClient).initializeAudioManager();
        Mockito.doNothing().when(webRTCClient).connectWebSocket();
        Mockito.doReturn(true).when(webRTCClient).checkPermissions(any());

        when(context.getString(R.string.pref_maxvideobitratevalue_default)).thenReturn("500");
        when(context.getString(R.string.pref_startaudiobitratevalue_default)).thenReturn("500");


        Intent intent = Mockito.mock(Intent.class);
        when(intent.getBooleanExtra(CallActivity.EXTRA_VIDEO_CALL, true)).thenReturn(false);
        when(intent.getBooleanExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, true)).thenReturn(true);

        webRTCClient.init("http://my.ams:5080/myapp/websocket", "stream", WebRTCClient.MODE_PUBLISH, "token", intent);

        assertEquals(false, webRTCClient.getVideoCallEnabled());
        assertEquals(true, webRTCClient.isDataChannelEnabled());
    }

    @Test
    public void testInitilizeVideoCapturer() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(listener, context));
        Mockito.doNothing().when(webRTCClient).initializeRenderers();
        Mockito.doReturn(true).when(webRTCClient).useCamera2();

        webRTCClient.setVideoCallEnabled(true);
        webRTCClient.initializeVideoCapturer();

        assertEquals(WebRTCClient.SOURCE_FRONT, webRTCClient.getCurrentSource());
    }


    @Test
    public void testAccessors() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(listener, context));

        webRTCClient.setStreamMode(IWebRTCClient.MODE_PLAY);
        assertEquals(IWebRTCClient.MODE_PLAY, webRTCClient.getStreamMode());


    }

    @Test
    public void testPCObserver() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, context);
        WebRTCClient webRTCClient = spy(webRTCClientReal);

        doNothing().when(webRTCClient).release(anyBoolean());

        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClient.setWsHandler(wsHandler);

        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        webRTCClient.setStreamId(streamId);

        WebRTCClient.PCObserver pcObserver = webRTCClient.getPCObserver(streamId);
        assertNotNull(pcObserver);

        IceCandidate iceCandidate = mock(IceCandidate.class);
        pcObserver.onIceCandidate(iceCandidate);
        verify(wsHandler, timeout(1000)).sendLocalIceCandidate(streamId, iceCandidate);

        pcObserver.onIceCandidateError(new IceCandidateErrorEvent("address", 5090, "url", 5, "errorText"));
        pcObserver.onSignalingChange(PeerConnection.SignalingState.CLOSED);

        pcObserver.onConnectionChange(PeerConnection.PeerConnectionState.CONNECTED);
        pcObserver.onConnectionChange(PeerConnection.PeerConnectionState.DISCONNECTED);
        verify(listener, timeout(1000)).onDisconnected(streamId);

        pcObserver.onConnectionChange(PeerConnection.PeerConnectionState.FAILED);
        verify(listener, timeout(1000)).onError(anyString(), eq(streamId));

        pcObserver.onIceConnectionChange(PeerConnection.IceConnectionState.CONNECTED);
        verify(listener, timeout(1000)).onIceConnected(streamId);

        pcObserver.onIceConnectionChange(PeerConnection.IceConnectionState.DISCONNECTED);
        verify(listener, timeout(1000)).onIceDisconnected(streamId);

        pcObserver.onIceConnectionChange(PeerConnection.IceConnectionState.FAILED);
        verify(listener, timeout(1000)).onError(anyString(), eq(streamId));

        webRTCClient.setDataChannelEnabled(true);
        webRTCClient.peers.put(streamId, mock(WebRTCClient.PeerInfo.class));

        pcObserver.onIceGatheringChange(PeerConnection.IceGatheringState.COMPLETE);
        pcObserver.onIceConnectionReceivingChange(true);
        pcObserver.onAddStream(new MediaStream(0));
        pcObserver.onRemoveStream(new MediaStream(0));
        pcObserver.onRenegotiationNeeded();
        pcObserver.onDataChannel(mock(DataChannel.class));
        
        webRTCClient.getRemoteSinks().add(mock(VideoSink.class));
        MediaStream[] tracks = {new MediaStream(0)};
        PeerConnection pc = mock(PeerConnection.class);
        RtpTransceiver transceiver = mock(RtpTransceiver.class);
        RtpReceiver receiver = mock(RtpReceiver.class);
        when(transceiver.getReceiver()).thenReturn(receiver);
        VideoTrack videoTrack= mock(VideoTrack.class);
        when(receiver.track()).thenReturn(videoTrack);
        when(pc.getTransceivers()).thenReturn(Arrays.asList(transceiver));
        webRTCClient.addPeerConnection(streamId, pc);
        webRTCClient.setRenderersProvidedAtStart(true);
        webRTCClient.setStreamMode(IWebRTCClient.MODE_PLAY);
        pcObserver.onAddTrack(receiver, tracks);
        verify(videoTrack, times(1)).addSink(any(VideoSink.class));

        pcObserver.onRemoveTrack(mock(RtpReceiver.class));
        assertNotNull(webRTCClient);

    }

    @Nullable
    private Handler getMockHandler() {
        final Handler handler = mock(Handler.class);
        when(handler.post(any(Runnable.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }

        });

        when(handler.postDelayed(any(Runnable.class), anyLong())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Long delay = invocation.getArgumentAt(1, Long.class);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        invocation.getArgumentAt(0, Runnable.class).run();
                    }
                });
                thread.start();
                return null;
            }

        });
        return handler;
    }

    @Test
    public void testSDPObserver() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = new WebRTCClient(listener, context);

        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        when(wsHandler.getSignallingListener()).thenReturn(webRTCClient);
        webRTCClient.setWsHandler(wsHandler);

        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        webRTCClient.setStreamId(streamId);

        WebRTCClient.SDPObserver sdpObserver = webRTCClient.getSdpObserver(streamId);
        assertNotNull(sdpObserver);

        PeerConnection pc = mock(PeerConnection.class);
        webRTCClient.addPeerConnection(streamId, pc);

        SessionDescription sessionDescription = mock(SessionDescription.class);
        sdpObserver.onCreateSuccess(sessionDescription);
        verify(pc, timeout(1000)).setLocalDescription(eq(sdpObserver), any());

        {
            webRTCClient.setInitiator(true);
            sdpObserver.onSetSuccess();
            verify(wsHandler, timeout(1000)).sendConfiguration(eq(streamId), any(), eq("offer"));
        }
        {
            when(pc.getLocalDescription()).thenReturn(mock(SessionDescription.class));
            webRTCClient.setInitiator(false);
            sdpObserver.onSetSuccess();
            verify(wsHandler, timeout(1000)).sendConfiguration(eq(streamId), any(), eq("answer"));
        }
        sdpObserver.onCreateFailure("error");
        sdpObserver.onSetFailure("error");

        verify(wsHandler, timeout(1000)).disconnect(true);
    }

    @Test
    public void testAudioVideoEnablement() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = Mockito.spy(new WebRTCClient(listener, context));

        when(context.getString(R.string.pref_maxvideobitratevalue_default)).thenReturn("500");
        when(context.getString(R.string.pref_startaudiobitratevalue_default)).thenReturn("500");

        Intent intent = Mockito.mock(Intent.class);

        webRTCClient.setStreamMode(WebRTCClient.MODE_PLAY);
        webRTCClient.initializeParameters();
        assertEquals(false, webRTCClient.getVideoCallEnabled());
        assertEquals(true, webRTCClient.getAudioCallEnabled());

        webRTCClient.setVideoEnabled(false);
        webRTCClient.setAudioEnabled(false);

        webRTCClient.setStreamMode(WebRTCClient.MODE_MULTI_TRACK_PLAY);
        webRTCClient.initializeParameters();
        assertEquals(false, webRTCClient.getVideoCallEnabled());
        assertEquals(true, webRTCClient.getAudioCallEnabled());

        webRTCClient.setVideoEnabled(false);
        webRTCClient.setAudioEnabled(false);

        webRTCClient.setStreamMode(WebRTCClient.MODE_TRACK_BASED_CONFERENCE);
        webRTCClient.initializeParameters();
        assertEquals(true, webRTCClient.getVideoCallEnabled());
        assertEquals(true, webRTCClient.getAudioCallEnabled());

        webRTCClient.setVideoEnabled(false);
        webRTCClient.setAudioEnabled(false);

        webRTCClient.setStreamMode("some other mode");
        webRTCClient.initializeParameters();
        assertEquals(false, webRTCClient.getVideoCallEnabled());
        assertEquals(false, webRTCClient.getAudioCallEnabled());


    }

    @Test
    public void testAVideoRotationExtention() {
        String streamId = "stream1";
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Context context = Mockito.mock(Context.class);
        WebRTCClient webRTCClient = new WebRTCClient(listener, context);
        PeerConnection pc = mock(PeerConnection.class);
        webRTCClient.addPeerConnection(streamId, pc);

        String fakeSdp = "something\r\n" +
                WebRTCClient.VIDEO_ROTATION_EXT_LINE +
                "something else\r\n";

        webRTCClient.getSdpObserver(streamId).onCreateSuccess(new SessionDescription(SessionDescription.Type.OFFER, fakeSdp));
        assertTrue(webRTCClient.getLocalDescription().description.contains(WebRTCClient.VIDEO_ROTATION_EXT_LINE));


        webRTCClient.setRemoveVideoRotationExtention(true);
        webRTCClient.getSdpObserver(streamId).onCreateSuccess(new SessionDescription(SessionDescription.Type.OFFER, fakeSdp));

        assertFalse(webRTCClient.getLocalDescription().description.contains(WebRTCClient.VIDEO_ROTATION_EXT_LINE));

    }

    @Test
    public void testSendPlayOtherTracks() {
        WebRTCClient webRTCClient = spy(new WebRTCClient(null, mock(Context.class)));
        webRTCClient.setAutoPlayTracks(true);
        webRTCClient.setSelfStreamId("self");
        String tracks[] = {"other1", "self", "other2"};

        doNothing().when(webRTCClient).init(anyString(), anyString(), anyString(), anyString(), any());
        doNothing().when(webRTCClient).play(anyString(), anyString(), any(), anyString(), anyString(), anyString());
        doReturn(false).when(webRTCClient).isStreaming();

        webRTCClient.sendPlayOtherTracks(tracks);

        ArgumentCaptor<String[]> tracksCaptor = ArgumentCaptor.forClass(String[].class);
        verify(webRTCClient, times(1)).play(anyString(), anyString(), tracksCaptor.capture());

        String[] capturedTracks = tracksCaptor.getValue();
        assertEquals("other1", capturedTracks[0]);
        assertEquals("!self", capturedTracks[1]);
        assertEquals("other2", capturedTracks[2]);
    }

    @Test
    public void testReconnection() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        webRTCClientReal.setWsHandler(mock(WebSocketHandler.class));

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        webRTCClient.createReconnectionRunnable();

        String streamId = "stream1";
        webRTCClient.setStreamId(streamId);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);
        webRTCClient.setReconnectionEnabled(true);
        webRTCClient.setReconnectionHandler(handler);


        doNothing().when(webRTCClient).init(anyString(), anyString(), anyString(), anyString(), any());

        String playStreamId = "playStreamId";
        webRTCClient.play(playStreamId, "", null, "", "", "");

        String publishStreamId = "publishStreamId";
        webRTCClient.publish(publishStreamId, "", true, true, "","", "", "");

        webRTCClient.onDisconnected();

        verify(listener, timeout(1000)).onDisconnected(streamId);

        verify(webRTCClient, timeout(WebRTCClient.RECONNECTION_CONTROL_PERIOD_MLS).atLeast(2)).play(anyString(), anyString(), any(), anyString(), anyString(), anyString());
        verify(webRTCClient, timeout(WebRTCClient.RECONNECTION_CONTROL_PERIOD_MLS).atLeast(2)).publish(anyString(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testWSAndListenerMessages() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        String room = "room1";

        webRTCClient.joinToConferenceRoom(room, streamId);
        verify(wsHandler, timeout(1000)).joinToConferenceRoom(room, streamId);

        webRTCClient.leaveFromConference(room);
        verify(wsHandler, timeout(1000)).leaveFromTheConferenceRoom(room);

        webRTCClient.getRoomInfo(room, streamId);
        verify(wsHandler, timeout(1000)).getRoomInfo(room, streamId);

        String[] streams = new String[1];

        webRTCClient.onJoinedTheRoom(streamId, streams);
        verify(listener, timeout(1000)).onJoinedTheRoom(streamId, streams);

        webRTCClient.onRoomInformation(streams);
        verify(listener, timeout(1000)).onRoomInformation(streams);

        webRTCClient.noStreamExistsToPlay(streamId);
        verify(listener, timeout(1000)).noStreamExistsToPlay(streamId);

        webRTCClient.onLeftTheRoom(room);
        verify(listener, timeout(1000)).onLeftTheRoom(room);

        webRTCClient.streamIdInUse(streamId);
        verify(listener, timeout(1000)).streamIdInUse(streamId);

        doNothing().when(webRTCClient).addRemoteIceCandidate(anyString(), any());
        IceCandidate mockCandidate = mock(IceCandidate.class);
        webRTCClient.onRemoteIceCandidate(streamId, mockCandidate);
        verify(webRTCClient, timeout(1000)).addRemoteIceCandidate(streamId, mockCandidate);

        doNothing().when(webRTCClient).sendPlayOtherTracks(any());
        webRTCClient.onTrackList(streams);
        verify(listener, timeout(1000)).onTrackList(streams);
        verify(webRTCClient, timeout(1000)).sendPlayOtherTracks(streams);

        ArrayList<StreamInfo> streamInfoList = new ArrayList<>();
        webRTCClient.onStreamInfoList(streamId, streamInfoList);
        verify(listener, timeout(1000)).onStreamInfoList(streamId, streamInfoList);

        String errorDefinition = "no_stream_exist";
        webRTCClient.onError(streamId, errorDefinition);
        verify(listener, timeout(1000)).onError(errorDefinition, streamId);

        webRTCClient.onBitrateMeasurement(streamId, 1000, 500, 100);
        verify(listener, timeout(1000)).onBitrateMeasurement(streamId, 1000, 500, 100);
    }

    @Test
    public void testCheckPermissions() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        webRTCClient.setStreamMode(WebRTCClient.MODE_TRACK_BASED_CONFERENCE);
        webRTCClient.checkPermissions(any());
        verify(listener).checkAndRequestPermisssions(eq(true), any());

        webRTCClient.setStreamMode(WebRTCClient.MODE_PUBLISH);
        webRTCClient.checkPermissions(any());
        verify(listener, times(2)).checkAndRequestPermisssions(eq(true), any());

        webRTCClient.setStreamMode(WebRTCClient.MODE_PLAY);
        webRTCClient.checkPermissions(any());
        verify(listener).checkAndRequestPermisssions(eq(false), any());

    }

    @Test
    public void testMultiplePlayAdaptations() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);
        String initialStreamId = "initialStreamId";
        webRTCClientReal.setStreamId(initialStreamId);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        when(wsHandler.isConnected()).thenReturn(true);

        String streamId = "stream1";
        webRTCClient.stopStream(streamId);
        verify(wsHandler, timeout(1000)).stop(streamId);

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, WebRTCClient.MODE_PUBLISH);
        PeerConnection pc = mock(PeerConnection.class);
        when(pc.iceConnectionState()).thenReturn(PeerConnection.IceConnectionState.CONNECTED);
        peerInfo.peerConnection = pc;
        webRTCClient.peers.put(initialStreamId, peerInfo);
        assertTrue(webRTCClient.isStreaming());
        when(pc.iceConnectionState()).thenReturn(PeerConnection.IceConnectionState.DISCONNECTED);
        assertFalse(webRTCClient.isStreaming());

        webRTCClient.getStreamInfoList();
        verify(wsHandler, timeout(1000)).getStreamInfoList(initialStreamId);

        webRTCClient.forceStreamQuality(360);
        verify(wsHandler, timeout(1000)).forceStreamQuality(initialStreamId, 360);

        assertEquals(initialStreamId, webRTCClient.getStreamId());

        DataChannel.Buffer buffer = mock(DataChannel.Buffer.class);
        webRTCClient.sendMessageViaDataChannel(buffer);
        verify(webRTCClient, timeout(1000)).sendMessageViaDataChannel(initialStreamId, buffer);

    }


    @Test
    public void testOnStartStreaming() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        doNothing().when(webRTCClient).createPeerConnection(streamId);
        doNothing().when(webRTCClient).createOffer(streamId);
        webRTCClient.onStartStreaming(streamId);

        verify(webRTCClient, timeout(1000)).createPeerConnection(streamId);
        verify(webRTCClient, timeout(1000)).createOffer(streamId);



    }

    @Test
    public void testOnTakeConfiguration() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";

        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, "sdp");

        doNothing().when(webRTCClient).createPeerConnection(streamId);
        doNothing().when(webRTCClient).setRemoteDescription(streamId, sdp);
        doNothing().when(webRTCClient).createAnswer(streamId);

        webRTCClient.peers.put(streamId, new WebRTCClient.PeerInfo(streamId, WebRTCClient.MODE_PUBLISH));

        webRTCClient.onTakeConfiguration(streamId, sdp);

        verify(webRTCClient).createPeerConnection(streamId);
        verify(webRTCClient).setRemoteDescription(streamId, sdp);
        verify(webRTCClient).createAnswer(streamId);

        SessionDescription sdpAnswer = new SessionDescription(SessionDescription.Type.ANSWER, "sdp");
        webRTCClient.onTakeConfiguration(streamId, sdpAnswer);

        verify(webRTCClient, times(1)).createPeerConnection(streamId);
        verify(webRTCClient, times(1)).setRemoteDescription(streamId, sdpAnswer);
        verify(webRTCClient, times(1)).createAnswer(streamId);
    }

    @Test
    public void testDatachannel() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        webRTCClient.setStreamMode(WebRTCClient.MODE_PUBLISH);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        IDataChannelObserver dcObserver = mock(IDataChannelObserver.class);
        webRTCClient.setDataChannelObserver(dcObserver);

        String streamId = "stream1";

        WebRTCClient.PeerInfo peerInfo =  new WebRTCClient.PeerInfo(streamId, WebRTCClient.MODE_PUBLISH);
        PeerConnection pc = mock(PeerConnection.class);
        DataChannel dc = mock(DataChannel.class);
        when(pc.createDataChannel(anyString(), any())).thenReturn(dc);
        peerInfo.peerConnection = pc;
        webRTCClient.peers.put(streamId, peerInfo);
        webRTCClient.setDataChannelEnabled(true);
        webRTCClient.initDataChannel(streamId);

        assertEquals(dc, peerInfo.dataChannel);


        doNothing().when(webRTCClient).reportError(anyString(), anyString());
        ByteBuffer bb = ByteBuffer.allocate(10);
        DataChannel.Buffer buffer = new DataChannel.Buffer(bb, false);

        when(dc.send(buffer)).thenReturn(true);
        webRTCClient.sendMessageViaDataChannel(streamId, buffer);
        verify(dcObserver, timeout(1000).times(1)).onMessageSent(buffer, true);

        when(dc.send(buffer)).thenReturn(false);
        webRTCClient.sendMessageViaDataChannel(streamId, buffer);
        verify(dcObserver, timeout(1000).times(1)).onMessageSent(buffer, false);

        when(dc.send(buffer)).thenThrow(new NullPointerException());
        webRTCClient.sendMessageViaDataChannel(streamId, buffer);
        verify(dcObserver, timeout(1000).times(2)).onMessageSent(buffer, false);

        webRTCClient.setDataChannelEnabled(false);
        webRTCClient.sendMessageViaDataChannel(streamId, buffer);
        verify(dcObserver, timeout(1000).times(2)).onMessageSent(buffer, false);
    }

    @Test
    public void testCreatePeerConnection() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";

        doNothing().when(webRTCClient).createMediaConstraintsInternal();
        doNothing().when(webRTCClient).createPeerConnectionInternal(streamId);
        doNothing().when(webRTCClient).maybeCreateAndStartRtcEventLog(streamId);
        doNothing().when(webRTCClient).reportError(anyString(), anyString());

        webRTCClient.createPeerConnection(streamId);

        verify(webRTCClient, never()).reportError(eq(streamId), anyString());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        doThrow(new NullPointerException()).when(webRTCClient).createMediaConstraintsInternal();
        webRTCClient.createPeerConnection(streamId);
        verify(webRTCClient, timeout(10000)).reportError(eq(streamId), anyString());

    }

    @Test
    public void testCreateAudioDevice() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        webRTCClient.setStreamId(streamId);
        JavaAudioDeviceModule.Builder builder = mock(JavaAudioDeviceModule.Builder.class);
        doReturn(builder).when(webRTCClient).getADMBuilder();
        doNothing().when(webRTCClient).reportError(anyString(), anyString());

        doReturn(builder).when(builder).setCustomAudioFeed(anyBoolean());
        doReturn(builder).when(builder).setUseHardwareAcousticEchoCanceler(anyBoolean());
        doReturn(builder).when(builder).setUseHardwareNoiseSuppressor(anyBoolean());
        doReturn(builder).when(builder).setAudioRecordErrorCallback(any());
        doReturn(builder).when(builder).setAudioTrackErrorCallback(any());
        doReturn(builder).when(builder).setAudioRecordStateCallback(any());
        doReturn(builder).when(builder).setAudioTrackStateCallback(any());
        AudioDeviceModule adm = mock(AudioDeviceModule.class);
        doReturn(adm).when(builder).createAudioDeviceModule();


        ArgumentCaptor<JavaAudioDeviceModule.AudioRecordErrorCallback> audioRecordErrorCallbackCaptor
                = ArgumentCaptor.forClass(JavaAudioDeviceModule.AudioRecordErrorCallback.class);
        ArgumentCaptor<JavaAudioDeviceModule.AudioTrackErrorCallback> audioTrackErrorCallbackCaptor
                = ArgumentCaptor.forClass(JavaAudioDeviceModule.AudioTrackErrorCallback.class);
        ArgumentCaptor<JavaAudioDeviceModule.AudioRecordStateCallback> audioRecordStateCallbackCaptor =
                ArgumentCaptor.forClass(JavaAudioDeviceModule.AudioRecordStateCallback.class);
        ArgumentCaptor<JavaAudioDeviceModule.AudioTrackStateCallback> audioTrackStateCallbackCaptor =
                ArgumentCaptor.forClass(JavaAudioDeviceModule.AudioTrackStateCallback.class);

        webRTCClient.createJavaAudioDevice();

        verify(builder).setAudioRecordErrorCallback(audioRecordErrorCallbackCaptor.capture());
        verify(builder).setAudioTrackErrorCallback(audioTrackErrorCallbackCaptor.capture());
        verify(builder).setAudioRecordStateCallback(audioRecordStateCallbackCaptor.capture());
        verify(builder).setAudioTrackStateCallback(audioTrackStateCallbackCaptor.capture());

        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = audioRecordErrorCallbackCaptor.getValue();
        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = audioTrackErrorCallbackCaptor.getValue();
        JavaAudioDeviceModule.AudioRecordStateCallback audioRecordStateCallback = audioRecordStateCallbackCaptor.getValue();
        JavaAudioDeviceModule.AudioTrackStateCallback audioTrackStateCallback = audioTrackStateCallbackCaptor.getValue();

        audioRecordStateCallback.onWebRtcAudioRecordStop();
        audioRecordStateCallback.onWebRtcAudioRecordStop();

        audioTrackStateCallback.onWebRtcAudioTrackStart();
        audioTrackStateCallback.onWebRtcAudioTrackStop();

        String msg = "test error";
        audioRecordErrorCallback.onWebRtcAudioRecordError(msg);
        verify(webRTCClient, times(1)).reportError(eq(streamId), eq(msg));

        audioRecordErrorCallback.onWebRtcAudioRecordInitError(msg);
        verify(webRTCClient, times(2)).reportError(eq(streamId), eq(msg));

        audioRecordErrorCallback.onWebRtcAudioRecordStartError(JavaAudioDeviceModule.AudioRecordStartErrorCode.AUDIO_RECORD_START_EXCEPTION, msg);
        verify(webRTCClient, times(3)).reportError(eq(streamId), eq(msg));

        audioTrackErrorCallback.onWebRtcAudioTrackError(msg);
        verify(webRTCClient, times(4)).reportError(eq(streamId), eq(msg));

        audioTrackErrorCallback.onWebRtcAudioTrackInitError(msg);
        verify(webRTCClient, times(5)).reportError(eq(streamId), eq(msg));

        audioTrackErrorCallback.onWebRtcAudioTrackStartError(JavaAudioDeviceModule.AudioTrackStartErrorCode.AUDIO_TRACK_START_EXCEPTION, msg);
        verify(webRTCClient, times(6)).reportError(eq(streamId), eq(msg));
    }

    @Test
    public void testCreatePeerConnectionInternal() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        webRTCClient.setStreamId(streamId);

        doNothing().when(webRTCClient).setWebRTCLogLevel();
        doReturn(mock(AudioTrack.class)).when(webRTCClient).createAudioTrack();

        //no factory so nothing happens, return immediately
        webRTCClient.createPeerConnectionInternal(streamId);

        PeerConnectionFactory factory = mock(PeerConnectionFactory.class);
        webRTCClient.setFactory(factory);

        PeerConnection pc = mock(PeerConnection.class);
        when(factory.createPeerConnection(any(PeerConnection.RTCConfiguration.class), any(PeerConnection.Observer.class)))
                .thenReturn(pc);

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, IWebRTCClient.MODE_PUBLISH);
        webRTCClient.peers.put(streamId, peerInfo);

        webRTCClient.createPeerConnectionInternal(streamId);
        verify(factory).createPeerConnection(any(PeerConnection.RTCConfiguration.class), any(PeerConnection.Observer.class));
    }

    @Test
    public void testStatsTest() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";

        doNothing().when(webRTCClient).onPeerConnectionStatsReady(any());

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, IWebRTCClient.MODE_PUBLISH);
        webRTCClient.peers.put(streamId, peerInfo);

        //no pc so nothing happens, return immediately
        webRTCClient.getStats(streamId);

        PeerConnection pc = mock(PeerConnection.class);
        peerInfo.peerConnection = pc;
        webRTCClient.getStats(streamId);
        verify(pc, timeout(1000).times(1)).getStats(any());


    }

    @Test
    public void testCreateSDP() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        doNothing().when(webRTCClient).initDataChannel(streamId);

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, IWebRTCClient.MODE_PUBLISH);
        webRTCClient.peers.put(streamId, peerInfo);

        PeerConnection pc = mock(PeerConnection.class);
        peerInfo.peerConnection = pc;

        webRTCClient.createOffer(streamId);
        verify(pc, timeout(1000).times(1)).createOffer(any(), any());

        webRTCClient.createAnswer(streamId);
        verify(pc, timeout(1000).times(1)).createAnswer(any(), any());


    }

    @Test
    public void testAddRemoveRemoteIceCandidate() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        String streamId = "stream1";
        doNothing().when(webRTCClient).initDataChannel(streamId);

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, IWebRTCClient.MODE_PUBLISH);
        webRTCClient.peers.put(streamId, peerInfo);

        PeerConnection pc = mock(PeerConnection.class);
        peerInfo.peerConnection = pc;

        IceCandidate iceCandidate = mock(IceCandidate.class);

        webRTCClient.setQueuedRemoteCandidates(null);
        webRTCClient.addRemoteIceCandidate(streamId, iceCandidate);
        verify(pc, timeout(1000).times(1)).addIceCandidate(any(), any());

        List<IceCandidate> iceCandidatesQ = new ArrayList<>();
        webRTCClient.setQueuedRemoteCandidates(iceCandidatesQ);
        webRTCClient.addRemoteIceCandidate(streamId, iceCandidate);
        Awaitility.await().until(() -> iceCandidatesQ.size() == 1);
        assertEquals(iceCandidate, iceCandidatesQ.get(0));

        IceCandidate[] iceCandidatesTorRemove = new IceCandidate[1];
        iceCandidatesTorRemove[0] = iceCandidate;
        webRTCClient.removeRemoteIceCandidates(streamId, iceCandidatesTorRemove);

        verify(pc, timeout(1000).times(1)).removeIceCandidates(any());
    }

    @Test
    public void testDegradationPreference() {
        String streamId = "stream1";

        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        RtpParameters.DegradationPreference degradationPreference = RtpParameters.DegradationPreference.BALANCED;
        webRTCClient.setDegradationPreference(streamId, degradationPreference);
        //will return imediately

        WebRTCClient.PeerInfo peerInfo = new WebRTCClient.PeerInfo(streamId, IWebRTCClient.MODE_PUBLISH);
        webRTCClient.peers.put(streamId, peerInfo);

        PeerConnection pc = mock(PeerConnection.class);
        peerInfo.peerConnection = pc;

        List<RtpSender> senders = new ArrayList<>();
        RtpSender sender = mock(RtpSender.class);
        senders.add(sender);
        when(pc.getSenders()).thenReturn(senders);

        MediaStreamTrack track = mock(MediaStreamTrack.class);
        when(sender.track()).thenReturn(track);

        when(track.kind()).thenReturn(WebRTCClient.VIDEO_TRACK_TYPE);

        RtpParameters parameters = mock(RtpParameters.class);
        when(sender.getParameters()).thenReturn(parameters);

        webRTCClient.setDegradationPreference(streamId, degradationPreference);

        verify(sender, timeout(1000).times(1)).setParameters(parameters);
    }

    @Test
    public void testCloseInternal() {
        String streamId = "stream1";

        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        WebRTCClient webRTCClientReal = new WebRTCClient(listener, mock(Context.class));
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        webRTCClientReal.setWsHandler(wsHandler);

        WebRTCClient webRTCClient = spy(webRTCClientReal);
        final Handler handler = getMockHandler();
        webRTCClient.setHandler(handler);

        webRTCClient.closeInternal();
    }
}