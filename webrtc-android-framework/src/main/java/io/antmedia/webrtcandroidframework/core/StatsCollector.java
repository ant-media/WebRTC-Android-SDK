package io.antmedia.webrtcandroidframework.core;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.Map;

import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.PublishStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

/**
 * Mostly used to retrieve localAudioLevel or
 * webrtc connection quality parameters such as RTT, Jitter etc.
 * For all description of different quality parameters hold by this class see
 * https://www.w3.org/TR/webrtc-stats/
 */

public class StatsCollector {
    public static final String SSRC = "ssrc";
    public static final String OUTBOUND_RTP = "outbound-rtp";
    public static final String INBOUND_RTP = "inbound-rtp";
    public static final String MEDIA_SOURCE = "media-source";
    public static final String AUDIO = "audio";
    public static final String MEDIA_TYPE = "mediaType";
    public static final String PACKETS_SENT = "packetsSent";
    public static final String BYTES_SENT = "bytesSent";
    public static final String BYTES_RECEIVED = "bytesReceived";
    public static final String CONCEALMENT_EVENTS ="concealmentEvents";
    public static final String VIDEO = "video";

    public static final String REMOTE_INBOUND_RTP = "remote-inbound-rtp";
    public static final String TRACK_ID = "trackId";

    public static final String TRACK_IDENTIFIER = "trackIdentifier";

    public static final String KIND = "kind";

    public static final String NACK_COUNT = "nackCount";

    public static final String PLI_COUNT = "pliCount";

    public static final String FIR_COUNT = "firCount";

    public static final String FRAMES_ENCODED = "framesEncoded";
    public static final String FRAMES_DECODED = "framesDecoded";

    public static final String FRAMES_RECEIVED = "framesReceived";
    public static final String FRAMES_DROPPED = "framesDropped";

    public static final String FRAMES_SENT = "framesSent";


    public static final String TOTAL_FREEZES_DURATION = "totalFreezesDuration";

    public static final String TARGET_BITRATE = "targetBitrate";
    public static final String TOTAL_PACKET_SEND_DELAY = "totalPacketSendDelay";


    public static final String ROUND_TRIP_TIME = "roundTripTime";
    public static final String JITTER = "jitter";
    public static final String PACKETS_LOST = "packetsLost";
    public static final String PACKETS_RECEIVED = "packetsReceived";

    public static final String VIDEO_TRACK_ID = "ARDAMSv";

    public static final String AUDIO_TRACK_ID = "ARDAMSa";

    public static final String AUDIO_LEVEL = "audioLevel";

    private double lastKnownStatsTimeStampMs;

    private double localAudioLevel;

    private PublishStats publishStats = new PublishStats();
    private PlayStats playStats = new PlayStats();


    public void onStatsReport(RTCStatsReport report) {
        parseStats(report);
    }

    private void parseStats(RTCStatsReport report) {
        Map<String, RTCStats> statsMap = report.getStatsMap();
        double timeMs = 0;
        for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
            RTCStats value = entry.getValue();
            timeMs = value.getTimestampUs()/1000;

            if (OUTBOUND_RTP.equals(value.getType())) { //Exiting data from local client.(Part of publish statistics)
                long timeDiffSeconds = (long) ((timeMs - lastKnownStatsTimeStampMs) / 1000); // convert it to seconds
                timeDiffSeconds = timeDiffSeconds == 0 ? 1 : timeDiffSeconds; // avoid division by zero

                if (AUDIO.equals(value.getMembers().get(KIND))) {
                    if(value.getMembers().containsKey(SSRC)) {
                        TrackStats audioTrackStats = publishStats.getAudioTrackStats();

                        long packetsSent = ((BigInteger) value.getMembers().get(PACKETS_SENT)).longValue();
                        audioTrackStats.setPacketsSent(packetsSent);

                        BigInteger bytesSent = ((BigInteger) value.getMembers().get(BYTES_SENT));
                        audioTrackStats.setBytesSent(bytesSent);
                        audioTrackStats.setTimeMs((long)timeMs);


                        if (value.getMembers().containsKey(TARGET_BITRATE)) {
                            double targetBitrate = (double) value.getMembers().get(TARGET_BITRATE);
                            audioTrackStats.setTargetBitrate(targetBitrate);
                        }

                        if (value.getMembers().containsKey(TOTAL_PACKET_SEND_DELAY)) {
                            double totalPacketSendDelay = (double) value.getMembers().get(TOTAL_PACKET_SEND_DELAY);
                            audioTrackStats.setTotalPacketSendDelay(totalPacketSendDelay);
                        }


                        publishStats.setLastKnownAudioBytesSent(bytesSent.longValue());
                        publishStats.setAudioBitrate((bytesSent.longValue() - publishStats.getLastKnownAudioBytesSent()) / timeDiffSeconds * 8);


                    }

                } else if (VIDEO.equals(value.getMembers().get(KIND))) {
                    if(value.getMembers().containsKey(SSRC)){
                        TrackStats videoTrackStats = publishStats.getVideoTrackStats();

                        if (value.getMembers().containsKey(FIR_COUNT)) {
                            long firCount = (long) value.getMembers().get(FIR_COUNT);
                            videoTrackStats.setFirCount(firCount);
                        }

                        if (value.getMembers().containsKey(PLI_COUNT)) {
                            long pliCount = (long) value.getMembers().get(PLI_COUNT);
                            videoTrackStats.setPliCount(pliCount);
                        }

                        if (value.getMembers().containsKey(NACK_COUNT)) {
                            long nackCount = (long) value.getMembers().get(NACK_COUNT);
                            videoTrackStats.setNackCount(nackCount);
                        }

                        if (value.getMembers().containsKey(PACKETS_SENT)) {
                            long packetsSent = ((BigInteger) value.getMembers().get(PACKETS_SENT)).longValue();
                            videoTrackStats.setPacketsSent(packetsSent);
                        }

                        if (value.getMembers().containsKey(BYTES_SENT)) {
                            BigInteger bytesSent = (BigInteger) value.getMembers().get(BYTES_SENT);
                            videoTrackStats.setBytesSent(bytesSent);
                            publishStats.setLastKnownVideoBytesSent(bytesSent.longValue());
                            publishStats.setVideoBitrate((bytesSent.longValue() - publishStats.getLastKnownVideoBytesSent()) / timeDiffSeconds * 8);
                        }

                        if (value.getMembers().containsKey(FRAMES_ENCODED)) {
                            long framesEncoded = (long) value.getMembers().get(FRAMES_ENCODED);
                            videoTrackStats.setFramesEncoded(framesEncoded);
                        }

                        if (value.getMembers().containsKey(FRAMES_SENT)) {
                            long framesSent = (long) value.getMembers().get(FRAMES_SENT);
                            videoTrackStats.setFramesSent(framesSent);
                        }

                        if (value.getMembers().containsKey(TARGET_BITRATE)) {
                            double targetBitrate = (double) value.getMembers().get(TARGET_BITRATE);
                            videoTrackStats.setTargetBitrate(targetBitrate);
                        }

                        if (value.getMembers().containsKey(TOTAL_PACKET_SEND_DELAY)) {
                            double totalPacketSendDelay = (double) value.getMembers().get(TOTAL_PACKET_SEND_DELAY);
                            videoTrackStats.setTotalPacketSendDelay(totalPacketSendDelay);
                        }

                        videoTrackStats.setTimeMs((long)timeMs);

                    }

                }

            } else if (REMOTE_INBOUND_RTP.equals(value.getType()) ) { //Exiting data from local client, received by remote peer statistics.(Part of publish)
                if (VIDEO.equals(value.getMembers().get(KIND))) {

                    if(value.getMembers().containsKey(SSRC)) {
                        TrackStats videoTrackStats = getPublishStats().getVideoTrackStats();

                        if(value.getMembers().containsKey(PACKETS_LOST)){
                            int packetsLost = (int)value.getMembers().get(PACKETS_LOST);
                            videoTrackStats.setPacketsLost(packetsLost);
                        }

                        if(value.getMembers().containsKey(JITTER)) {
                            double jitter = (double)value.getMembers().get(JITTER);
                            videoTrackStats.setJitter(jitter);

                        }

                        if(value.getMembers().containsKey(ROUND_TRIP_TIME)) {
                            double roundTripTime = (double)value.getMembers().get(ROUND_TRIP_TIME);
                            videoTrackStats.setRoundTripTime(roundTripTime);
                        }
                    }
                } else if (AUDIO.equals(value.getMembers().get(KIND))) {
                    if(value.getMembers().containsKey(SSRC)){
                        TrackStats audioTrackStats = publishStats.getAudioTrackStats();

                        if(value.getMembers().containsKey(PACKETS_LOST)){
                            int packetsLost = (int)value.getMembers().get(PACKETS_LOST);
                            audioTrackStats.setPacketsLost(packetsLost);
                        }

                        if(value.getMembers().containsKey(JITTER)) {
                            double jitter = (double)value.getMembers().get(JITTER);
                            audioTrackStats.setJitter(jitter);

                        }

                        if(value.getMembers().containsKey(ROUND_TRIP_TIME)) {
                            double roundTripTime = (double)value.getMembers().get(ROUND_TRIP_TIME);
                            audioTrackStats.setRoundTripTime(roundTripTime);
                        }
                    }
                }
            }else if(INBOUND_RTP.equals(value.getType())){ //Incoming data from peers.(Play statistics)

                if (VIDEO.equals(value.getMembers().get(KIND))) {

                    if(value.getMembers().containsKey(SSRC)) {

                        TrackStats videoTrackStats = new TrackStats();
                        videoTrackStats.setVideoTrackStats(true);

                        if (value.getMembers().containsKey(FIR_COUNT)) {
                            long firCount = (long) value.getMembers().get(FIR_COUNT);
                            videoTrackStats.setFirCount(firCount);
                        }

                        if (value.getMembers().containsKey(PLI_COUNT)) {
                            long pliCount = (long) value.getMembers().get(PLI_COUNT);
                            videoTrackStats.setPliCount(pliCount);
                        }

                        if (value.getMembers().containsKey(NACK_COUNT)) {
                            long nackCount = (long) value.getMembers().get(NACK_COUNT);
                            videoTrackStats.setNackCount(nackCount);
                        }

                        if (value.getMembers().containsKey(JITTER)) {
                            double jitter = (double) value.getMembers().get(JITTER);
                            videoTrackStats.setJitter(jitter);
                        }

                        if (value.getMembers().containsKey(PACKETS_LOST)) {
                            int packetsLost = (int) value.getMembers().get(PACKETS_LOST);
                            videoTrackStats.setPacketsLost(packetsLost);
                        }

                        if (value.getMembers().containsKey(PACKETS_RECEIVED)) {
                            long packetsReceived = (long) value.getMembers().get(PACKETS_RECEIVED);
                            videoTrackStats.setPacketsReceived(packetsReceived);
                        }

                        if (value.getMembers().containsKey(BYTES_RECEIVED)) {
                            BigInteger bytesReceived = (BigInteger) value.getMembers().get(BYTES_RECEIVED);
                            videoTrackStats.setBytesReceived(bytesReceived);

                        }

                        if (value.getMembers().containsKey(FRAMES_ENCODED)) {
                            long framesEncoded = (long) value.getMembers().get(FRAMES_ENCODED);
                            videoTrackStats.setFramesEncoded(framesEncoded);
                        }

                        if (value.getMembers().containsKey(FRAMES_DECODED)) {
                            long framesDecoded = (long) value.getMembers().get(FRAMES_DECODED);
                            videoTrackStats.setFramesDecoded(framesDecoded);
                        }

                        if (value.getMembers().containsKey(FRAMES_RECEIVED)) {
                            Number framesReceivedNumber = (Number) value.getMembers().get(FRAMES_RECEIVED);
                            long framesReceived = framesReceivedNumber.longValue();

                            videoTrackStats.setFramesReceived(framesReceived);
                        }

                        if (value.getMembers().containsKey(FRAMES_DROPPED)) {
                            Number framesDroppedNumber = (Number) value.getMembers().get(FRAMES_DROPPED);
                            long framesDropped = framesDroppedNumber.longValue();

                            videoTrackStats.setFramesDropped(framesDropped);
                        }

                        if (value.getMembers().containsKey(TOTAL_FREEZES_DURATION)) {
                            double totalFreezesDuration = (double) value.getMembers().get(TOTAL_FREEZES_DURATION);
                            videoTrackStats.setTotalFreezesDuration(totalFreezesDuration);
                        }

                        videoTrackStats.setTimeMs((long)timeMs);

                        if (value.getMembers().containsKey(TRACK_IDENTIFIER)) { // must have track identifier.
                            String trackIdentifier = (String) value.getMembers().get(TRACK_IDENTIFIER);
                            trackIdentifier = trackIdentifier.substring(VIDEO_TRACK_ID.length());
                            videoTrackStats.setTrackId(trackIdentifier);
                            playStats.getVideoTrackStatsMap().put(trackIdentifier, videoTrackStats);
                        } else if (value.getMembers().containsKey(TRACK_ID)) {
                            String trackId = (String) value.getMembers().get(TRACK_ID);
                            RTCStats track = report.getStatsMap().get(trackId);
                            String trackIdentifier = (String) track.getMembers().get(TRACK_IDENTIFIER);
                            trackIdentifier = trackIdentifier.substring(VIDEO_TRACK_ID.length());
                            videoTrackStats.setTrackId(trackIdentifier);
                            playStats.getVideoTrackStatsMap().put(trackIdentifier, videoTrackStats);
                        }
                    }
                } else if (AUDIO.equals(value.getMembers().get(KIND))) {
                    if(value.getMembers().containsKey(SSRC)){
                        TrackStats audioTrackStat = new TrackStats();
                        audioTrackStat.setAudioTrackStats(true);

                        if(value.getMembers().containsKey(PACKETS_LOST)){
                            int packetsLost = (int)value.getMembers().get(PACKETS_LOST);
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

                        if(value.getMembers().containsKey(CONCEALMENT_EVENTS)) {
                            BigInteger concealmentEvents = ((BigInteger) value.getMembers().get(CONCEALMENT_EVENTS));
                            audioTrackStat.setConcealmentEvents(concealmentEvents);
                        }

                        if (value.getMembers().containsKey(TRACK_IDENTIFIER)) { // must have track identifier.
                            String trackIdentifier = (String) value.getMembers().get(TRACK_IDENTIFIER);
                            trackIdentifier = trackIdentifier.substring(AUDIO_TRACK_ID.length());
                            audioTrackStat.setTrackId(trackIdentifier);
                            playStats.getAudioTrackStatsMap().put(trackIdentifier, audioTrackStat);
                        }
                        else if (value.getMembers().containsKey(TRACK_ID)) {
                            String trackId = (String) value.getMembers().get(TRACK_ID);
                            RTCStats track = report.getStatsMap().get(trackId);
                            String trackIdentifier = (String) track.getMembers().get(TRACK_IDENTIFIER);
                            trackIdentifier = trackIdentifier.substring(AUDIO_TRACK_ID.length());
                            audioTrackStat.setTrackId(trackIdentifier);
                            playStats.getAudioTrackStatsMap().put(trackIdentifier, audioTrackStat);
                        }
                    }
                }

            }else if(MEDIA_SOURCE.equals(value.getType())){
                Map<String,Object> members =  value.getMembers();
                if(members.containsKey(AUDIO_LEVEL)){
                    //backwards comp
                    localAudioLevel = (double) members.get(AUDIO_LEVEL);

                    publishStats.setLocalAudioLevel((double) members.get(AUDIO_LEVEL));
                }
            }
        }
        lastKnownStatsTimeStampMs = timeMs;
    }


    public double getLocalAudioLevel(){
        return localAudioLevel;
    }

    public PlayStats getPlayStats() {
        return playStats;
    }

    public PublishStats getPublishStats() {
        return publishStats;
    }

}