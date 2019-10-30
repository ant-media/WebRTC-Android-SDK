package io.antmedia.webrtcandroidframework;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.audio.WebRtcAudioRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConferenceManager implements AntMediaSignallingEvents{
    private final Context context;
    private final Intent intent;
    private final String serverUrl;
    private final String roomName;
    private HashMap<String, WebRTCClient> peers = new HashMap<>();
    private HashMap<SurfaceViewRenderer, WebRTCClient> playRendererAllocationMap = new HashMap<>();
    private SurfaceViewRenderer publishViewRenderer;
    private final IWebRTCListener webRTCListener;
    private WebSocketHandler wsHandler;
    private Handler handler = new Handler();
    private boolean joined = false;

    private int index = 0;

    public ConferenceManager(Context context, IWebRTCListener webRTCListener, Intent intent, String serverUrl, String roomName, SurfaceViewRenderer publishViewRenderer, ArrayList<SurfaceViewRenderer> playViewRenderers) {
        this.context = context;
        this.intent = intent;
        this.publishViewRenderer = publishViewRenderer;
        if (playViewRenderers != null) {
            for (SurfaceViewRenderer svr : playViewRenderers) {
                this.playRendererAllocationMap.put(svr, null);
            }
        }
        this.serverUrl = serverUrl;
        this.roomName = roomName;
        this.webRTCListener = webRTCListener;
    }

    public boolean isJoined() {
        return joined;
    }

    public void joinTheConference() {
        if (wsHandler == null) {
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(serverUrl);
        }
        wsHandler.joinToConferenceRoom(roomName);
    }

    public void leaveFromConference() {
        for (WebRTCClient peer : peers.values()) {
            peer.stopStream();
            deallocateRenderer(peer);
        }

        wsHandler.leaveFromTheConferenceRoom(roomName);

        joined = false;
    }

    private WebRTCClient createPeer(String streamId, String mode) {
        WebRTCClient webRTCClient = new WebRTCClient(webRTCListener, context, new WebRtcAudioRecord.IAudioRecordStatusListener() {
            @Override
            public void audioRecordStarted() {
                Log.i("AudioStatus", "Audio recorder started");
            }

            @Override
            public void audioRecordStopped() {
                Log.i("AudioStatus", "Audio recorder stopped");
            }
        });

        webRTCClient.setWsHandler(wsHandler);

        String tokenId = "tokenId";

        if(mode == IWebRTCClient.MODE_PUBLISH) {
            webRTCClient.setVideoRenderers(null, publishViewRenderer);
        }
        else {
            webRTCClient.setVideoRenderers(null, allocateRenderer(webRTCClient));
        }

        webRTCClient.init(serverUrl, streamId, mode, tokenId, intent);

        return webRTCClient;
    }

    private SurfaceViewRenderer allocateRenderer(WebRTCClient peer) {
        for (Map.Entry<SurfaceViewRenderer, WebRTCClient> entry : playRendererAllocationMap.entrySet()) {
            if(entry.getValue() == null) {
                entry.setValue(peer);
                return entry.getKey();
            }
        }
        return null;
    }

    private void deallocateRenderer(WebRTCClient peer) {
        for (Map.Entry<SurfaceViewRenderer, WebRTCClient> entry : playRendererAllocationMap.entrySet()) {
            if(entry.getValue() == peer) {
                entry.setValue(null);
            }
        }
    }


    //AntMediaSignallingEvents
    @Override
    public void onPublishStarted(String streamId) {
        peers.get(streamId).onPublishStarted(streamId);
    }

    @Override
    public void onRemoteIceCandidate(String streamId, IceCandidate candidate) {
        peers.get(streamId).onRemoteIceCandidate(streamId, candidate);
    }

    @Override
    public void onTakeConfiguration(String streamId, SessionDescription sdp) {
        peers.get(streamId).onTakeConfiguration(streamId, sdp);
    }

    @Override
    public void onPublishFinished(String streamId) {
        peers.get(streamId).onPublishFinished(streamId);
    }

    @Override
    public void onPlayStarted(String streamId) {
        peers.get(streamId).onPlayStarted(streamId);
    }

    @Override
    public void onPlayFinished(String streamId) {
        //it has been deleted because of stream leaved message
        if(peers.containsKey(streamId)) {
            peers.get(streamId).onPlayFinished(streamId);
        }
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        peers.get(streamId).noStreamExistsToPlay(streamId);
    }

    @Override
    public void onStartStreaming(String streamId) {
        peers.get(streamId).onStartStreaming(streamId);
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        WebRTCClient publisher = createPeer(streamId, IWebRTCClient.MODE_PUBLISH);
        peers.put(streamId, publisher);
        publisher.startStream();

        if(streams != null) {
            for (String id : streams) {
                WebRTCClient player = createPeer(id, IWebRTCClient.MODE_PLAY);
                peers.put(id, player);
                player.startStream();
            }
        }

        joined = true;
    }

    @Override
    public void onStreamJoined(String streamId) {
        WebRTCClient player = createPeer(streamId, IWebRTCClient.MODE_PLAY);
        peers.put(streamId, player);
        player.startStream();
    }

    @Override
    public void onStreamLeaved(String streamId) {
        WebRTCClient peer = peers.remove(streamId);
        deallocateRenderer(peer);
        peer.stopStream();
    }

    @Override
    public void onDisconnected() {
    }
}
