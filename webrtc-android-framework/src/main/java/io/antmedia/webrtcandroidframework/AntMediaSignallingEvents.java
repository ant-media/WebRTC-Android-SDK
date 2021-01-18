package io.antmedia.webrtcandroidframework;


import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

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

    void onDisconnected();

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
}