package io.antmedia.webrtcandroidframework;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import io.antmedia.webrtcandroidframework.core.ProxyVideoSink;

public class ProxyVideoSinkTest {
    @Test
    public void test() {
        ProxyVideoSink proxyVideoSink = new ProxyVideoSink();
        VideoSink videoSink = mock(VideoSink.class);
        proxyVideoSink.setTarget(videoSink);

        VideoFrame frame = mock(VideoFrame.class);
        proxyVideoSink.onFrame(frame);

        verify(videoSink).onFrame(frame);

    }

}