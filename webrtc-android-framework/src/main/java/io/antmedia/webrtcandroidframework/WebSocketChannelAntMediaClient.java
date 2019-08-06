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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import io.antmedia.webrtcandroidframework.apprtc.util.AsyncHttpURLConnection;
import io.antmedia.webrtcandroidframework.apprtc.util.AsyncHttpURLConnection.AsyncHttpEvents;

/**
 * WebSocket client implementation.
 *
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */

public class WebSocketChannelAntMediaClient {
  private static final String TAG = "WSChannelRTCClient";
  private static final int CLOSE_TIMEOUT = 1000;
  private final WebSocketChannelEvents events;
  private final Handler handler;
  private final String streamMode;
  private WebSocketConnection ws;
  private WebSocketObserver wsObserver;
  private String wsServerUrl;
  private String postServerUrl;
  private String roomID;
  private String clientID;
  private String token;
  private WebSocketConnectionState state;
  private final Object closeEventLock = new Object();
  private boolean closeEvent;
  // WebSocket send queue. Messages are added to the queue when WebSocket
  // client is not registered and are consumed in register() call.
  private final LinkedList<String> wsSendQueue;



  /**
   * Possible WebSocket connection states.
   */
  public enum WebSocketConnectionState { NEW, CONNECTED, REGISTERED, CLOSED, ERROR }

  /**
   * Callback interface for messages delivered on WebSocket.
   * All events are dispatched from a looper executor thread.
   */
  public interface WebSocketChannelEvents {
    void onWebSocketMessage(final String message);
    void onWebSocketClose(WebSocketConnectionObserver.WebSocketCloseNotification code);
    void onWebSocketError(final String description);
  }

  public WebSocketChannelAntMediaClient(Handler handler, WebSocketChannelEvents events, String roomID, String streamMode, String token) {
    this.handler = handler;
    this.events = events;
    this.roomID = roomID;
    this.streamMode = streamMode;
    this.token = token;
    clientID = null;
    wsSendQueue = new LinkedList<String>();
    state = WebSocketConnectionState.NEW;
  }

  public WebSocketConnectionState getState() {
    return state;
  }





  public void connect(final String wsUrl, final String postUrl) {
    checkIfCalledOnValidThread();
    if (state != WebSocketConnectionState.NEW) {
      Log.e(TAG, "WebSocket is already connected.");
      return;
    }
    wsServerUrl = wsUrl;
    postServerUrl = postUrl;
    closeEvent = false;

    Log.d(TAG, "Connecting WebSocket to: " + wsUrl + ". Post URL: " + postUrl);
    ws = new WebSocketConnection();
    wsObserver = new WebSocketObserver();
    try {
      ws.connect(new URI(wsServerUrl), wsObserver);

      for (String sendMessage : wsSendQueue) {
        send(sendMessage);
      }
      wsSendQueue.clear();


    } catch (URISyntaxException e) {
      reportError("URI error: " + e.getMessage());
    } catch (WebSocketException e) {
      reportError("WebSocket connection error: " + e.getMessage());
    }
  }

  public void sendPingPong() {

    handler.post(new Runnable() {
      @Override
      public void run() {

        JSONObject json = new JSONObject();
        try {
          json.put(WebSocketRTCAntMediaClient.COMMAND, "ping");

          Log.d(TAG, "Ping/Pong message send " + json.toString());

          ws.sendTextMessage(json.toString());

        } catch (JSONException e) {
          Log.d(TAG, "Ping/Pong message error " + json.toString());

          reportError("WebSocket send JSON error: " + e.getMessage());
        }

      }
    });


  }
  //send messages are sent directly via ws.sendmessage

  public void send(String message) {
    checkIfCalledOnValidThread();
    switch (state) {
      case NEW:
      case CONNECTED:
        // Store outgoing messages and send them after websocket client
        // is registered.
        Log.d(TAG, "WS ACC: " + message);

        wsSendQueue.add(message);
        JSONObject json = new JSONObject();
        try {
          json.put("cmd", "send");
          json.put("msg", message);


          message = json.toString();
          Log.d(TAG, "C->WSS: " + message);



          ws.sendTextMessage(message);
        } catch (JSONException e) {
          reportError("WebSocket send JSON error: " + e.getMessage());
        }


        return;
      case ERROR:
      case CLOSED:
        Log.e(TAG, "WebSocket send() in error or closed state : " + message);
        return;
      case REGISTERED:

        break;
    }
  }


  public void sendTextMessage(String message) {

    ws.sendTextMessage(message);

  }

  // This call can be used to send WebSocket messages before WebSocket
  // connection is opened.
  public void post(String message) {
    checkIfCalledOnValidThread();
    sendWSSMessage("POST", message);
  }

  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
    Log.d(TAG, "Disconnect WebSocket. State: " + state);
    if (state == WebSocketConnectionState.REGISTERED) {
      // Send "bye" to WebSocket server.
      send("{\"type\": \"bye\"}");
      state = WebSocketConnectionState.CONNECTED;
      // Send http DELETE to http WebSocket server.
      sendWSSMessage("DELETE", "");
    }
    // Close WebSocket in CONNECTED or ERROR states only.
    if (state == WebSocketConnectionState.CONNECTED || state == WebSocketConnectionState.ERROR) {

      ws.disconnect();
      state = WebSocketConnectionState.CLOSED;

      // Wait for websocket close event to prevent websocket library from
      // sending any pending messages to deleted looper thread.
      if (waitForComplete) {
        synchronized (closeEventLock) {
          while (!closeEvent) {
            try {
              closeEventLock.wait(CLOSE_TIMEOUT);
              break;
            } catch (InterruptedException e) {
              Log.e(TAG, "Wait error: " + e.toString());
            }
          }
        }
      }
    }
    Log.d(TAG, "Disconnecting WebSocket done.");
  }

  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    handler.post(new Runnable() {
      @Override
      public void run() {
        if (state != WebSocketConnectionState.ERROR) {
          state = WebSocketConnectionState.ERROR;
          events.onWebSocketError(errorMessage);
        }
      }
    });
  }

  // Asynchronously send POST/DELETE to WebSocket server.
  private void sendWSSMessage(final String method, final String message) {
    String postUrl = postServerUrl + "/" + roomID + "/" + clientID;
    Log.d(TAG, "WS " + method + " : " + postUrl + " : " + message);
    AsyncHttpURLConnection httpConnection =
            new AsyncHttpURLConnection(method, postUrl, message, new AsyncHttpEvents() {
              @Override
              public void onHttpError(String errorMessage) {
                reportError("WS " + method + " error: " + errorMessage);
              }

              @Override
              public void onHttpComplete(String response) {}
            });
    httpConnection.send();
  }

  // Helper method for debugging purposes. Ensures that WebSocket method is
  // called on a looper thread.
  private void checkIfCalledOnValidThread() {
    if (Thread.currentThread() != handler.getLooper().getThread()) {
      throw new IllegalStateException("WebSocket method is not called on valid thread");
    }
  }


  public void register() {
    checkIfCalledOnValidThread();

    JSONObject json = new JSONObject();
    try {
      json.put(WebSocketRTCAntMediaClient.COMMAND, streamMode);
      json.put(WebSocketRTCAntMediaClient.STREAM_ID, roomID);
      json.put(WebSocketRTCAntMediaClient.TOKEN_ID, token);

      Log.d(TAG, "websocket first message from client " + json.toString());

      ws.sendTextMessage(json.toString());



    } catch (JSONException e) {
      reportError("WebSocket send JSON error: " + e.getMessage());
    }
  }

  private class WebSocketObserver implements WebSocketConnectionObserver {
    @Override
    public void onOpen() {
      Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
      handler.post(new Runnable() {
        @Override
        public void run() {
          state = WebSocketConnectionState.CONNECTED;
          // Check if we have pending register request.
          register();
        }
      });
    }

    @Override
    public void onClose(WebSocketCloseNotification code, String reason) {
      Log.d(TAG, "WebSocket connection closed. Code: " + code + ". Reason: " + reason + ". State: "
              + state);
      synchronized (closeEventLock) {
        closeEvent = true;
        closeEventLock.notify();
      }
      handler.post(new Runnable() {
        @Override
        public void run() {
          if (state != WebSocketConnectionState.CLOSED) {
            state = WebSocketConnectionState.CLOSED;
            events.onWebSocketClose(code);
          }
        }
      });
    }

    @Override
    public void onTextMessage(String payload) {
      Log.d(TAG, "WSS->C: " + payload);
      final String message = payload;
      handler.post(new Runnable() {
        @Override
        public void run() {
          //  if (state == WebSocketConnectionState.CONNECTED
          //        || state == WebSocketConnectionState.REGISTERED) {
          events.onWebSocketMessage(message);
          //  }
        }
      });
    }

    @Override
    public void onRawTextMessage(byte[] payload) {}

    @Override
    public void onBinaryMessage(byte[] payload) {}
  }
}
