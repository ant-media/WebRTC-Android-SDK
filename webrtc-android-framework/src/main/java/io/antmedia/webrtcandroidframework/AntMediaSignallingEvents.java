package io.antmedia.webrtcandroidframework;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import io.antmedia.webrtcandroidframework.apprtc.AppRTCClient;

public interface AntMediaSignallingEvents {
    void onPublishStarted(String streamId);

    void onRemoteIceCandidate(String streamId, IceCandidate candidate);

    void onTakeConfiguration(String streamId, SessionDescription sdp);

    void onPublishFinished(String streamId);

    void onPlayStarted(String streamId);

    void onPlayFinished(String streamId);

    void noStreamExistsToPlay(String streamId);

    void onStartStreaming(String streamId);

    void onJoinedTheRoom(String streamId, String[] streams);

    void onStreamJoined(String streamId);

    void onStreamLeaved(String streamId);

    void onDisconnected();
}
