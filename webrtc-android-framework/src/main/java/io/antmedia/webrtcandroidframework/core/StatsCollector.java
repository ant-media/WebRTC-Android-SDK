package io.antmedia.webrtcandroidframework.core;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mostly used to retrieve localAudioLevel or
 * webrtc connection quality parameters such as RTT, Jitter etc.
 */

public class StatsCollector {
    public static final String SSRC = "ssrc";
    public static final String OUTBOUND_RTP = "outbound-rtp";
    private static final String AUDIO = "audio";
    private static final String MEDIA_TYPE = "mediaType";
    private static final String PACKETS_SENT = "packetsSent";
    private static final String BYTES_SENT = "bytesSent";
    private static final String VIDEO = "video";

    public static final String REMOTE_INBOUND_RTP = "remote-inbound-rtp";
    public static final String TRACK_ID = "trackId";

    public static final String TRACK_IDENTIFIER = "trackIdentifier";

    public static final String KIND = "kind";

    public static final String NACK_COUNT = "nackCount";

    public static final String PLI_COUNT = "pliCount";

    public static final String FIR_COUNT = "firCount";

    public static final String FRAME_ENCODED = "framesEncoded";

    public static final String ROUND_TRIP_TIME = "roundTripTime";
    public static final String JITTER = "jitter";
    public static final String PACKETS_LOST = "packetsLost";

    public static final String VIDEO_TRACK_ID = "ARDAMSv";

    public static final String AUDIO_TRACK_ID = "ARDAMSa";

    private double lastKnownStatsTimeStampMs;

    private long lastKnownAudioBytesSent;

    private long lastKnownVideoBytesSent;
    private long localAudioBitrate;
    private long localVideoBitrate;
    private double localAudioLevel;


    private final Map<Long, TrackStats> videoTrackStatsMap = new ConcurrentHashMap<>();
    private final Map<Long, TrackStats> audioTrackStatsMap = new ConcurrentHashMap<>();


    public void onStatsReport(RTCStatsReport report) {
        parseStats(report);
    }

    private void parseStats(RTCStatsReport report) {
        Map<String, RTCStats> statsMap = report.getStatsMap();
        double timeMs = 0;
        for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
            RTCStats value = entry.getValue();
            timeMs = value.getTimestampUs()/1000;

            if (OUTBOUND_RTP.equals(value.getType())) {
                long timeDiffSeconds = (long) ((timeMs - lastKnownStatsTimeStampMs) / 1000); // convert it to seconds
                timeDiffSeconds = timeDiffSeconds == 0 ? 1 : timeDiffSeconds; // avoid division by zero

                if (AUDIO.equals(value.getMembers().get(MEDIA_TYPE))) {
                    long ssrc = (long) value.getMembers().get(SSRC);
                    TrackStats audioTrackStat = audioTrackStatsMap.get(ssrc);
                    if(audioTrackStat == null) {
                        audioTrackStat = new TrackStats();
                        audioTrackStatsMap.put(ssrc, audioTrackStat);
                    }

                    long packetsSent = (long)value.getMembers().get(PACKETS_SENT);
                    audioTrackStat.setPacketsSent(packetsSent);

                    BigInteger bytesSent = ((BigInteger) value.getMembers().get(BYTES_SENT));
                    audioTrackStat.setBytesSent(bytesSent);
                    audioTrackStat.setTimeMs((long)timeMs);

                    String trackSenderId = (String) value.getMembers().get(TRACK_ID);
                    String trackId = (String) statsMap.get(trackSenderId).getMembers().get(TRACK_IDENTIFIER);
                    trackId = trackId.replace(AUDIO_TRACK_ID, "");
                    audioTrackStat.setTrackId(trackId);

                    localAudioBitrate = (bytesSent.longValue() - lastKnownAudioBytesSent) / timeDiffSeconds * 8;
                    lastKnownAudioBytesSent = bytesSent.longValue();

                } else if (VIDEO.equals(value.getMembers().get(MEDIA_TYPE))) {
                    if(value.getMembers().containsKey(SSRC)){
                        long ssrc = (long) value.getMembers().get(SSRC);
                        TrackStats videoTrackStat = videoTrackStatsMap.get(ssrc);
                        if(videoTrackStat == null) {
                            videoTrackStat = new TrackStats();
                            videoTrackStatsMap.put(ssrc, videoTrackStat);
                        }
                        if (value.getMembers().containsKey(FIR_COUNT)) {
                            long firCount = (long) value.getMembers().get(FIR_COUNT);
                            videoTrackStat.setFirCount(firCount);
                        }

                        if (value.getMembers().containsKey(PLI_COUNT)) {
                            long pliCount = (long) value.getMembers().get(PLI_COUNT);
                            videoTrackStat.setPliCount(pliCount);
                        }

                        if (value.getMembers().containsKey(NACK_COUNT)) {
                            long nackCount = (long) value.getMembers().get(NACK_COUNT);
                            videoTrackStat.setNackCount(nackCount);
                        }

                        if (value.getMembers().containsKey(PACKETS_SENT)) {
                            long packetsSent = (long) value.getMembers().get(PACKETS_SENT);
                            videoTrackStat.setPacketsSent(packetsSent);
                        }

                        if (value.getMembers().containsKey(BYTES_SENT)) {
                            BigInteger bytesSent = (BigInteger) value.getMembers().get(BYTES_SENT);
                            videoTrackStat.setBytesSent(bytesSent);

                            localVideoBitrate = (bytesSent.longValue() - lastKnownVideoBytesSent) / timeDiffSeconds * 8;
                            lastKnownVideoBytesSent = bytesSent.bitCount();
                        }

                        if (value.getMembers().containsKey(FRAME_ENCODED)) {
                            long framesEncoded = (long) value.getMembers().get(FRAME_ENCODED);
                            videoTrackStat.setFramesEncoded(framesEncoded);
                        }

                        videoTrackStat.setTimeMs((long)timeMs);

                        String trackSenderId = (String) value.getMembers().get(TRACK_ID);
                        String trackId = (String) statsMap.get(trackSenderId).getMembers().get(TRACK_IDENTIFIER);
                        trackId = trackId.replace(VIDEO_TRACK_ID, "");
                        videoTrackStat.setTrackId(trackId);
                    }

                }

            } else if (REMOTE_INBOUND_RTP.equals(value.getType()) ) {
                if (VIDEO.equals(value.getMembers().get(KIND))) {

                    if(value.getMembers().containsKey(SSRC)) {
                        long ssrc = (long) value.getMembers().get(SSRC);
                        TrackStats videoTrackStat = videoTrackStatsMap.get(ssrc);
                        if(videoTrackStat == null) {
                            videoTrackStat = new TrackStats();
                            videoTrackStatsMap.put(ssrc, videoTrackStat);
                        }

                        if(value.getMembers().containsKey(PACKETS_LOST)){
                            long packetsLost = (int)value.getMembers().get(PACKETS_LOST);
                            videoTrackStat.setPacketsLost(packetsLost);
                        }

                        if(value.getMembers().containsKey(JITTER)) {
                            double jitter = (double)value.getMembers().get(JITTER);
                            videoTrackStat.setJitter(jitter);

                        }

                        if(value.getMembers().containsKey(ROUND_TRIP_TIME)) {
                            double roundTripTime = (double)value.getMembers().get(ROUND_TRIP_TIME);
                            videoTrackStat.setRoundTripTime(roundTripTime);
                        }
                    }
                } else if (AUDIO.equals(value.getMembers().get(KIND))) {
                    if(value.getMembers().containsKey(SSRC)){
                        long ssrc = (long) value.getMembers().get(SSRC);
                        TrackStats audioTrackStat = audioTrackStatsMap.get(ssrc);
                        if(audioTrackStat == null) {
                            audioTrackStat = new TrackStats();
                            audioTrackStatsMap.put(ssrc, audioTrackStat);
                        }

                        if(value.getMembers().containsKey(PACKETS_LOST)){
                            long packetsLost = (int)value.getMembers().get(PACKETS_LOST);
                            audioTrackStat.setPacketsLost(packetsLost);
                        }

                        if(value.getMembers().containsKey(JITTER)) {
                            double jitter = (double)value.getMembers().get(JITTER);
                            audioTrackStat.setJitter(jitter);

                        }

                        if(value.getMembers().containsKey(ROUND_TRIP_TIME)) {
                            double roundTripTime = (double)value.getMembers().get(ROUND_TRIP_TIME);
                            audioTrackStat.setRoundTripTime(roundTripTime);
                        }
                    }
                }
            }else if("media-source".equals(value.getType())){
                Map<String,Object> members =  value.getMembers();
                if(members.containsKey("audioLevel")){
                    localAudioLevel = (double) members.get("audioLevel");
                }
            }
        }
        lastKnownStatsTimeStampMs = timeMs;
    }


    public static class TrackStats {
        /**
         * The number of the total packets lost
         */
        private long packetsLost = 0;
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

        long firCount;
        long pliCount;
        long nackCount;
        long packetsSent;
        private long framesEncoded;
        BigInteger bytesSent = BigInteger.ZERO;
        private long packetsSentPerSecond;
        private BigInteger bytesSentPerSecond = BigInteger.ZERO;
        private long framesEncodedPerSecond;
        private long timeMs;
        private long packetsSentDifference;
        private BigInteger bytesSentDiff = BigInteger.ZERO;
        private long framesEncodedDifference;
        private String trackId;
        private long timeDifference;

        public void setPacketsLost(long packetsLost) {
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

        @Override
        public String toString() {
            return "VideoTrackStats{" +
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

    public double getLocalAudioLevel(){
        return localAudioLevel;
    }

    public long getLocalAudioBitrate(){
        return localAudioBitrate;
    }

    public long getLocalVideoBitrate(){
        return localVideoBitrate;
    }

    public long getLastKnownAudioBytesSent() {
        return lastKnownAudioBytesSent;
    }

    public long getLastKnownVideoBytesSent() {
        return lastKnownVideoBytesSent;
    }

    public Map<Long, TrackStats> getVideoTrackStatsMap(){
        return videoTrackStatsMap;
    }

    public Map<Long, TrackStats> getAudioTrackStatsMap(){
        return audioTrackStatsMap;
    }
}