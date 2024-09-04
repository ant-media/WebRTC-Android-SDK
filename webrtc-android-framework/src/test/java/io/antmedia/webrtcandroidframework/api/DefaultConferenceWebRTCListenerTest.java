package io.antmedia.webrtcandroidframework.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;

public class DefaultConferenceWebRTCListenerTest{
    private String roomId;
    private String streamId;
    private DefaultConferenceWebRTCListener defaultWebRTCListener;

    @Mock
    IWebRTCClient mockWebRTCClient;

    @Before
    public void setUp() {
        roomId = "roomId" + RandomStringUtils.randomNumeric(3);
        streamId = "streamId" + RandomStringUtils.randomNumeric(3);
        MockitoAnnotations.initMocks(this);
        defaultWebRTCListener = spy(new DefaultConferenceWebRTCListener(roomId, streamId));
        defaultWebRTCListener.setWebRTCClient(mockWebRTCClient);
        WebRTCClientConfig config = new WebRTCClientConfig();
        when(mockWebRTCClient.getConfig()).thenReturn(config);
    }

    @Test
    public void testOnDisconnected() {
        defaultWebRTCListener.onDisconnected();
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnPublishFinished() {
        defaultWebRTCListener.onPublishFinished("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnPlayFinished() {
        defaultWebRTCListener.onPlayFinished("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnPublishStarted() {
        defaultWebRTCListener.onPublishStarted("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnPlayStarted() {
        defaultWebRTCListener.onPlayStarted("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testNoStreamExistsToPlay() {
        defaultWebRTCListener.noStreamExistsToPlay("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnError() {
        defaultWebRTCListener.onError("description", "streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnSignalChannelClosed() {
        defaultWebRTCListener.onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification.NORMAL, "streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testStreamIdInUse() {
        defaultWebRTCListener.streamIdInUse("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnIceConnected() {
        defaultWebRTCListener.onIceConnected("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnIceDisconnected() {
        defaultWebRTCListener.onIceDisconnected("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnTrackList() {
        defaultWebRTCListener.onTrackList(new String[]{"track1", "track2"});
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnBitrateMeasurement() {
        defaultWebRTCListener.onBitrateMeasurement("streamId", 100, 50, 50);
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnStreamInfoList() {
        defaultWebRTCListener.onStreamInfoList("streamId", new ArrayList<>());
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnNewVideoTrack() {
        VideoTrack mockVideoTrack = mock(VideoTrack.class);
        SurfaceViewRenderer mockSurfaceViewRenderer = mock(SurfaceViewRenderer.class);

        when(mockSurfaceViewRenderer.getTag()).thenReturn(null);

        mockWebRTCClient.getConfig().remoteVideoRenderers.add(mockSurfaceViewRenderer);


        defaultWebRTCListener.onNewVideoTrack(mockVideoTrack,"stream1");

        verify(mockSurfaceViewRenderer, times(1)).setTag(mockVideoTrack);
        verify(mockWebRTCClient, times(1)).setRendererForVideoTrack(mockSurfaceViewRenderer, mockVideoTrack);
    }

    @Test
    public void testOnVideoTrackEnded() {
        VideoTrack mockVideoTrack = mock(VideoTrack.class);
        when(mockVideoTrack.id()).thenReturn("stream1");
        SurfaceViewRenderer mockSurfaceViewRenderer = mock(SurfaceViewRenderer.class);

        when(mockSurfaceViewRenderer.getTag()).thenReturn(mockVideoTrack);

        mockWebRTCClient.getConfig().remoteVideoRenderers.add(mockSurfaceViewRenderer);

        defaultWebRTCListener.onVideoTrackEnded(mockVideoTrack);

        verify(mockWebRTCClient, times(1)).releaseRenderer(mockSurfaceViewRenderer);
    }

    @Test
    public void testOnReconnectionAttempt() {
        defaultWebRTCListener.onReconnectionAttempt("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnLeftTheRoom() {
        defaultWebRTCListener.onLeftTheRoom("roomId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnMutedFor() {
        defaultWebRTCListener.onMutedFor("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnUnmutedFor() {
        defaultWebRTCListener.onUnmutedFor("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnCameraTurnOnFor() {
        defaultWebRTCListener.onCameraTurnOnFor("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnCameraTurnOffFor() {
        defaultWebRTCListener.onCameraTurnOffFor("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnSatatusUpdateFor() {
        defaultWebRTCListener.onSatatusUpdateFor("streamId", true, false);
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testReconnecting() {
        defaultWebRTCListener.onReconnectionAttempt(roomId);
        assertFalse(defaultWebRTCListener.isPublishReconnectingForTest());

        defaultWebRTCListener.onReconnectionAttempt(streamId);
        assertTrue(defaultWebRTCListener.isPublishReconnectingForTest());

        defaultWebRTCListener.onPublishStarted(streamId);
        assertFalse(defaultWebRTCListener.isPublishReconnectingForTest());

        defaultWebRTCListener.onReconnectionAttempt(streamId);
        assertTrue(defaultWebRTCListener.isPublishReconnectingForTest());

        defaultWebRTCListener.onSessionRestored(streamId);
        assertFalse(defaultWebRTCListener.isPublishReconnectingForTest());

    }

    @Test
    public void testStartAfterPublishStarted() {
        //after publish started, play should be called
        defaultWebRTCListener.onPublishStarted(streamId);
        assertFalse(defaultWebRTCListener.isPublishReconnectingForTest());
        verify(mockWebRTCClient, times(1)).play(roomId);

        //playStarted false, but play should not be called because publish is reconnecting state
        defaultWebRTCListener.onReconnectionAttempt(streamId);
        assertTrue(defaultWebRTCListener.isPublishReconnectingForTest());
        defaultWebRTCListener.onPublishStarted(streamId);
        verify(mockWebRTCClient, times(2)).play(roomId);

        //playStarted will be true, so play should not be called
        defaultWebRTCListener.onPlayStarted(roomId);
        defaultWebRTCListener.onPublishStarted(streamId);
        verify(mockWebRTCClient, times(3)).play(roomId);
    }
    @Test
    public void testOnPeerConnectionCreated() {
        defaultWebRTCListener.onPeerConnectionCreated("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }
}
