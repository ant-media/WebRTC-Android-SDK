package io.antmedia.webrtcandroidframework.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PlayStats {

    //There can be more than 1 tracks playing at a time. Multi track playing(also conference)
    //So this class holds a map for video tracks and audio tracks.

    //trackId -> TrackStats map for play peers.
    private Map<String, TrackStats> videoTrackStatsMap = new ConcurrentHashMap<>();
    private Map<String, TrackStats> audioTrackStatsMap = new ConcurrentHashMap<>();


    public void PlayStats(){




    }

    public Map<String, TrackStats> getVideoTrackStatsMap() {
        return videoTrackStatsMap;
    }

    public void setVideoTrackStatsMap(Map<String, TrackStats> videoTrackStatsMap) {
        this.videoTrackStatsMap = videoTrackStatsMap;
    }

    public Map<String, TrackStats> getAudioTrackStatsMap() {
        return audioTrackStatsMap;
    }

    public void setAudioTrackStatsMap(Map<String, TrackStats> audioTrackStatsMap) {
        this.audioTrackStatsMap = audioTrackStatsMap;
    }

    public void reset(){
        audioTrackStatsMap.clear();
        videoTrackStatsMap.clear();
    }


}
