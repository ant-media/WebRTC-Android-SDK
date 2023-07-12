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

import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.StatsCollector;

import static org.mockito.Mockito.*;

public class StatsCollectorTest {

    @Mock
    private RTCStatsReport report;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testOnStatsReportPublish() {
        // Create a StatsCollector instance
        StatsCollector statsCollector = new StatsCollector();

        // Mock the RTCStatsReport
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("statsKey", rtcStats);
        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn("outbound-rtp");
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap("audio"));

        // Call the onStatsReport method with the publish mode
        statsCollector.onStatsReport(report, IWebRTCClient.MODE_PUBLISH);

        // Verify that the audio bitrate is logged correctly
        // You can add additional verification based on your requirements
        // verify(mockLogger).log("Audio bitrate: 64 kbps, Video bitrate: 0 kbps");
    }

    @Test
    public void testOnStatsReportPlay() {
        // Create a StatsCollector instance
        StatsCollector statsCollector = new StatsCollector();

        // Mock the RTCStatsReport
        Map<String, RTCStats> statsMap = new HashMap<>();
        RTCStats rtcStats = mock(RTCStats.class);
        statsMap.put("statsKey", rtcStats);
        when(report.getStatsMap()).thenReturn(statsMap);
        when(rtcStats.getType()).thenReturn("outbound-rtp");
        when(rtcStats.getTimestampUs()).thenReturn(1000.0);
        when(rtcStats.getMembers()).thenReturn(createMembersMap("video"));

        // Call the onStatsReport method with the play mode
        statsCollector.onStatsReport(report, IWebRTCClient.MODE_PLAY);

        // Verify that the video bitrate is logged correctly
        // You can add additional verification based on your requirements
        // verify(mockLogger).log("Audio bitrate: 0 kbps, Video bitrate: 128 kbps");
    }

    // Add more test cases to cover other methods and scenarios within the StatsCollector class

    // Helper method to create a members map
    private Map<String, Object> createMembersMap(String mediaType) {
        Map<String, Object> membersMap = new HashMap<>();
        membersMap.put("mediaType", mediaType);
        membersMap.put("packetsSent", 10L);
        membersMap.put("bytesSent", new BigInteger("1000"));
        return membersMap;
    }
}
