/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package io.antmedia.webrtcandroidframework;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.WebSocketChannelAntMediaClient.WebSocketChannelEvents;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCClient;


/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class WebSocketRTCAntMediaClient implements AppRTCClient, WebSocketChannelEvents {

  private static final String TAG = "WSRTCClient";
  private static final String ROOM_JOIN = "join";
  private static final String ROOM_MESSAGE = "message";
  private static final String ROOM_LEAVE = "leave";


  public static final String VIDEO = "video";
  public static final String AUDIO = "audio";


  /*
  WS communication parameters for Ant media server integration
   */

  private static final String COMMAND_PUBLISH = "publish";
  private static final String COMMAND_START = "start";
  private static final String COMMAND_CONFIG = "takeConfiguration";
  private static final String COMMAND_CANDIDATE = "takeCandidate";
  private static final String COMMAND_STOP = "stop";
  public static final String NOTIFICATION_COMMAND = "notification";
  public static final String TAKE_CANDIDATE_COMMAND = "takeCandidate";
  public static final String TAKE_CONFIGURATION_COMMAND = "takeConfiguration";
  public static final String PUBLISH_STARTED_DEFINITION = "publish_started";
  private static final String PUBLISH_FINISHED_DEFINITION = "publish_finished";
  private static final String PLAY_STARTED_DEFINITION = "play_started";
  private static final String PLAY_FINISHED_DEFINITION = "play_finished";
  public static final String COMMAND = "command";
  public static final String STREAM_ID = "streamId";
  public static final String TOKEN_ID = "token";
  private static final String COMMAND_PLAY = "play";
  public static final String COMMAND_JOIN = "join";
  public static final String ERROR_COMMAND = "error";
  public static final String PONG = "pong";
  public static final String NO_STREAM_EXIST = "no_stream_exist";
  public static final String DEFINITION = "definition";
  public static final  long TIMER_DELAY  = 3000L;
  public static final  long TIMER_PERIOD = 2000L;

  private SignalingParameters signalingParameters;
  private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

  private enum MessageType { MESSAGE, LEAVE }

  private final Handler handler;
  private boolean initiator;
  private SignalingEvents events;
  private WebSocketChannelAntMediaClient wsClient;
  private ConnectionState roomState;
  private RoomConnectionParameters connectionParameters;
  private String messageUrl;
  private String leaveUrl;
  //private WebSocketConnection ws;
  //private final LinkedList<String> wsSendQueue = null;

  private String wsURL;
  private String serverName;
  private String leaveMessage;
  private String stunServerUri ="stun:stun.l.google.com:19302";
  public ScheduledExecutorService pingPongExecutor;
  private int pingPongTimoutCount = 0;





  public WebSocketRTCAntMediaClient(SignalingEvents events) {
    this.events = events;
    roomState = ConnectionState.NEW;
    final HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());



  }

  // --------------------------------------------------------------------
  // AppRTCClient interface implementation.
  // Asynchronously connect to an AppRTC room URL using supplied connection
  // parameters, retrieves room parameters and connect to WebSocket server.


  @Override
  public void connectToRoom(RoomConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;
    handler.post(new Runnable() {
      @Override
      public void run() {
        try {
          connectToRoomInternal();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }



  @Override
  public void disconnectFromRoom() {

    if (roomState == ConnectionState.CONNECTED) {
      handler.post(new Runnable() {

        @Override
        public void run() {

          sendLeaveMessage();
        }
      });
    }
  }

  //@Override
  public void setStunServerUri(String stunServerUri) {
    this.stunServerUri=stunServerUri;
  }


  // Connects to room - function runs on a local looper thread.
  private void connectToRoomInternal() {

    //  setServerName("192.168.1.4");


    setWsURL(connectionParameters.roomUrl);

    Log.d(TAG, "Connect to room: " + connectionParameters.roomId);

    roomState = ConnectionState.NEW;


    wsClient = new WebSocketChannelAntMediaClient(handler, this, connectionParameters.roomId, connectionParameters.mode, connectionParameters.token);

    if(connectionParameters.mode.equals(COMMAND_JOIN)){
      setLeaveMessage("leave");
    }else if(connectionParameters.mode.equals(COMMAND_PUBLISH)){
      setLeaveMessage(COMMAND_STOP);
    } else   if(connectionParameters.mode.equals(COMMAND_PLAY)){
      setLeaveMessage(COMMAND_STOP);
    } else setLeaveMessage("leave");

    wsClient.connect(wsURL, "");

  }

  // Disconnect from room and send bye messages - runs on a local looper thread.
  private void disconnectFromRoomInternal() {
    Log.d(TAG, "Disconnect. Room state: " + roomState);
    if (roomState == ConnectionState.CONNECTED) {
      Log.d(TAG, "Closing room.");
    }
    roomState = ConnectionState.CLOSED;
    if (wsClient != null) {

      wsClient.disconnect(true);
    }
  }
  public void sendPingPongMessage() {

    if (wsClient != null) {
      Log.d(TAG, "Ping Pong message is sent");

      wsClient.sendPingPong();
    }
  }


  public void startPingPongTimer(){
    Log.d(TAG, "Ping Pong timer is started");

    Runnable timerTask = new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Ping Pong timer is executed");
        sendPingPongMessage();
        if (pingPongTimoutCount == 2){
          Log.d(TAG, "Ping Pong websocket response not received for 4 seconds");
          stopPingPongTimer();
          onWebSocketClose(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification.CONNECTION_LOST);
        }
        pingPongTimoutCount++;

      }
    };

    pingPongExecutor = Executors.newSingleThreadScheduledExecutor();
    pingPongExecutor.scheduleAtFixedRate(timerTask, TIMER_DELAY, TIMER_PERIOD, TimeUnit.MILLISECONDS);

  }

  public void stopPingPongTimer(){

    Log.d(TAG, "Ping Pong timer stop called");

    if (pingPongExecutor != null) {
      pingPongExecutor.shutdown();
      pingPongExecutor = null;
      pingPongTimoutCount = 0;
    }

  }

  // Helper functions to get connection, post message and leave message URLs
  private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters.roomId
            + getQueryString(connectionParameters);
  }

  private String getMessageUrl(
          RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters.roomId
            + "/" + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getLeaveUrl(
          RoomConnectionParameters connectionParameters, SignalingParameters signalingParameters) {
    return connectionParameters.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters.roomId + "/"
            + signalingParameters.clientId + getQueryString(connectionParameters);
  }

  private String getQueryString(RoomConnectionParameters connectionParameters) {
    if (connectionParameters.urlParameters != null) {
      return "?" + connectionParameters.urlParameters;
    } else {
      return "";
    }
  }

  // Callback issued when room parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(final SignalingParameters signalingParameters) {
    Log.d(TAG, "Room connection completed.");
    if (connectionParameters.loopback
            && (!signalingParameters.initiator || signalingParameters.offerSdp != null)) {
      reportError("Loopback room is busy.");
      return;
    }
    if (!connectionParameters.loopback && !signalingParameters.initiator
            && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP in room response.");
    }
    initiator = signalingParameters.initiator;
    messageUrl = getMessageUrl(connectionParameters, signalingParameters);
    leaveUrl = getLeaveUrl(connectionParameters, signalingParameters);
    Log.d(TAG, "Message URL: " + messageUrl);
    Log.d(TAG, "Leave URL: " + leaveUrl);
    roomState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onConnectedToRoom(signalingParameters);


  }

  // @Override
  public void sendLeaveMessage(){

    Log.d(TAG, "stop ws connection command");

    JSONObject jsonStop = new JSONObject();

    try {
      jsonStop.put(COMMAND,leaveMessage);
      jsonStop.put(STREAM_ID,connectionParameters.roomId);


      Log.d(TAG, "stop message" + jsonStop.toString());

      wsClient.sendTextMessage(jsonStop.toString());

    } catch (JSONException e) {
      reportError("WebSocket send JSON error: " + e.getMessage());
    }

  }




  // Send local offer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }

        Log.d(TAG, "send offer sdp ");

        JSONObject jsonOfferSdp = new JSONObject();
        try {
          jsonOfferSdp.put("command", "takeConfiguration");
          jsonOfferSdp.put("streamId", connectionParameters.roomId);
          jsonOfferSdp.put("type", "offer");
          jsonOfferSdp.put("sdp", sdp.description);


          // message = json.toString();
          Log.d(TAG, "send offer sdp  " + jsonOfferSdp.toString());

          wsClient.sendTextMessage(jsonOfferSdp.toString());

        } catch (JSONException e) {
          reportError("WebSocket send JSON error: " + e.getMessage());
        }


        if (connectionParameters.loopback) {
          // In loopback mode rename this offer to answer and route it back.
          SessionDescription sdpAnswer = new SessionDescription(
                  SessionDescription.Type.fromCanonicalForm("answer"), sdp.description);
          events.onRemoteDescription(sdpAnswer);
        }
      }
    });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (connectionParameters.loopback) {
          Log.e(TAG, "Sending answer in loopback mode.");
          return;
        }


        JSONObject jsonAnswerSdp = new JSONObject();

        jsonPut(jsonAnswerSdp, "command", "takeConfiguration");
        jsonPut(jsonAnswerSdp, "streamId", connectionParameters.roomId);
        jsonPut(jsonAnswerSdp, "type", "answer");
        jsonPut(jsonAnswerSdp, "sdp", sdp.description);

        wsClient.sendTextMessage(jsonAnswerSdp.toString());

        Log.d(TAG, "SDP Answer: " + jsonAnswerSdp.toString());
      }
    });
  }

  // Send Ice candidate to the other participant.
  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    handler.post(new Runnable() {
      @Override
      public void run() {


        JSONObject jsonLocalIce = new JSONObject();

        jsonPut(jsonLocalIce, "command", "takeCandidate");
        jsonPut(jsonLocalIce, "streamId", connectionParameters.roomId);
        jsonPut(jsonLocalIce, "label", candidate.sdpMLineIndex);
        jsonPut(jsonLocalIce, "id", candidate.sdpMid);
        jsonPut(jsonLocalIce, "candidate", candidate.sdp);



        // Call initiator sends ice candidates to GAE server.
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate in non connected state.");
          return;
        }


        // sendPostMessage(messageUrl, json.toString());

        if (connectionParameters.loopback) {
          events.onRemoteIceCandidate(candidate);
        }

        // Call receiver sends ice candidates to websocket server.

        wsClient.sendTextMessage(jsonLocalIce.toString());

        Log.d(TAG, "Local ICE: " + jsonLocalIce.toString());
      }
    });
  }

  // Send removed Ice candidates to the other participant.


  //TODO: this function should be implemented later (davut)

  @Override
  public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);
        if (initiator) {
          // Call initiator sends ice candidates to GAE server.
          if (roomState != ConnectionState.CONNECTED) {
            reportError("Sending ICE candidate removals in non connected state.");
            return;
          }
          if (connectionParameters.loopback) {
            events.onRemoteIceCandidatesRemoved(candidates);
          }
        } else {
          // Call receiver sends ice candidates to websocket server.
          wsClient.send(json.toString());
        }
      }
    });
  }

  // --------------------------------------------------------------------
  // WebSocketChannelEvents interface implementation.
  // All events are called by WebSocketChannelAntMediaClient on a local looper thread
  // (passed to WebSocket client constructor).
  @Override
  public void onWebSocketMessage(final String msg) {
    if (wsClient.getState() != WebSocketChannelAntMediaClient.WebSocketConnectionState.CONNECTED) {
      Log.e(TAG, "Got WebSocket message in non registered state.");
      return;
    }
    try {
      JSONObject json = new JSONObject(msg);

      String commandText = json.getString(COMMAND);

      if (commandText.equals("start")) {

        signalingParametersReady(getSignalingParameters(true, null));

        Log.d(TAG, "websocket server first reply: "+ commandText);

        initiator=true;
      }

      else if (commandText.equals(TAKE_CONFIGURATION_COMMAND)) {


        SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(json.getString("type"));
        SessionDescription sdp = new SessionDescription(
                type, json.getString("sdp"));
        if (type == SessionDescription.Type.OFFER) {
          signalingParametersReady(getSignalingParameters(false, sdp));

        }
        else {

          events.onRemoteDescription(sdp);
        }

        Log.d(TAG, "websocket server sdp reply: "+ json.getString("sdp"));
      }

      else if (commandText.equals(TAKE_CANDIDATE_COMMAND)) {

        events.onRemoteIceCandidate(toJavaCandidate(json));

        Log.d(TAG, "websocket server ice candidate reply: "+ toJavaCandidate(json).sdpMid);

      }
      else if (commandText.equals(NOTIFICATION_COMMAND)) {

        String definition= json.getString(DEFINITION);

        Log.d(TAG, "notification:   "+ definition);
        if (definition.equals(PUBLISH_STARTED_DEFINITION)) {
          events.onPublishStarted();
          startPingPongTimer();
        }
        else if (definition.equals(PUBLISH_FINISHED_DEFINITION)) {
          events.onPublishFinished();
          disConnectAndQuit();
          stopPingPongTimer();
        }
        else if (definition.equals(PLAY_STARTED_DEFINITION)) {
          events.onPlayStarted();

        }
        else if (definition.equals(PLAY_FINISHED_DEFINITION)) {
          events.onPlayFinished();
          disConnectAndQuit();
        }
      }
      else if (commandText.equals(ERROR_COMMAND))
      {

        String definition= json.getString(DEFINITION);
        Log.d(TAG, "error command received: "+ definition);
        stopPingPongTimer();

        if (definition.equals(NO_STREAM_EXIST))
        {
          events.noStreamExistsToPlay();
          disConnectAndQuit();
        }
      }

      else if (commandText.equals(PONG))
      {
        pingPongTimoutCount = 0;
        Log.d(TAG, "pong reply is received");
      }

      else {

        reportError("Received offer for call receiver: " + msg);
      }


    } catch (JSONException e) {
      reportError("WebSocket message JSON parsing error: " + e.toString());
    }

  }

  public void disConnectAndQuit() {
    handler.post(new Runnable() {
      @Override
      public void run() {
        disconnectFromRoomInternal();

        if (events != null) {
          events.onDisconnected();
        }
        handler.getLooper().quit();
      }
    });
  }

  @Override
  public void onWebSocketClose(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code) {
    events.onChannelClose(code);
  }

  @Override
  public void onWebSocketError(String description) {
    reportError("WebSocket error: " + description);
  }

  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
      }
    });
  }

  // Put a |key|->|value| mapping in |json|.
  private static void jsonPut(JSONObject json, String key, Object value) {
    try {
      json.put(key, value);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }






  // Converts a Java candidate to a JSONObject.
  private JSONObject toJsonCandidate(final IceCandidate candidate) {
    JSONObject json = new JSONObject();
    jsonPut(json, "label", candidate.sdpMLineIndex);
    jsonPut(json, "id", candidate.sdpMid);
    jsonPut(json, "candidate", candidate.sdp);
    return json;
  }



  // Converts a JSON candidate to a Java object.
  IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
    return new IceCandidate(
            json.getString("id"), json.getInt("label"), json.getString("candidate"));
  }



  public SignalingParameters getSignalingParameters(boolean initiator, SessionDescription offerSdp) {

    if (signalingParameters == null) {

      List<IceServer> iceServers = new ArrayList();
      iceServers.add(new IceServer(stunServerUri));

      Log.d(TAG, "stun: " + stunServerUri);
      signalingParameters = new SignalingParameters(iceServers, initiator, null, null, null, offerSdp, null);
    }
    return signalingParameters;
  }


  public String getLeaveMessage() {
    return leaveMessage;
  }

  public void setLeaveMessage(String leaveMessage) {
    this.leaveMessage = leaveMessage;
  }




  public String getStunServerUri() {
    return stunServerUri;
  }





  public String getWsURL() {
    return wsURL;
  }

  public void setWsURL(String wsURL) {
    this.wsURL = wsURL;

  }


  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }


}
