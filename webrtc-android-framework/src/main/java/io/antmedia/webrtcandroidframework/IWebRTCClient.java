package io.antmedia.webrtcandroidframework;

import android.content.Intent;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtcandroidframework.apprtc.CallFragment;

/**
 * Created by karinca on 20.10.2017.
 */

public interface IWebRTCClient extends CallFragment.OnCallEvents {


    /**
     * Publish mode
     */
    String MODE_PUBLISH = "publish";

    /**
     * Play mode
     */
    String MODE_PLAY = "play";

    /**
     * Join mode
     */
    String MODE_JOIN = "join";


    /**
     * Camera open order
     * By default front camera is attempted to be opened at first,
     * if it is set to false, another camera that is not front will be tried to be open
     * @param openFrontCamera if it is true, front camera will tried to be opened
     *                        if it is false, another camera that is not front will be tried to be opened
     */
    void setOpenFrontCamera(boolean openFrontCamera);


    /**

     * If mode is MODE_PUBLISH, stream with streamId field will be published to the Server
     * if mode is MODE_PLAY, stream with streamId field will be played from the Server
     *
     * @param url websocket url to connect
     * @param streamId is the stream id in the server to process
     * @param mode one of the MODE_PUBLISH, MODE_PLAY, MODE_JOIN
     * @param token one time token string
     * @param intent the intent to be
     */
    void init(String url, String streamId, String mode, String token, Intent intent);


    /**
     * Starts the streaming according to mode
     */
    void startStream();

    /**
     * Stops the streaming
     */
    void stopStream();

    /**
     * Switches the cameras
     */
    void switchCamera();

    /**
     * Switches the video according to type and its aspect ratio
     * @param scalingType
     */
    void switchVideoScaling(RendererCommon.ScalingType scalingType);

    /**
     * toggle microphone
     * @return
     */
    boolean toggleMic();

    /**
     * Stops the video source
     */
    void stopVideoSource();

    /**
     * Starts or restarts the video source
     */
    void startVideoSource();

    /**
     * Swapeed the fullscreen renderer and pip renderer
     * @param b
     */
    void setSwappedFeeds(boolean b);

    /**
     * Set's the video renderers,
     * @param pipRenderer can be nullable
     * @param fullscreenRenderer cannot be nullable
     */
    void setVideoRenderers(SurfaceViewRenderer pipRenderer, SurfaceViewRenderer fullscreenRenderer);

    /**
     * Get the error
     * @return error or null if not
     */
    String getError();


    void setCameraOrientationFix(int orientation);

    void setMediaProjectionParams(int resultCode, Intent data);
}
