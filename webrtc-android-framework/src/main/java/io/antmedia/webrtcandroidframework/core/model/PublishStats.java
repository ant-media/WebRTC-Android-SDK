package io.antmedia.webrtcandroidframework.core.model;

import io.antmedia.webrtcandroidframework.core.StatsCollector;

public class PublishStats {

    //There can be only 1 publish peer connection at a time.
    //So this class has one TrackStats object for video track and audio track. TrackStats object will have trackId null. Because its one and local.

    private long audioBitrate;
    private long lastKnownAudioBytesSent;

    private long videoBitrate;
    private long lastKnownVideoBytesSent;

    private double localAudioLevel;

    private TrackStats videoTrackStats = new TrackStats();
    private TrackStats audioTrackStats = new TrackStats();

    public void PublishStats(){


    }

    public TrackStats getVideoTrackStats() {
        return videoTrackStats;
    }

    public void setVideoTrackStats(TrackStats videoTrackStats) {
        this.videoTrackStats = videoTrackStats;
    }

    public TrackStats getAudioTrackStats() {
        return audioTrackStats;
    }

    public void setAudioTrackStats(TrackStats audioTrackStats) {
        this.audioTrackStats = audioTrackStats;
    }

    public long getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(long audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    public long getLastKnownAudioBytesSent() {
        return lastKnownAudioBytesSent;
    }

    public void setLastKnownAudioBytesSent(long lastKnownAudioBytesSent) {
        this.lastKnownAudioBytesSent = lastKnownAudioBytesSent;
    }

    public long getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(long videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    public long getLastKnownVideoBytesSent() {
        return lastKnownVideoBytesSent;
    }

    public void setLastKnownVideoBytesSent(long lastKnownVideoBytesSent) {
        this.lastKnownVideoBytesSent = lastKnownVideoBytesSent;
    }

    public double getLocalAudioLevel() {
        return localAudioLevel;
    }

    public void setLocalAudioLevel(double localAudioLevel) {
        this.localAudioLevel = localAudioLevel;
    }

    public void reset(){
       audioBitrate = 0;
       videoBitrate = 0;
       lastKnownAudioBytesSent = 0;
       lastKnownVideoBytesSent = 0;
       videoTrackStats = new TrackStats();
       audioTrackStats = new TrackStats();
    }


}
