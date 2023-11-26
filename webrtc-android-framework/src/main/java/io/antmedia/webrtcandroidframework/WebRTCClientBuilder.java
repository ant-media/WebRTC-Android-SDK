package io.antmedia.webrtcandroidframework;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

public class WebRTCClientBuilder {

    private WebRTCClientConfig webRTCClientConfig;

    WebRTCClientBuilder() {
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

    public WebRTCClientBuilder setFullScreenRenderer(SurfaceViewRenderer fullScreenRenderer) {
        webRTCClientConfig.fullScreenRenderer = fullScreenRenderer;
        return this;
    }

    public WebRTCClientBuilder setPipRenderer(SurfaceViewRenderer pipRenderer) {
        webRTCClientConfig.pipRenderer = pipRenderer;
        return this;
    }

    public WebRTCClientBuilder setWebRTCListener(IWebRTCListener webRTCListener) {
        webRTCClientConfig.webRTCListener = webRTCListener;
        return this;
    }

    public WebRTCClientBuilder setContext(Context context) {
        webRTCClientConfig.context = context;
        return this;
    }
}
