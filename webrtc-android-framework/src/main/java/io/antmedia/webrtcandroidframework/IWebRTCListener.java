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
}
