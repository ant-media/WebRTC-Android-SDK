package io.antmedia.webrtcandroidframework.api;

import org.webrtc.VideoTrack;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.core.StreamInfo;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;
import io.antmedia.webrtcandroidframework.websocket.Subscriber;

/**
 * Created by karinca on 23.10.2017.
 * This interface is used to listen WebRTCClient (or SDK) events
 * You may implement this interface or extend {@link DefaultWebRTCListener} class
 * If you want to create a conference application, you may extend {@link DefaultConferenceWebRTCListener} class
 */

public interface IWebRTCListener {

 /**
  * It's called when websocket connection established to server.
  */
 void onWebSocketConnected();

 /**
  * It's called when websocket connection has been disconnected
  */
 void onWebSocketDisconnected();

 /**
  * It's called when websocket connection has been disconnected
  */
 @Deprecated //use onWebSocketDisconnected, onIceDisconnected instead
 void onDisconnected();

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
  * This method will be called when the peer connection object has been created
  * This would allow user to have a low level control on the peer connection object before connection establishment
  */
 void onPeerConnectionCreated(String streamId);

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
  *
  * @param track
  * @param trackId
  * @return
  */
 void onNewVideoTrack(VideoTrack track);

 /**
  * It's called when a new video track is added.
  *
  * @param track
  * @param trackId
  * @return
  */
 void onNewVideoTrack(VideoTrack track, String trackId);

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
     * It's called when joiened the room
     *
     * @param streamId
     * @param streams in the room
     */
    void onJoinedTheRoom(String streamId, String[] streams);

    /**
     * It's called when room information is received
     *
     * @param streams
     */
    void onRoomInformation(String[] streams);

    /**
     * It's called when left the room
     *
     * @param roomId
     */
   void onLeftTheRoom(String roomId);

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

    /**
     * It's called in WebRTCClient constructor to set
     */
    void setWebRTCClient(IWebRTCClient webRTCClient);

    /**
     * It's called when a stream is restored (restarted in the restoration timeout)
     * @param streamId
     */
    void onSessionRestored(String streamId);

    /**
     * It's called when a broadcast object is received from server
     * @param broadcast
     */
    void onBroadcastObject(Broadcast broadcast);

    void onPeerConnectionClosed();

    /**
     * It's called when all peer connection states are CONNECTED after reconnection.
     */
    void onReconnectionSuccess();

    /**
     * It's called when user attempts to publish a stream.
     */
    void onPublishAttempt(String streamId);

   /**
    * It's called when streams/subtracks resolution changes.
    */
    void onResolutionChange(String streamId, int resolution);

    /**
    * It's called when webrtc client releases all resources and shutdowns.
    */
    void onShutdown();

    /**
     * It's called when user attempts to play a stream.
     */
    void onPlayAttempt(String streamId);

    /**
     * It's called when user attempts to join peer to peer.
     */
    void onJoinAttempt(String streamId);

    /**
     * It's called when user joins to a P2P room.
     */
    void onJoined(String streamId);

    /**
     * It's called when user left P2P room.
     */
    void onLeft(String streamId);


 /**
  * It's called when Subscriber Count received.
  */
 void onSubscriberCount(String streamId, int count);

 /**
  * It's called when Subscriber List received.
  */
 void onSubscriberList(String streamId, Subscriber[] subscribers);
}
