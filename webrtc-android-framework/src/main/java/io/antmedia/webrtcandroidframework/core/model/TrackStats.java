package io.antmedia.webrtcandroidframework.core.model;

import java.math.BigInteger;

public class TrackStats {

    /**
    *   https://www.w3.org/TR/webrtc-stats/
    */
    boolean isVideoTrackStats = false;
    boolean isAudioTrackStats = false;

    /**
     * The number of the total packets lost
     */
    private int packetsLost = 0;

    /**
     * The number of the total packets received
     */
    private long packetsReceived = 0;
    /**
     /**
     * The instant jitter value
     */
    private double jitter;
    /**
     * The instant round trip time
     */
    private double roundTripTime;
    /**
     * The lost packets & total packets ratio between two successive stats
     */
    private float packetLostRatio;

    private long packetsLostDifference;

    private long firCount;
    private long pliCount;
    private long nackCount;
    private long packetsSent;

    private long framesEncoded;
    private long framesReceived;
    private long framesDropped;
    private long framesDecoded;
    private long framesSent;

    private double totalFreezesDuration;

    private double targetBitrate;

    private double totalPacketSendDelay;

    private BigInteger bytesSent = BigInteger.ZERO;
    private BigInteger bytesReceived = BigInteger.ZERO;
    private BigInteger concealmentEvents = BigInteger.ZERO;
    private long packetsSentPerSecond;
    private BigInteger bytesSentPerSecond = BigInteger.ZERO;
    private long framesEncodedPerSecond;
    private long timeMs;
    private long packetsSentDifference;
    private BigInteger bytesSentDiff = BigInteger.ZERO;
    private long framesEncodedDifference;
    private String trackId;
    private long timeDifference;

    public void setPacketsLost(int packetsLost) {
        packetsLostDifference = packetsLost - this.packetsLost;
        this.packetsLost = packetsLost;
    }

    public void setJitter(double jitter) {
        this.jitter = jitter;
    }

    public void setPacketLostRatio(float packetLostRatio) {
        this.packetLostRatio = packetLostRatio;
    }

    public void setRoundTripTime(double roundTripTime) {
        this.roundTripTime = roundTripTime;
    }


    public void setFirCount(long firCount) {
        this.firCount = firCount;
    }

    public void setPliCount(long pliCount) {
        this.pliCount = pliCount;
    }

    public void setNackCount(long nackCount) {
        this.nackCount = nackCount;
    }

    public void setPacketsSent(long packetsSent) {
        packetsSentDifference = packetsSent - this.packetsSent;
        this.packetsSent = packetsSent;
    }

    public void setBytesSent(BigInteger bytesSent) {
        bytesSentDiff = bytesSent.subtract(this.bytesSent);
        this.bytesSent = bytesSent;
    }

    public BigInteger getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(BigInteger bytesReceived) {
        this.bytesReceived = bytesReceived;
    }


    public void setFramesEncoded(long framesEncoded) {
        framesEncodedDifference = framesEncodedPerSecond - this.framesEncodedPerSecond;
        this.framesEncoded = framesEncoded;
    }

    public void setTimeMs(long timeMs) {
        timeDifference = timeMs - this.timeMs;
        if (timeDifference > 0) {
            packetsSentPerSecond = packetsSentDifference * 1000 / timeDifference;
            bytesSentPerSecond = bytesSentDiff.multiply(BigInteger.valueOf(1000)).divide(BigInteger.valueOf(timeDifference));
            framesEncodedPerSecond = framesEncodedDifference * 1000 / timeDifference;
        }

        if (timeDifference == 0) {
            //sync block may cause unexpected values
            return;
        }
        this.timeMs = timeMs;
    }

    public long getPacketsLost() {
        return packetsLost;
    }

    public double getJitter() {
        return jitter;
    }

    public double getRoundTripTime() {
        return roundTripTime;
    }

    public float getPacketLostRatio() {
        packetLostRatio = (float) 100 * packetsLostDifference / packetsSentDifference;
        return packetLostRatio;
    }

    public BigInteger getBytesSent() {
        return bytesSent;
    }

    public long getPacketsLostDifference() {
        return packetsLostDifference;
    }

    public long getFirCount() {
        return firCount;
    }

    public long getPliCount() {
        return pliCount;
    }

    public long getNackCount() {
        return nackCount;
    }

    public long getPacketsSent() {
        return packetsSent;
    }

    public long getPacketsSentPerSecond() {
        return packetsSentPerSecond;
    }

    public BigInteger getBytesSentPerSecond() {
        return bytesSentPerSecond;
    }

    public long getFramesEncoded() {
        return framesEncoded;
    }

    public long getFramesEncodedPerSecond() {
        return framesEncodedPerSecond;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public long getPacketsSentDifference() {
        return packetsSentDifference;
    }

    public BigInteger getBytesSentDiff() {
        return bytesSentDiff;
    }

    public long getFramesEncodedDifference() {
        return framesEncodedDifference;
    }

    public String getTrackId() {
        return trackId;
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public BigInteger getConcealmentEvents() {
        return concealmentEvents;
    }
    public void setConcealmentEvents(BigInteger concealmentEvents) {
        this.concealmentEvents = concealmentEvents;
    }

    public long getPacketsReceived() {
        return packetsReceived;
    }

    public void setPacketsReceived(long packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public long getFramesReceived() {
        return framesReceived;
    }

    public void setFramesReceived(long framesReceived) {
        this.framesReceived = framesReceived;
    }

    public long getFramesDropped() {
        return framesDropped;
    }

    public void setFramesDropped(long framesDropped) {
        this.framesDropped = framesDropped;
    }

    public long getFramesDecoded() {
        return framesDecoded;
    }

    public void setFramesDecoded(long framesDecoded) {
        this.framesDecoded = framesDecoded;
    }

    public double getTotalFreezesDuration() {
        return totalFreezesDuration;
    }

    public void setTotalFreezesDuration(double totalFreezesDuration) {
        this.totalFreezesDuration = totalFreezesDuration;
    }

    public double getTargetBitrate() {
        return targetBitrate;
    }

    public void setTargetBitrate(double targetBitrate) {
        this.targetBitrate = targetBitrate;
    }

    public double getTotalPacketSendDelay() {
        return totalPacketSendDelay;
    }

    public void setTotalPacketSendDelay(double totalPacketSendDelay) {
        this.totalPacketSendDelay = totalPacketSendDelay;
    }

    public long getFramesSent() {
        return framesSent;
    }

    public void setFramesSent(long framesSent) {
        this.framesSent = framesSent;
    }

    public boolean isVideoTrackStats() {
        return isVideoTrackStats;
    }

    public void setVideoTrackStats(boolean videoTrackStats) {
        isVideoTrackStats = videoTrackStats;
    }

    public boolean isAudioTrackStats() {
        return isAudioTrackStats;
    }

    public void setAudioTrackStats(boolean audioTrackStats) {
        isAudioTrackStats = audioTrackStats;
    }

    @Override
    public String toString() {
        return "TrackStats {" +
                "trackId='" + trackId +
                ", time diff =" + timeDifference +
                ", packetsLost=" + packetsLost +
                ", jitter=" + jitter +
                ", roundTripTime=" + roundTripTime +
                ", packetLostRatio=" + getPacketLostRatio() +
                ", videoPacketsLostDifference=" + packetsLostDifference +
                ", firCount=" + firCount +
                ", pliCount=" + pliCount +
                ", nackCount=" + nackCount +
                ", packetsSent=" + packetsSent +
                ", framesEncoded=" + framesEncoded +
                ", bytesSent=" + bytesSent +
                ", packetsSentPerSecond=" + packetsSentPerSecond +
                ", bytesSentPerSecond=" + bytesSentPerSecond +
                ", framesEncodedPerSecond=" + framesEncodedPerSecond +
                ", timeMs=" + timeMs +
                ", packetsSentDifference=" + packetsSentDifference +
                ", bytesSentDiff=" + bytesSentDiff +
                ", framesEncodedDifference=" + framesEncodedDifference + '\'' +
                '}';
    }
}
