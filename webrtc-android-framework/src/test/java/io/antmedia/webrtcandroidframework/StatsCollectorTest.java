package io.antmedia.webrtcandroidframework;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import io.antmedia.webrtcandroidframework.core.StatsCollector;
import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.PublishStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

import static org.junit.Assert.assertEquals;

public class StatsCollectorTest {

    @Mock
    private RTCStatsReport report;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnStatsReportPublish(){

        StatsCollector statsCollector = new StatsCollector();
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);

        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn(StatsCollector.OUTBOUND_RTP);
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.AUDIO, StatsCollector.OUTBOUND_RTP));
        statsMap.put("", rtcStats);

        statsCollector.onStatsReport(report);

        PublishStats publishStats = statsCollector.getPublishStats();

        TrackStats audioTrackStats = publishStats.getAudioTrackStats();
        TrackStats videoTrackStats = publishStats.getVideoTrackStats();

        assertEquals(10, audioTrackStats.getPacketsSent());
        assertEquals(BigInteger.valueOf(1000), audioTrackStats.getBytesSent());
        assertEquals(1000.0, audioTrackStats.getTargetBitrate(), 0);
        assertEquals(100.0, audioTrackStats.getTotalPacketSendDelay(), 0);
        assertEquals(publishStats.getLastKnownAudioBytesSent(), audioTrackStats.getBytesSent().longValue());
        assertEquals(publishStats.getAudioBitrate(), 0);


        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.VIDEO, StatsCollector.OUTBOUND_RTP));

        statsCollector.onStatsReport(report);

        assertEquals(10, videoTrackStats.getPacketsSent());
        assertEquals(BigInteger.valueOf(1000), videoTrackStats.getBytesSent());
        assertEquals(1000L, videoTrackStats.getFirCount());
        assertEquals(1000L, videoTrackStats.getPliCount());
        assertEquals(1000L, videoTrackStats.getNackCount());
        assertEquals(1000L, videoTrackStats.getFramesEncoded());
        assertEquals(1000L, videoTrackStats.getFramesSent());
        assertEquals(1000.0, videoTrackStats.getTargetBitrate(), 0);
        assertEquals(0.3, videoTrackStats.getTotalPacketSendDelay(), 0);

        when(rtcStats.getType()).thenReturn(StatsCollector.REMOTE_INBOUND_RTP);

        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.AUDIO, StatsCollector.REMOTE_INBOUND_RTP));

        statsCollector.onStatsReport(report);

        assertEquals(10, audioTrackStats.getPacketsLost());
        assertEquals(10.0, audioTrackStats.getJitter(),0);
        assertEquals(0.3, audioTrackStats.getRoundTripTime(),0);

        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.VIDEO, StatsCollector.REMOTE_INBOUND_RTP));
        statsCollector.onStatsReport(report);

        assertEquals(10, videoTrackStats.getPacketsLost());
        assertEquals(10.0, videoTrackStats.getJitter(),0);
        assertEquals(0.3, videoTrackStats.getRoundTripTime(),0);


        //publish stats so trackId is null.
        assertNull(videoTrackStats.getTrackId());
        assertNull(audioTrackStats.getTrackId());


    }

    @Test
    public void testOnStatsReportPlay(){

        StatsCollector statsCollector = new StatsCollector();
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);

        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn(StatsCollector.INBOUND_RTP);
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.AUDIO, StatsCollector.INBOUND_RTP));
        statsMap.put("", rtcStats);
        statsCollector.onStatsReport(report);

        PlayStats playStats = statsCollector.getPlayStats();

        Map<String, TrackStats> audioTrackStatsMap = playStats.getAudioTrackStatsMap();


        TrackStats audioTrackStats = audioTrackStatsMap.get("audioTrack1");
        assertNotNull(audioTrackStats);

        assertEquals(10, audioTrackStats.getPacketsLost());
        assertEquals(0.3, audioTrackStats.getJitter(),0);
        assertEquals(0.3, audioTrackStats.getRoundTripTime(),0);
        assertEquals(BigInteger.valueOf(3), audioTrackStats.getConcealmentEvents());

        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.VIDEO, StatsCollector.INBOUND_RTP));

        statsCollector.onStatsReport(report);

        Map<String, TrackStats> videoTrackStatsMap = playStats.getVideoTrackStatsMap();
        TrackStats videoTrackStats = videoTrackStatsMap.get("videoTrack1");
        assertNotNull(videoTrackStats);


        assertEquals(1000L, videoTrackStats.getFirCount());
        assertEquals(1000L, videoTrackStats.getPliCount());
        assertEquals(1000L, videoTrackStats.getNackCount());
        assertEquals(0.3, videoTrackStats.getJitter(), 0);
        assertEquals(10, videoTrackStats.getPacketsLost());
        assertEquals(10, videoTrackStats.getPacketsReceived());
        assertEquals(BigInteger.valueOf(1000), videoTrackStats.getBytesReceived());
        assertEquals(1000L, videoTrackStats.getFramesEncoded());
        assertEquals(1000L, videoTrackStats.getFramesDecoded());
        assertEquals(1000L, videoTrackStats.getFramesReceived());
        assertEquals(1000L, videoTrackStats.getFramesDropped());
        assertEquals(0.3, videoTrackStats.getTotalFreezesDuration(), 0);


    }

    @Test
    public void testLocalAudioLevel() {
        StatsCollector statsCollector = new StatsCollector();
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("", rtcStats);
        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn(StatsCollector.MEDIA_SOURCE);
        when(rtcStats.getMembers()).thenReturn(createMembersMap(StatsCollector.AUDIO, StatsCollector.MEDIA_SOURCE));
        statsCollector.onStatsReport(report);
        assertEquals(100.0, statsCollector.getLocalAudioLevel(), 0.0);

        PublishStats publishStats = statsCollector.getPublishStats();
        double localAudioLevel = publishStats.getLocalAudioLevel();
        assertEquals(100.0, localAudioLevel, 0.0);
    }

    // Helper method to create a members map
    private Map<String, Object> createMembersMap(String mediaType, String statType) {
        Map<String, Object> membersMap = new HashMap<>();
        membersMap.put(StatsCollector.KIND, mediaType);
        if(StatsCollector.OUTBOUND_RTP.equals(statType)){
            membersMap.put(StatsCollector.SSRC, 1000L);

            if(StatsCollector.AUDIO.equals(mediaType)){

                membersMap.put(StatsCollector.PACKETS_SENT, BigInteger.valueOf(10));
                membersMap.put(StatsCollector.BYTES_SENT, new BigInteger("1000"));
                membersMap.put(StatsCollector.TARGET_BITRATE, 1000.0);
                membersMap.put(StatsCollector.TOTAL_PACKET_SEND_DELAY, 100.0);

            }else if(StatsCollector.VIDEO.equals(mediaType)){

                membersMap.put(StatsCollector.PACKETS_SENT, BigInteger.valueOf(10));
                membersMap.put(StatsCollector.FIR_COUNT, 1000L);
                membersMap.put(StatsCollector.PLI_COUNT, 1000L);
                membersMap.put(StatsCollector.NACK_COUNT, 1000L);
                membersMap.put(StatsCollector.BYTES_SENT, new BigInteger("1000"));
                membersMap.put(StatsCollector.FRAMES_ENCODED, 1000L);
                membersMap.put(StatsCollector.FRAMES_SENT, 1000L);
                membersMap.put(StatsCollector.TARGET_BITRATE, 1000.0);
                membersMap.put(StatsCollector.TOTAL_PACKET_SEND_DELAY, 0.3);

            }

        }else if(StatsCollector.REMOTE_INBOUND_RTP.equals(statType)){
            membersMap.put(StatsCollector.SSRC, 1000L);

            if(StatsCollector.AUDIO.equals(mediaType)){

                membersMap.put(StatsCollector.PACKETS_LOST, 10);
                membersMap.put(StatsCollector.JITTER, 10.0);
                membersMap.put(StatsCollector.ROUND_TRIP_TIME, 0.3);


            }else if(StatsCollector.VIDEO.equals(mediaType)){

                membersMap.put(StatsCollector.PACKETS_LOST, 10);
                membersMap.put(StatsCollector.JITTER, 10.0);
                membersMap.put(StatsCollector.ROUND_TRIP_TIME, 0.3);

            }

        }else if(StatsCollector.INBOUND_RTP.equals(statType)){

            membersMap.put(StatsCollector.SSRC, 1000L);

            if(StatsCollector.AUDIO.equals(mediaType)){

                membersMap.put(StatsCollector.JITTER, 0.3);
                membersMap.put(StatsCollector.PACKETS_LOST, 10);
                membersMap.put(StatsCollector.ROUND_TRIP_TIME, 0.3);
                membersMap.put(StatsCollector.CONCEALMENT_EVENTS, new BigInteger("3"));
                membersMap.put(StatsCollector.TRACK_IDENTIFIER, "ARDAMSaaudioTrack1");

            }else if(StatsCollector.VIDEO.equals(mediaType)){
                membersMap.put(StatsCollector.JITTER, 0.3);
                membersMap.put(StatsCollector.PACKETS_LOST, 10);
                membersMap.put(StatsCollector.PACKETS_RECEIVED, 10L);
                membersMap.put(StatsCollector.BYTES_RECEIVED, BigInteger.valueOf(1000));
                membersMap.put(StatsCollector.FRAMES_ENCODED, 1000L);
                membersMap.put(StatsCollector.FRAMES_DECODED, 1000L);
                membersMap.put(StatsCollector.FRAMES_RECEIVED, 1000L);
                membersMap.put(StatsCollector.FRAMES_DROPPED, 1000L);
                membersMap.put(StatsCollector.TOTAL_FREEZES_DURATION, 0.3);

                membersMap.put(StatsCollector.FIR_COUNT, 1000L);
                membersMap.put(StatsCollector.PLI_COUNT, 1000L);
                membersMap.put(StatsCollector.NACK_COUNT, 1000L);
                membersMap.put(StatsCollector.TRACK_IDENTIFIER, "ARDAMSvvideoTrack1");
            }
        }else if(StatsCollector.MEDIA_SOURCE.equals(statType)){
            membersMap.put(StatsCollector.AUDIO_LEVEL, 100.0);
            membersMap.put(StatsCollector.TRACK_IDENTIFIER, "ARDAMSaaudioTrack1");
        }
        return membersMap;
    }
}
