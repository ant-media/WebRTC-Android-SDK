package io.antmedia.webrtcandroidframework;

import de.tavendo.autobahn.WebSocket; /**
 * Created by karinca on 23.10.2017.
 */

public interface IWebRTCListener {


 void onDisconnected();

 void onPublishFinished();

 void onPlayFinished();

 void onPublishStarted();

 void onPlayStarted();

 void noStreamExistsToPlay();

 void onError(String description);

 void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code);

 /**
  * This method is called every time, connection is established with the remote peer.
  * It's called both p2p, play and publish modes.
  */
 void onConnected();

 /**
  * It's called in multi track play mode and reports the tracks to the listener
  * @param tracks
  */
 void onTrackList(String[] tracks);
}
