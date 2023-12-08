package io.antmedia.webrtcandroidframework.api;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoTrack;
import org.webrtc.audio.CustomWebRtcAudioRecord;

/**
 * Created by karinca on 20.10.2017.
 */

public interface IWebRTCClient {
    enum StreamSource
    {
        SCREEN,
        FRONT_CAMERA,
        REAR_CAMERA,
        CUSTOM
    }


    /**
     * Switches the cameras
     */
    void switchCamera();


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
     * Get the error
     * @return error or null if not
     */
    String getError();

    /**
     * Return if data channel is enabled and open
     * @return true if data channel is available
     * false if it's not opened either by mobile or server side
     */
    boolean isDataChannelEnabled();

    /**
     * This is used to get stream info list
     */
    void getStreamInfoList(String streamId);

    /**
     * This is used to play the specified resolution
     * @param height
     */
    void forceStreamQuality(String streamId, int height);

    //FIXME: add comment
    void onCameraSwitch();
    void onCaptureFormatChange(int width, int height, int framerate);
    boolean onToggleMic();

    static WebRTCClientBuilder builder() {
        return new WebRTCClientBuilder();
    }

    /**
     * This is used to strart a WebRTC publish stream
     * @param streamId: any name
     */
    void publish(String streamId);


    /**
     * This is used to strart a WebRTC publish stream
     * @param streamId: any name
     * @param token: token for stream
     * TODO: add comment
     */
    void publish(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled,
                 String subscriberId, String subscriberCode, String streamName, String mainTrackId);


    /**
     * This is used to play a WebRTC stream
     * @param streamId
     */
    void play(String streamId);

    /**
     * This is used to play a multitrack WebRTC stream
     * @param streamId
     */
    void play(String streamId, String[] tracks);

    /**
     * This is used to play a WebRTC stream with all parameters
     * @param streamId
     */
    void play(String streamId, String token, String[] tracks,  String subscriberId, String subscriberCode, String viewerInfo);



    /**
     * This is used to get streaming status for a stream id
     * @param streamId
     * @return
     */
    boolean isStreaming(String streamId);

    /**
     * This is used to join a conference room
     * @param roomId
     * @param streamId
     * @return
     */
    void joinToConferenceRoom(String roomId, String streamId);

    /**
     * This is used to leave from a conference room
     * @param roomId
     */
    void leaveFromConference(String roomId);

    /**
     * This is used to send data via data channel
     * @param streamId
     * @param buffer
     */
    void sendMessageViaDataChannel(String streamId, DataChannel.Buffer buffer);

    /**
     * This is used to stop a stream
     * @param streamId
     */
    void stop(String streamId);

    /**
     * This is used to join a peer to peer stream
     * @param streamId
     */
    void join(String streamId);

    /**
     * This is used to get reconnecting status
     */
    boolean isReconnectionInProgress();

    /**
     * This is used to get room info
     * @param roomId
     * @param streamId
     */
    void getRoomInfo(String roomId, String streamId);

    /**
     * This is used to change video source on the fly
     * @param newSource
     */
    void changeVideoSource(StreamSource newSource);


    WebRTCClientConfig getConfig();

    VideoCapturer getVideoCapturer();

    CustomWebRtcAudioRecord getAudioInput();

    void setVideoEnabled(boolean b);

    void setAudioEnabled(boolean b);

    void enableTrack(String streamId, String selecetedTrack, boolean enabled);

    void getTrackList(String streamId, String token);

    void setRendererForVideoTrack(SurfaceViewRenderer renderer, VideoTrack videoTrack);

}
