package io.antmedia.webrtcandroidframework;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class CustomVideoCapturerTest {
    @Mock
    private CapturerObserver capturerObserver;

    @Mock
    private SurfaceTextureHelper surfaceTextureHelper;

    @Mock
    private Context applicationContext;

    private CustomVideoCapturer customVideoCapturer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        customVideoCapturer = new CustomVideoCapturer();
    }

    @Test
    public void testWriteFrame() {
        VideoFrame videoFrame = mock(VideoFrame.class);

        customVideoCapturer.initialize(surfaceTextureHelper, applicationContext, capturerObserver);
        customVideoCapturer.writeFrame(videoFrame);

        verify(capturerObserver).onFrameCaptured(videoFrame);
        verify(videoFrame).release();
    }

    @Test
    public void testStartCapture() {
        customVideoCapturer.startCapture(0, 0, 0);
    }

    @Test
    public void testStopCapture() throws InterruptedException {
        customVideoCapturer.stopCapture();
    }

    @Test
    public void testChangeCaptureFormat() {
        customVideoCapturer.changeCaptureFormat(0, 0, 0);
    }

    @Test
    public void testDispose() {
        customVideoCapturer.dispose();
    }

    @Test
    public void testIsScreencast() {
        boolean isScreencast = customVideoCapturer.isScreencast();
        assertFalse(isScreencast);
    }
}
