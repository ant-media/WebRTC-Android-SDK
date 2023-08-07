package io.antmedia.webrtcandroidframework;

import org.webrtc.VideoTrack;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket; /**
 * Created by karinca on 23.10.2017.
 */

public interface IWebRTCListener {

 /**
  * It's called when websocket connection has been disconnected
  */
 void onDisconnected(String streamId);

 /**
  * This method is fired when publishing(broadcasting) to the server has been finished
  */
 void onPublishFinished(String streamId);

 /**
  * This method is fired when playing stream has been finished
  */
 void onPlayFinished(String streamId);

 /**
  * This method is fired when publishing to the server has been started
  */
 void onPublishStarted(String streamId);

 /**
  * This method is fired when playing has been started
  */
 void onPlayStarted(String streamId);

 /**
  * This method is fired when client tries to play a stream that is not available in the server
  */
 void noStreamExistsToPlay(String streamId);

 void onError(String description, String streamId);

 void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId);

 /**
  * It's called if client tried to stream with a stream id that is currently used in another stream.
  *
  */
 void streamIdInUse(String streamId);

 /**
  * This method is called every time, connection is established with the remote peer.
  * It's called both p2p, play and publish modes.
  */
 void onIceConnected(String streamId);

 /**
  * This method is fired when Ice connection has been disconnected
  */
 void onIceDisconnected(String streamId);

 /**
  * It's called in multi track play mode and reports the tracks to the listener
  * @param tracks
  */
 void onTrackList(String[] tracks);

 /**
  * It's called when bitrate measurements received from server.
  * targetBitrate should be greater than (videoBitrate + audioBitrate) for a good quality stream
  * @param streamId
  * @param targetBitrate
  * @param videoBitrate
  * @param audioBitrate
  */
 void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate);

 /**
  * It's called when stream info list received from server.
  * @param streamId
  * @param streamInfoList
  */
 void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList);

 /**
  * It's called when a new video track is added.
  * @param track
  */
 void onNewVideoTrack(VideoTrack track);

 /**
  * It's called when a video track is removed.
  *
  * @param track
  */
 void onVideoTrackEnded(VideoTrack track);

    /**
     * @param streamId
     * It's called when reconnection attempt is started
     */
    void onReconnectionAttempt(String streamId);


    /**
     * @param streamId
     * It's called when mic is muted for the stream
     */
    void onMutedFor(String streamId);

    /**
     * @param streamId
     * It's called when mic is unmuted for the stream
     */
    void onUnmutedFor(String streamId);

    /**
     * @param streamId
     * It's called when camera is turned on for the stream
     */
    void onCameraTurnOnFor(String streamId);

    /**
     * @param streamId
     * It's called when camera is turned off for the stream
     */
    void onCameraTurnOffFor(String streamId);

    /**
     * @param streamId
     * @param micStatus
     * @param cameraStatus
     * It's called when status of mic and camera is updated for the stream
     */
    void onSatatusUpdateFor(String streamId, boolean micStatus, boolean cameraStatus);
}
