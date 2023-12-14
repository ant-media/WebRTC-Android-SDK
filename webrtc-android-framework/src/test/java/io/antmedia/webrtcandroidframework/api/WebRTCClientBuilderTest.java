package io.antmedia.webrtcandroidframework.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.RendererCommon;
import android.app.Activity;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.antmedia.webrtcandroidframework.core.WebRTCClient;

public class WebRTCClientBuilderTest {
    private WebRTCClientBuilder webRTCClientBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webRTCClientBuilder = new WebRTCClientBuilder();
    }

    @Test
    public void testBuild() {
        Activity activity = mock(Activity.class);
        when(activity.getMainLooper()).thenReturn(mock(android.os.Looper.class));
        webRTCClientBuilder.setActivity(activity);
        WebRTCClient webRTCClient = webRTCClientBuilder.build();
        assertNotNull(webRTCClient);
        assertEquals(webRTCClientBuilder.getConfig(), webRTCClient.getConfig());
    }

    @Test
    public void testSetServerUrl() {
        String serverUrl = "https://example.com";
        webRTCClientBuilder.setServerUrl(serverUrl);
        assertEquals(serverUrl, webRTCClientBuilder.getConfig().serverUrl);
    }

    @Test
    public void testSetStreamId() {
        String streamId = "stream123";
        webRTCClientBuilder.setStreamId(streamId);
        assertEquals(streamId, webRTCClientBuilder.getConfig().streamId);
    }

    // Repeat the above pattern for each setter method in WebRTCClientBuilder

    @Test
    public void testAddRemoteVideoRenderer() {
        SurfaceViewRenderer mockSurfaceViewRenderer = mock(SurfaceViewRenderer.class);
        webRTCClientBuilder.addRemoteVideoRenderer(mockSurfaceViewRenderer);
        assertTrue(webRTCClientBuilder.getConfig().remoteVideoRenderers.contains(mockSurfaceViewRenderer));
    }

    @Test
    public void testSetWebRTCListener() {
        IWebRTCListener mockWebRTCListener = mock(IWebRTCListener.class);
        webRTCClientBuilder.setWebRTCListener(mockWebRTCListener);
        assertEquals(mockWebRTCListener, webRTCClientBuilder.getConfig().webRTCListener);
    }

    @Test
    public void testSetDataChannelObserver() {
        IDataChannelObserver mockDataChannelObserver = mock(IDataChannelObserver.class);
        webRTCClientBuilder.setDataChannelObserver(mockDataChannelObserver);
        assertEquals(mockDataChannelObserver, webRTCClientBuilder.getConfig().dataChannelObserver);
    }

    @Test
    public void testSetActivity() {
        Activity mockActivity = mock(Activity.class);
        webRTCClientBuilder.setActivity(mockActivity);
        assertEquals(mockActivity, webRTCClientBuilder.getConfig().activity);
    }

    @Test
    public void testSetInitiateBeforeStream() {
        boolean initiateBeforeStream = true;
        webRTCClientBuilder.setInitiateBeforeStream(initiateBeforeStream);
        assertEquals(initiateBeforeStream, webRTCClientBuilder.getConfig().initiateBeforeStream);
    }

    @Test
    public void testSetCustomAudioFeed() {
        boolean customAudioFeed = true;
        webRTCClientBuilder.setCustomAudioFeed(customAudioFeed);
        assertEquals(customAudioFeed, webRTCClientBuilder.getConfig().customAudioFeed);
    }

    @Test
    public void testSetScalingType() {
        RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;
        webRTCClientBuilder.setScalingType(scalingType);
        assertEquals(scalingType, webRTCClientBuilder.getConfig().scalingType);
    }

    @Test
    public void testSetStunServerUri() {
        String stunServerUri = "stun:stun.example.com";
        webRTCClientBuilder.setStunServerUri(stunServerUri);
        assertEquals(stunServerUri, webRTCClientBuilder.getConfig().stunServerUri);
    }

    @Test
    public void testSetReconnectionEnabled() {
        boolean reconnectionEnabled = true;
        webRTCClientBuilder.setReconnectionEnabled(reconnectionEnabled);
        assertEquals(reconnectionEnabled, webRTCClientBuilder.getConfig().reconnectionEnabled);
    }

    @Test
    public void testSetToken() {
        String token = "testToken";
        webRTCClientBuilder.setToken(token);
        assertEquals(token, webRTCClientBuilder.getConfig().token);
    }

    @Test
    public void testSetVideoCallEnabled() {
        boolean videoCallEnabled = true;
        webRTCClientBuilder.setVideoCallEnabled(videoCallEnabled);
        assertEquals(videoCallEnabled, webRTCClientBuilder.getConfig().videoCallEnabled);
    }

    @Test
    public void testSetAudioCallEnabled() {
        boolean audioCallEnabled = true;
        webRTCClientBuilder.setAudioCallEnabled(audioCallEnabled);
        assertEquals(audioCallEnabled, webRTCClientBuilder.getConfig().audioCallEnabled);
    }

    @Test
    public void testSetDataChannelEnabled() {
        boolean dataChannelEnabled = true;
        webRTCClientBuilder.setDataChannelEnabled(dataChannelEnabled);
        assertEquals(dataChannelEnabled, webRTCClientBuilder.getConfig().dataChannelEnabled);
    }

    @Test
    public void testSetVideoWidth() {
        int videoWidth = 640;
        webRTCClientBuilder.setVideoWidth(videoWidth);
        assertEquals(videoWidth, webRTCClientBuilder.getConfig().videoWidth);
    }

    @Test
    public void testSetVideoHeight() {
        int videoHeight = 480;
        webRTCClientBuilder.setVideoHeight(videoHeight);
        assertEquals(videoHeight, webRTCClientBuilder.getConfig().videoHeight);
    }

    @Test
    public void testSetVideoFps() {
        int videoFps = 30;
        webRTCClientBuilder.setVideoFps(videoFps);
        assertEquals(videoFps, webRTCClientBuilder.getConfig().videoFps);
    }

    @Test
    public void testSetVideoStartBitrate() {
        int videoStartBitrate = 500;
        webRTCClientBuilder.setVideoStartBitrate(videoStartBitrate);
        assertEquals(videoStartBitrate, webRTCClientBuilder.getConfig().videoStartBitrate);
    }

    @Test
    public void testSetVideoCodec() {
        String videoCodec = "H.264";
        webRTCClientBuilder.setVideoCodec(videoCodec);
        assertEquals(videoCodec, webRTCClientBuilder.getConfig().videoCodec);
    }

    @Test
    public void testSetAudioStartBitrate() {
        int audioStartBitrate = 64;
        webRTCClientBuilder.setAudioStartBitrate(audioStartBitrate);
        assertEquals(audioStartBitrate, webRTCClientBuilder.getConfig().audioStartBitrate);
    }

    @Test
    public void testSetAudioCodec() {
        String audioCodec = "Opus";
        webRTCClientBuilder.setAudioCodec(audioCodec);
        assertEquals(audioCodec, webRTCClientBuilder.getConfig().audioCodec);
    }

    @Test
    public void testSetLocalVideoRenderer() {
        SurfaceViewRenderer mockSurfaceViewRenderer = mock(SurfaceViewRenderer.class);
        webRTCClientBuilder.setLocalVideoRenderer(mockSurfaceViewRenderer);
        assertEquals(mockSurfaceViewRenderer, webRTCClientBuilder.getConfig().localVideoRenderer);
    }
}
