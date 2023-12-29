package io.antmedia.webrtcandroidframework.api;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;

public class DefaultConferenceWebRTCListenerTest extends DefaultWebRTCListenerTest{
    private String roomId;
    private String streamId;

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


        defaultWebRTCListener.onNewVideoTrack(mockVideoTrack);

        verify(mockSurfaceViewRenderer, times(1)).setTag(mockVideoTrack);
        verify(mockWebRTCClient, times(1)).setRendererForVideoTrack(mockSurfaceViewRenderer, mockVideoTrack);
    }

    @Test
    public void testOnVideoTrackEnded() {
        VideoTrack mockVideoTrack = mock(VideoTrack.class);
        SurfaceViewRenderer mockSurfaceViewRenderer = mock(SurfaceViewRenderer.class);

        when(mockSurfaceViewRenderer.getTag()).thenReturn(mockVideoTrack);

        mockWebRTCClient.getConfig().remoteVideoRenderers.add(mockSurfaceViewRenderer);

        defaultWebRTCListener.onVideoTrackEnded(mockVideoTrack);

        verify(mockSurfaceViewRenderer, times(1)).setTag(null);
    }

    @Test
    public void testOnReconnectionAttempt() {
        defaultWebRTCListener.onReconnectionAttempt("streamId");
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
    }

    @Test
    public void testOnJoinedTheRoom() {
        String[] tracks = new String[]{"stream1", "stream2"};
        defaultWebRTCListener.onJoinedTheRoom(streamId, tracks);
        verify(defaultWebRTCListener, times(2)).callbackCalled(anyString());

        verify(mockWebRTCClient, times(1)).publish(eq(streamId), anyString(),
                anyBoolean(), anyBoolean(), anyString(), anyString(), anyString(), eq(roomId));

        verify(defaultWebRTCListener, times(1)).onTrackList(tracks);

    }

    @Test
    public void testOnRoomInformation() {
        String[] tracks = new String[]{"stream1", "stream2"};
        defaultWebRTCListener.onRoomInformation(tracks);
        verify(defaultWebRTCListener, times(1)).callbackCalled(anyString());
        verify(mockWebRTCClient, times(1)).play(eq(roomId), eq(tracks));
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
}
