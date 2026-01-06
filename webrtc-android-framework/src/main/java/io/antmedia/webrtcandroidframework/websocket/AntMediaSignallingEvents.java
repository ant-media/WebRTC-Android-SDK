package io.antmedia.webrtcandroidframework.websocket;


import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import io.antmedia.webrtcandroidframework.core.StreamInfo;

public interface AntMediaSignallingEvents {

    /**
     * It's called when stream publish has started
     * @param streamId
     */
    void onPublishStarted(String streamId);

    /**
     * It's called when remote ice candidate received
     * @param streamId
     * @param candidate
     */
    void onRemoteIceCandidate(String streamId, IceCandidate candidate);

    /**
     * It's called when other peer(server or browser) sends SDP configuration
     * @param streamId
     * @param sdp
     */
    void onTakeConfiguration(String streamId, SessionDescription sdp);

    /**
     * It's called when stream publishing has started
     * @param streamId
     */
    void onPublishFinished(String streamId);
    /**
     *
     * It's called when stream playing has started
     * @param streamId
     */
    void onPlayStarted(String streamId);

    /**
     * It's called when stream playing has finished
     * @param streamId
     */
    void onPlayFinished(String streamId);

    /**
     * It's called when client tries to play a stream that does not exist in the server
     * @param streamId
     */
    void noStreamExistsToPlay(String streamId);

    /**
     * It's called if client tried to stream with a stream id that is currently used in another stream.
     * @param streamId
     */
    void streamIdInUse(String streamId);


    void onStartStreaming(String streamId);

    /**
     * It's called when client is joined the conference in the server
     * @param streamId
     * @param streams
     */
    void onJoinedTheRoom(String streamId, String[] streams);

    /**
     * It's called when room information is received
     *
     * @param streams
     */
    void onRoomInformation(String[] streams);

    /**
     * It's called when websocket connection is connected
     */
    void onWebSocketConnected();

    /**
     * It's called when websocket connection is disconnected
     */
    void onWebSocketDisconnected();

    /**
     * It's called in responde the getTrackList methods
     * @param tracks
     */
    void onTrackList(String[] tracks);

    /**
     * It's called when bitrate measurements received from server
     * @param streamId
     * @param targetBitrate
     * @param videoBitrate
     * @param audioBitrate
     */
    void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate);

    /**
     * It's called when stream info list received from server
     * @param streamId
     * @param streamInfoList
     */
    void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList);

    /**
     * It's called when error message is received
     * @param streamId
     * @param definition
     */
    void onError(String streamId, String definition);

    /**
     * It's called when participant left the room
     * @param roomId
     */
    void onLeftTheRoom(String roomId);

    /**
     * It's called when session is restored in the server side
     * @param streamId
     */
    void onSessionRestored(String streamId);

    /**
     * It's called when broadcast object is received
     * @param broadcast
     */
    void onBroadcastObject(Broadcast broadcast);

    /**
     * It's called when streams/subtracks resolution changes.
     * @param streamId Stream id of the stream/subtrack whose resolution changed
     * @param resolution New resolution height(720,360,240...)
     */
    void onResolutionChange(String streamId, int resolution);

    /**
     * It's called when peer joins peer to peer room.
     * @param streamId Peer to peer room name/id.
     */
    void onJoined(String streamId);

    /**
     * It's called when peer left peer to peer room.
     * @param streamId Peer to peer room name/id.
     */
    void onLeft(String streamId);

    /**
     * It's called when subscriber count received from server
     * @param streamId Stream Id
     * @param count Count
     */
    void onSubscriberCount(String streamId, int count);

    /**
     * It's called when subscriber list received from server
     * @param streamId Stream Id
     * @param subscribers subscriber array
     */
    void onSubscriberList(String streamId, Subscriber[] subscribers);
}