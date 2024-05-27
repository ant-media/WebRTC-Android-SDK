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
import static org.mockito.Mockito.*;
import io.antmedia.webrtcandroidframework.core.StatsCollector;
import static org.junit.Assert.assertEquals;

public class StatsCollectorTest {

    @Mock
    private RTCStatsReport report;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnStatsReportAudio() {
        // Create a StatsCollector instance
        StatsCollector statsCollector = new StatsCollector();
        // Mock the RTCStatsReport
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("ARDAMSastream1", rtcStats);

        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn(StatsCollector.OUTBOUND_RTP);
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap("audio"));

        // Call the onStatsReport method with the publish mode
        statsCollector.onStatsReport(report);

        StatsCollector.TrackStats audioTrackStat1 = statsCollector.getAudioTrackStatsMap().get(64L);

        assertEquals(1000, audioTrackStat1.getBytesSent().intValue());
        assertEquals(1000, audioTrackStat1.getBytesSentDiff().intValue());
        assertEquals(10, audioTrackStat1.getPacketsSentDifference());
        assertEquals(0, audioTrackStat1.getFramesEncodedPerSecond());


        assertEquals(10, audioTrackStat1.getPacketsSent());
        assertEquals(1.0, audioTrackStat1.getTimeMs(), 0.0);
        assertEquals("stream1", audioTrackStat1.getTrackId());

        assertEquals(8000L, statsCollector.getLocalAudioBitrate());
        assertEquals(1000L, statsCollector.getLastKnownAudioBytesSent());


        when(rtcStats.getType()).thenReturn(StatsCollector.REMOTE_INBOUND_RTP);

        statsCollector.onStatsReport(report);

        StatsCollector.TrackStats audioTrackStat2 = statsCollector.getAudioTrackStatsMap().get(64L);

        assertEquals(10.0, audioTrackStat2.getRoundTripTime(), 0.0);
        assertEquals(10.0, audioTrackStat2.getJitter(), 0.0);
        assertEquals(10, audioTrackStat2.getPacketsLost());
        assertEquals(10, audioTrackStat2.getPacketsLostDifference());

    }

    @Test
    public void testOnStatsReportVideo() {
        // Create a StatsCollector instance
        StatsCollector statsCollector = new StatsCollector();

        // Mock the RTCStatsReport
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("ARDAMSvstream1", rtcStats);
        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn(StatsCollector.OUTBOUND_RTP);
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap("video"));

        // Call the onStatsReport method with the play mode
        statsCollector.onStatsReport(report);

        assertEquals(8000, statsCollector.getLocalVideoBitrate());
        assertEquals(6, statsCollector.getLastKnownVideoBytesSent());

        when(rtcStats.getType()).thenReturn(StatsCollector.REMOTE_INBOUND_RTP);

        statsCollector.onStatsReport(report);

        StatsCollector.TrackStats videoTrackStat = statsCollector.getVideoTrackStatsMap().get(64L);

        assertEquals(10.0, videoTrackStat.getRoundTripTime(), 0.0);
        assertEquals(10.0, videoTrackStat.getJitter(), 0.0);
        assertEquals(10, videoTrackStat.getPacketsLost());
        assertEquals(1000L, videoTrackStat.getFirCount());
        assertEquals(1000L, videoTrackStat.getPliCount());
        assertEquals(1000L, videoTrackStat.getNackCount());
        assertEquals(1000L, videoTrackStat.getFramesEncoded());
        assertEquals(10000L, videoTrackStat.getPacketsSentPerSecond());
        assertEquals(1000000, videoTrackStat.getBytesSentPerSecond().intValue());
        assertEquals(0, videoTrackStat.getFramesEncodedDifference());

        assertEquals("stream1", videoTrackStat.getTrackId());

    }

    @Test
    public void testLocalAudioLevel() {
        StatsCollector statsCollector = new StatsCollector();
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("ARDAMSastream1", rtcStats);
        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn("media-source");
        when(rtcStats.getMembers()).thenReturn(createMembersMap("audio"));
        statsCollector.onStatsReport(report);
        assertEquals(100.0, statsCollector.getLocalAudioLevel(), 0.0);
    }

    // Helper method to create a members map
    private Map<String, Object> createMembersMap(String mediaType) {
        Map<String, Object> membersMap = new HashMap<>();
        membersMap.put("mediaType", mediaType);
        membersMap.put("packetsSent", 10L);
        membersMap.put(StatsCollector.FIR_COUNT, 1000L);
        membersMap.put(StatsCollector.PLI_COUNT, 1000L);
        membersMap.put(StatsCollector.NACK_COUNT, 1000L);
        membersMap.put(StatsCollector.FRAME_ENCODED, 1000L);
        membersMap.put(StatsCollector.JITTER, 10.0);
        membersMap.put(StatsCollector.ROUND_TRIP_TIME,10.0);
        membersMap.put(StatsCollector.PACKETS_LOST, 10);

        if(mediaType.equals("audio")){
            membersMap.put(StatsCollector.TRACK_ID, "ARDAMSastream1");
            membersMap.put(StatsCollector.TRACK_IDENTIFIER, "ARDAMSastream1");

            membersMap.put(StatsCollector.KIND, "audio");
            membersMap.put("audioLevel", 100.0);

        }else if(mediaType.equals("video")){
            membersMap.put(StatsCollector.TRACK_ID, "ARDAMSvstream1");
            membersMap.put(StatsCollector.TRACK_IDENTIFIER, "ARDAMSvstream1");

            membersMap.put(StatsCollector.KIND, "video");
        }
        membersMap.put(StatsCollector.SSRC, 64L);
        membersMap.put("bytesSent", new BigInteger("1000"));
        return membersMap;
    }
}
