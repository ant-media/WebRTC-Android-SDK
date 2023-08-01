package io.antmedia.webrtcandroidframework;

import android.util.Log;

import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;

import java.math.BigInteger;
import java.util.Map;

public class StatsCollector {
    private static final String OUTBOUND_RTP = "outbound-rtp";
    private static final String AUDIO = "audio";
    private static final String MEDIA_TYPE = "mediaType";
    private static final String PACKETS_SENT = "packetsSent";
    private static final String BYTES_SENT = "bytesSent";
    private static final String VIDEO = "video";
    private double lastKnownStatsTimeStampMs;
    private long lastKnownAudioBytesSent;
    private long lastKnownVideoBytesSent;
    private long audioBitrate;
    private long videoBitrate;

    public void onStatsReport(RTCStatsReport report, String mode) {
        Log.i("Stats", "onStatsReport:\n"+report.toString());
        if (mode.equals(IWebRTCClient.MODE_PUBLISH)) {
            onSenderReport(report);
        } else if (mode.equals(IWebRTCClient.MODE_PLAY)) {
            onReceiverReport(report);
        } else if (mode.equals(IWebRTCClient.MODE_MULTI_TRACK_PLAY)) {
            onMultitrackReceiverReport(report);
        }
    }

    private void onMultitrackReceiverReport(RTCStatsReport report) {
    }

    private void onReceiverReport(RTCStatsReport report) {
    }

    private void onSenderReport(RTCStatsReport report) {
        Map<String, RTCStats> statsMap = report.getStatsMap();
        double timeMs = 0;
        for (Map.Entry<String, RTCStats> entry : statsMap.entrySet()) {
            RTCStats value = entry.getValue();
            if (OUTBOUND_RTP.equals(value.getType())) {
                timeMs = value.getTimestampUs() / 1000;
                long timeDiffSeconds = (long) ((timeMs - lastKnownStatsTimeStampMs) / 1000); // convert it to seconds
                timeDiffSeconds = timeDiffSeconds == 0 ? 1 : timeDiffSeconds; // avoid division by zero
                if (AUDIO.equals(value.getMembers().get(MEDIA_TYPE))) {
                    long packetsSent = (long) value.getMembers().get(PACKETS_SENT);
                    long bytesSent = ((BigInteger) value.getMembers().get(BYTES_SENT)).longValue();
                    audioBitrate = (bytesSent - lastKnownAudioBytesSent) / timeDiffSeconds * 8;
                    lastKnownAudioBytesSent = bytesSent;
                } else if (VIDEO.equals(value.getMembers().get(MEDIA_TYPE))) {
                    long packetsSent = (long) value.getMembers().get(PACKETS_SENT);
                    long bytesSent = ((BigInteger) value.getMembers().get(BYTES_SENT)).longValue();
                    videoBitrate = (bytesSent - lastKnownVideoBytesSent) / timeDiffSeconds * 8;
                    lastKnownVideoBytesSent = bytesSent;
                }
            }
        }
        lastKnownStatsTimeStampMs = timeMs;
        Log.i("Stats", "Audio bitrate: "+audioBitrate / 1000+" kbps, Video bitrate: "+videoBitrate / 1000+" kbps");
    }
}
