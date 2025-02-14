package io.antmedia.webrtcandroidframework.api;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;


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
     * Data channel observer for the data channel events
     */
    public IDataChannelObserver dataChannelObserver = new DefaultDataChannelObserver();

    /*
     * Intent for screen capture got by calling activity
     */
    public Intent mediaProjectionIntent;

    /*
     * Media projection for screen capture
     */
    public MediaProjection mediaProjection;

    /*
     * Current video source to publish
     */
    public IWebRTCClient.StreamSource videoSource = IWebRTCClient.StreamSource.FRONT_CAMERA;


    /*
     * Flag indicating whether initate WebRTCClient (renderers, websocket, capturers etc.)  before stream publish starts
     */
    public boolean initiateBeforeStream;

    /*
     * Flag indicating whether custom audio feed is enabled
     */
    public boolean customAudioFeed;

    /*
     * Scaling type for video rendering
     */
    public RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;

    /*
     * Stun server uri
     */
    public String stunServerUri = "stun:stun1.l.google.com:19302";

    /*
     * TURN server uri
     */
    public String turnServerUri;

    /*
     * TURN server user name
     */
    public String turnServerUserName;

    /*
     * TURN server password
     */
    public String turnServerPassword;

    /*
     * Flag indicating whether reconnection is enabled
     */
    public boolean reconnectionEnabled = true;

    /*
     * Flag indicating whether AGC (automatic gain contol) and HPF (high pass filter) is disabled in audio processing
     */
    public boolean disableWebRtcAGCAndHPF = false;

    /*
     * Flag for connecting bluetooth headphones.
     */
    public boolean bluetoothEnabled = false;
}
