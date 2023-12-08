package io.antmedia.webrtcandroidframework.api;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;

import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class WebRTCClientConfig {

    /*
     * Renderer for local video
     */
    public SurfaceViewRenderer localVideoRenderer;

    /*
     * Renderers for remote video
     */
    public ArrayList<SurfaceViewRenderer> remoteVideoRenderers = new ArrayList<>();

    /*
     * websocket connection url to Ant Media Server
     * ex. wss://{AMS URL}:5443/{AppName}/websocket
     */
    public String serverUrl;

    /*
     * stream id for stream
     */
    public String streamId;

    /*
     * token for stream
     */
    public String token;

    /*
     * Flag indicating whether video call is enabled
     */
    public boolean videoCallEnabled = true;

    /*
     * Flag indicating whether audio call is enabled
     */
    public boolean audioCallEnabled = true;

    /*
     * Flag indicating whether data channel is enabled
     */
    public boolean dataChannelEnabled = true;

    /*
     * Width of the video in pixels
     */
    public int videoWidth = 720;

    /*
     * Height of the video in pixels
     */
    public int videoHeight = 1280;

    /*
     * Frames per second for the video
     */
    public int videoFps = 30;

    /*
     * Initial bitrate for video transmission
     */
    public int videoStartBitrate = 1700;

    /*
     * Codec used for video encoding and decoding, default VP8
     */
    public String videoCodec = "VP8";

    /*
     * Flag for hardware codec acceleration
     */
    public boolean hwCodecAcceleration = true;

    /*
     * Flag indicating whether flexible forward error correction (FlexFEC) is enabled for video
     */
    public boolean videoFlexfecEnabled = false;

    /*
     * Initial bitrate for audio transmission
     */
    public int audioStartBitrate = 32;

    /*
     * Codec used for audio encoding and decoding
     */
    public String audioCodec = "OPUS";

    /*
     * Flag indicating whether audio processing is disabled
     */
    public boolean noAudioProcessing = false;

    /*
     * WebRTC listener for callbacks
     */
    public IWebRTCListener webRTCListener = new DefaultWebRTCListener();

    /*
     * Running activity
     */
    public Activity activity;

    /*
     * Flag indicating whether screencapture is enabled
     */
    public boolean screencaptureEnabled = false;

    public IDataChannelObserver dataChannelObserver = new DefaultDataChannelObserver();


    public Intent mediaProjectionIntent;

    public MediaProjection mediaProjection;
    public IWebRTCClient.StreamSource videoSource;
    public boolean customVideoCapturerEnabled;
    public boolean initiateBeforeStream;
    public boolean customAudioFeed;
    public RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;

    public String stunServerUri = "stun:stun1.l.google.com:19302";
    public boolean reconnectionEnabled = true;
    public boolean disableWebRtcAGCAndHPF = false;
}
