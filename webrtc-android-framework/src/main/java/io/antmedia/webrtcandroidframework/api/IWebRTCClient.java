package io.antmedia.webrtcandroidframework.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.RtpParameters;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoTrack;
import org.webrtc.audio.CustomWebRtcAudioRecord;

import io.antmedia.webrtcandroidframework.core.StatsCollector;

/**
 * Created by karinca on 20.10.2017.
 */

public interface IWebRTCClient {


    /**
     * This enum is used to specify the stream source
     */
    enum StreamSource {
        SCREEN,
        FRONT_CAMERA,
        REAR_CAMERA,
        CUSTOM
    }

    /**
     * This method is used to initialize the WebRTCClient and configure it
     */
    static WebRTCClientBuilder builder() {
        return new WebRTCClientBuilder();
    }

    /**
     * This is used to strart a WebRTC publish stream
     *
     * @param streamId: any name
     */
    void publish(String streamId);


    /**
     * This is used to strart a WebRTC publish stream
     *
     * @param streamId:         id for the stream to publish
     * @param token:            token to authenticate
     * @param videoCallEnabled: true if it's a video call
     * @param audioCallEnabled: true if it's an audio call
     * @param subscriberId:     id of the subscriber
     * @param subscriberCode:   code of the subscriber
     * @param streamName:       name of the stream
     * @param mainTrackId:      id of the main track
     */
    void publish(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled,
                 String subscriberId, String subscriberCode, String streamName, String mainTrackId);


    /**
     * This is used to play a WebRTC stream
     *
     * @param streamId: id for the stream to play
     */
    void play(String streamId);

    /**
     * This is used to play a multitrack WebRTC stream
     *
     * @param streamId: id for the stream to play
     * @param tracks:   subtracks to play in multitrack stream
     */
    void play(String streamId, String[] tracks);

    /**
     * This is used to play a WebRTC stream with all parameters
     *
     * @param streamId:       id for the stream to play
     * @param token:          token to authenticate
     * @param tracks:         subtracks to play in multitrack stream
     * @param subscriberId:   id of the subscriber
     * @param subscriberCode: code of the subscriber
     * @param viewerInfo:     viewer info, any string is accepted
     */
    void play(String streamId, String token, String[] tracks, String subscriberId, String subscriberCode, String viewerInfo);

    /**
     * This is used to play a WebRTC stream with a parameter object
     *
     * @param params the play parameters
     */
    void play(PlayParams params);

    /**
     * This is used to join a peer to peer call
     *
     * @param streamId: id for the call
     */
    void join(String streamId);

    /**
     * This is used to join a conference room
     *
     * @param roomId:   id for the conference room
     * @param streamId: id for the participant
     */
    void joinToConferenceRoom(String roomId, String streamId);

    /**
     * This is used to join a conference room
     *
     * @param roomId:           id for the conference room
     * @param streamId:         id for the participant
     * @param videoCallEnabled: true if it's a video call
     * @param audioCallEnabled: true if it's an audio call
     * @param token:            token to authenticate
     * @param subscriberId:     id of the subscriber
     * @param subscriberCode:   code of the subscriber
     * @param streamName:       name of the stream
     */
    void joinToConferenceRoom(String roomId, String streamId, boolean videoCallEnabled, boolean audioCallEnabled, String token, String subscriberId, String subscriberCode, String streamName);

    /**
     * This is used to join a conference room as player
     *
     * @param roomId: id for the conference room
     */
    void joinToConferenceRoom(String roomId);

    /**
     * This is used to leave from a conference room
     *
     * @param roomId: id for the conference room
     */
    void leaveFromConference(String roomId);

    /**
     * This is used to stop a stream publishing, playing or peer to peer call
     *
     * @param streamId id for the stream
     */
    void stop(String streamId);

    /**
     * This is used to send data via data channel
     *
     * @param streamId: id for the stream
     * @param buffer:   data to send
     */
    void sendMessageViaDataChannel(String streamId, DataChannel.Buffer buffer);

    /**
     * This Function Can be used to set Degradation Preference for the Stream such as to
     * Maintaining resolution or FrameRate in bad network conditions
     *
     * @param degradationPreference : RtpParameters.DegradationPreference
     */

    void setDegradationPreference(RtpParameters.DegradationPreference degradationPreference);

    /**
     * This is used to change video source on the fly
     *
     * @param newSource: may be front camera, rear camera, screen or custom source which provides video frames
     */
    void changeVideoSource(StreamSource newSource);

    /**
     * Sets quality of a stream/subtrack to certain resolution.
     *
     * @param mainTrackStreamId : If its a single stream pass its streamId as mainTrackId. If you want to change resolution of a subtrack, pass mainTrack stream Id and subtrack stream Id.
     * @param subTrackStreamId  : If you want to change resolution of sub track, pass its subTrack stream Id. If you dont want to change subtrack resolution pass null or empty str.
     * @param resolutionHeight: The height to be forced. If you set the height to zero, it will become auto. example: For 720p pass 720 as resolution.
     */
    void forceStreamQuality(String mainTrackStreamId, String subTrackStreamId, int resolutionHeight);

    /**
     * enable/disable video stream of call. Use this before call start.
     * Eg if you call this method before call start with false, you wont be able to turn on video stream during call.
     * For in call video sending toggle use toggleSendVideo
     *
     * @param enabled true for enable, false for disable
     */
    void setVideoEnabled(boolean enabled);

    /**
     * enable/disable audio stream of call.
     * Use this before call start.
     * For in call audio sending(for example muting microphone) toggle use toggleSendAudio
     *
     * @param enabled true for enable, false for disable
     */
    void setAudioEnabled(boolean enabled);

    /**
     * enable/disable played track stream from the server
     *
     * @param streamId      id for the main track
     * @param selectedTrack id for the subtrack
     * @param enabled       true for enable, false for disable
     */
    void enableTrack(String streamId, String selectedTrack, boolean enabled);

    /**
     * Called to set the renderer for a video track
     *
     * @param renderer:   renderer for the video track
     * @param videoTrack: video track to set
     */
    void setRendererForVideoTrack(SurfaceViewRenderer renderer, VideoTrack videoTrack);

    /**
     * Called to swap the local renderer with the first remote renderer
     *
     * @param isSwappedFeeds: true if it's swapped
     */
    void setSwappedFeeds(boolean isSwappedFeeds);

    /**
     * Switches the front and rear camera
     */
    void switchCamera();

    /**
     * This is used to change the capture format for the camera
     *
     * @param width:     desired width
     * @param height:    desired height
     * @param framerate: desired framerate
     */
    void changeCaptureFormat(int width, int height, int framerate);

    /**
     * Return if data channel is enabled and open
     *
     * @return true if data channel is available
     * false if it's not opened either by mobile or server side
     */
    boolean isDataChannelEnabled();

    /**
     * This is used to get streaming status for a stream id
     *
     * @param streamId: id for the stream
     * @return true if it's streaming at the moment
     */
    boolean isStreaming(String streamId);

    /**
     * This is used to get room info from server
     *
     * @param roomId:   id for the room
     * @param streamId: id for the calling participant
     */
    void getRoomInfo(String roomId, String streamId);

    /**
     * This is used to get stream info list for a stream from server
     *
     * @param streamId: id for the stream
     */
    void getStreamInfoList(String streamId);

    /**
     * This is used to get reconnecting status
     *
     * @return true if it's reconnecting
     */
    boolean isReconnectionInProgress();

    /**
     * Get the error
     *
     * @return error or null if not
     */
    String getError();

    /**
     * Get the current configuration for the client
     *
     * @return current configuration
     */
    WebRTCClientConfig getConfig();

    /**
     * Get the current video capturer, used for custom video feed
     *
     * @return current video capturer
     */
    VideoCapturer getVideoCapturer();

    /**
     * Get the current audio input, used for custom audio feed
     *
     * @return current audio input
     */
    CustomWebRtcAudioRecord getAudioInput();

    /**
     * Called to request the subtracks for a main track from server
     */
    void getTrackList(String streamId, String token);

    /**
     * Called to get the broadcast object from server
     *
     * @param streamId: id for the broadcast
     */
    void getBroadcastObject(String streamId);

    /**
     * Releases the renderer
     *
     * @param renderer: renderer to release
     */
    void releaseRenderer(SurfaceViewRenderer renderer);

    /**
     * Toggle audio for all participants in a call.
     * If 'enabled' is true, unmutes all participants; otherwise, mutes all participants.
     * This method will only work for current participants. If a new participant joins, he wont be muted.
     * If you want to keep all participants muted all the time, call this method every time a participant joins.
     */
    void toggleAudioOfAllParticipants(boolean enabled);

    /**
     * Register user push notification token to Ant Media Server according to subscriberId and authToken.
     */
    void registerPushNotificationToken(String subscriberId, String authToken, String pushNotificationToken, String tokenType);

    /**
     * Send push notification to subscribers.
     */
    void sendPushNotification(String subscriberId, String authToken, JSONObject pushNotificationContent, JSONArray subscriberIdsToNotify);

    /**
     * Get stats collector object. StatsCollector holds data of stream quality such as RTT, Jitter, Bytes sent/received.
     */
    StatsCollector getStatsCollector();

    /**
     * Returns true if SDK resources are released and its shutdown, false otherwise.
     */
    boolean isShutdown();

    /**
     * Use this to get the local audio track status.
     * E.g., if the microphone is off, it will return false. If the microphone is on, it will return true.
     */
    boolean isLocalAudioTrackEnabled();

    /**
     * Use this to toggle sending video during a call.
     * E.g., turn the camera on or off during a call.
     */
    void toggleSendVideo(boolean enableVideo);

    /**
     * Use this to toggle sending audio during a call.
     * E.g., call this with false to mute the microphone during a call.
     */
    void toggleSendAudio(boolean enableAudio);

    /**
     * Get the audio sending status.
     * E.g., if the microphone is muted, it will return false; otherwise, it will return true.
     */
    boolean isSendAudioEnabled();

    /**
     * Get the video sending status.
     * E.g., if the camera is turned off, it will return false; otherwise, it will return true.
     */
    boolean isSendVideoEnabled();

    /**
     * Called to get the subscriber count for a broadcast
     *
     * @param streamId: id for the broadcast
     */
    void getSubscriberCount(String streamId);

    /**
     * Called to get the subscriber list for a broadcast
     *
     * @param streamId: id for the broadcast
     * @param offset: offset of the list
     * @param size: size of the list
     */
    void getSubscriberList(String streamId, long offset, long size);

    /**
     * Called to get the debug info for the viewer
     *
     * @param streamId: id for the broadcast
     */
    void getDebugInfo(String streamId);
}
