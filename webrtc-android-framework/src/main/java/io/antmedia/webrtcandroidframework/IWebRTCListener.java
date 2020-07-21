package io.antmedia.webrtcandroidframework;

import de.tavendo.autobahn.WebSocket; /**
 * Created by karinca on 23.10.2017.
 */

public interface IWebRTCListener {

 /**
  * It's called when websocket connection has been disconnected
  */
 void onDisconnected();

 /**
  * This method is fired when publishing(broadcasting) to the server has been finished
  */
 void onPublishFinished();

 /**
  * This method is fired when playing stream has been finished
  */
 void onPlayFinished();

 /**
  * This method is fired when publishing to the server has been started
  */
 void onPublishStarted();

 /**
  * This method is fired when playing has been started
  */
 void onPlayStarted();

 /**
  * This method is fired when client tries to play a stream that is not available in the server
  */
 void noStreamExistsToPlay();

 void onError(String description);

 void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code);

 /**
  * This method is called every time, connection is established with the remote peer.
  * It's called both p2p, play and publish modes.
  */
 void onIceConnected();

 /**
  * This method is fired when Ice connection has been disconnected
  */
 void onIceDisconnected();

 /**
  * It's called in multi track play mode and reports the tracks to the listener
  * @param tracks
  */
 void onTrackList(String[] tracks);

 /**
  * It's called when bitrate measurements received from serves.
  * targetBitrate should be greater than (videoBitrate + audioBitrate) for a good quality stream
  * @param streamId
  * @param targetBitrate
  * @param videoBitrate
  * @param audioBitrate
  */
 void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate);
}
