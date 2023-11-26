package io.antmedia.webrtcandroidframework;

import android.content.Context;

import org.webrtc.SurfaceViewRenderer;

public class WebRTCClientConfig {

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
    public boolean videoCallEnabled;

    /*
     * Flag indicating whether audio call is enabled
     */
    public boolean audioCallEnabled;

    /*
     * Flag indicating whether data channel is enabled
     */
    public boolean dataChannelEnabled;

    /*
     * Width of the video in pixels
     */
    public int videoWidth;

    /*
     * Height of the video in pixels
     */
    public int videoHeight;

    /*
     * Frames per second for the video
     */
    public int videoFps;

    /*
     * Initial bitrate for video transmission
     */
    public int videoStartBitrate;

    /*
     * Codec used for video encoding and decoding
     */
    public String videoCodec;

    /*
     * Flag for hardware codec acceleration
     */
    public boolean hwCodecAcceleration;

    /*
     * Flag indicating whether flexible forward error correction (FlexFEC) is enabled for video
     */
    public boolean videoFlexfecEnabled;

    /*
     * Initial bitrate for audio transmission
     */
    public int audioStartBitrate;

    /*
     * Codec used for audio encoding and decoding
     */
    public String audioCodec;

    /*
     * Flag indicating whether audio processing is disabled
     */
    public boolean noAudioProcessing;

    /*
     * WebRTC listener for callbacks
     */
    public IWebRTCListener webRTCListener;

    /*
     * Context for WebRTCClient
     */
    public Context context;


    /*
     * Fullscreen video renderer
     */
    public SurfaceViewRenderer fullScreenRenderer;

    /*
     * PIP video renderer
     */
    public SurfaceViewRenderer pipRenderer;
}
