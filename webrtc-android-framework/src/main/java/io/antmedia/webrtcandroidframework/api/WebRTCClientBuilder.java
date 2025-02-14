package io.antmedia.webrtcandroidframework.api;

import android.app.Activity;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.Arrays;

import io.antmedia.webrtcandroidframework.core.WebRTCClient;

public class WebRTCClientBuilder {

    private WebRTCClientConfig webRTCClientConfig;

    public WebRTCClientBuilder() {
        webRTCClientConfig = new WebRTCClientConfig();
    }

    public WebRTCClient build() {
        return new WebRTCClient(webRTCClientConfig);
    }

    public WebRTCClientBuilder setServerUrl(String serverUrl) {
        webRTCClientConfig.serverUrl = serverUrl;
        return this;
    }

    public WebRTCClientBuilder setStreamId(String streamId) {
        webRTCClientConfig.streamId = streamId;
        return this;
    }

    public WebRTCClientBuilder setToken(String token) {
        webRTCClientConfig.token = token;
        return this;
    }

    public WebRTCClientBuilder setVideoCallEnabled(boolean videoCallEnabled) {
        webRTCClientConfig.videoCallEnabled = videoCallEnabled;
        return this;
    }

    public WebRTCClientBuilder setAudioCallEnabled(boolean audioCallEnabled) {
        webRTCClientConfig.audioCallEnabled = audioCallEnabled;
        return this;
    }

    public WebRTCClientBuilder setDataChannelEnabled(boolean dataChannelEnabled) {
        webRTCClientConfig.dataChannelEnabled = dataChannelEnabled;
        return this;
    }

    public WebRTCClientBuilder setVideoWidth(int videoWidth) {
        webRTCClientConfig.videoWidth = videoWidth;
        return this;
    }

    public WebRTCClientBuilder setVideoHeight(int videoHeight) {
        webRTCClientConfig.videoHeight = videoHeight;
        return this;
    }

    public WebRTCClientBuilder setVideoFps(int videoFps) {
        webRTCClientConfig.videoFps = videoFps;
        return this;
    }

    public WebRTCClientBuilder setVideoStartBitrate(int videoStartBitrate) {
        webRTCClientConfig.videoStartBitrate = videoStartBitrate;
        return this;
    }

    public WebRTCClientBuilder setVideoCodec(String videoCodec) {
        webRTCClientConfig.videoCodec = videoCodec;
        return this;
    }

    public WebRTCClientBuilder setAudioStartBitrate(int audioStartBitrate) {
        webRTCClientConfig.audioStartBitrate = audioStartBitrate;
        return this;
    }

    public WebRTCClientBuilder setAudioCodec(String audioCodec) {
        webRTCClientConfig.audioCodec = audioCodec;
        return this;
    }

    public WebRTCClientBuilder setLocalVideoRenderer(SurfaceViewRenderer localVideoRenderer) {
        webRTCClientConfig.localVideoRenderer = localVideoRenderer;
        return this;
    }

    public WebRTCClientBuilder addRemoteVideoRenderer (SurfaceViewRenderer ... remoteVideoRenderers) {
        webRTCClientConfig.remoteVideoRenderers.addAll(Arrays.asList(remoteVideoRenderers));
        return this;
    }

    public WebRTCClientBuilder setWebRTCListener(IWebRTCListener webRTCListener) {
        webRTCClientConfig.webRTCListener = webRTCListener;
        return this;
    }

    public WebRTCClientBuilder setDataChannelObserver(IDataChannelObserver dataChannelObserver) {
        webRTCClientConfig.dataChannelObserver = dataChannelObserver;
        return this;
    }

    public WebRTCClientBuilder setActivity(Activity activity) {
        webRTCClientConfig.activity = activity;
        return this;
    }

    public WebRTCClientBuilder setInitiateBeforeStream(boolean b) {
        webRTCClientConfig.initiateBeforeStream = b;
        return this;
    }

    public WebRTCClientBuilder setCustomAudioFeed(boolean b) {
        webRTCClientConfig.customAudioFeed = b;
        return this;
    }

    public WebRTCClientBuilder setScalingType(RendererCommon.ScalingType scaleAspectFit) {
        webRTCClientConfig.scalingType = scaleAspectFit;
        return this;
    }

    public WebRTCClientBuilder setStunServerUri(String stunServerUri) {
        webRTCClientConfig.stunServerUri = stunServerUri;
        return this;
    }

    public WebRTCClientBuilder setTurnServer(String turnServerUri, String turnServerUserName, String turnServerPassword){
        webRTCClientConfig.turnServerUri = turnServerUri;
        webRTCClientConfig.turnServerUserName = turnServerUserName;
        webRTCClientConfig.turnServerPassword = turnServerPassword;
        return this;
    }

    public WebRTCClientBuilder setReconnectionEnabled(boolean b) {
        webRTCClientConfig.reconnectionEnabled = b;
        return this;
    }

    public WebRTCClientConfig getConfig() {
        return webRTCClientConfig;
    }

    public WebRTCClientBuilder setVideoSource(IWebRTCClient.StreamSource rearCamera) {
        webRTCClientConfig.videoSource = rearCamera;
        return this;
    }

    public WebRTCClientBuilder setBluetoothEnabled(boolean bluetoothEnabled) {
        webRTCClientConfig.bluetoothEnabled = bluetoothEnabled;
        return this;
    }
}
