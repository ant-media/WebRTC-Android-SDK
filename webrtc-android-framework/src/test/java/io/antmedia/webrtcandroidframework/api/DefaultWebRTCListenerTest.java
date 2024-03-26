package io.antmedia.webrtcandroidframework.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;
import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class DefaultWebRTCListenerTest {

    @Mock
    IWebRTCClient mockWebRTCClient;

    DefaultWebRTCListener defaultWebRTCListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        defaultWebRTCListener = spy(new DefaultWebRTCListener());
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


        defaultWebRTCListener.onNewVideoTrack(mockVideoTrack, "stream1");

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
    public void testOnJoinedTheRoom() {
        defaultWebRTCListener.onJoinedTheRoom("streamId", new String[]{"stream1", "stream2"});
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnRoomInformation() {
        defaultWebRTCListener.onRoomInformation(new String[]{"stream1", "stream2"});
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
    public void testOnBroadcastObject(){
        defaultWebRTCListener.onBroadcastObject((Broadcast) Mockito.anyObject());
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }
}
