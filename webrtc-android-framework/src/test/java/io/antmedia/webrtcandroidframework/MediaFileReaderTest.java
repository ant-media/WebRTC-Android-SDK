package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.antmedia.webrtcandroidframework.core.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.core.MediaFileReader;

public class MediaFileReaderTest {

    @Test
    public void testCreateFromResources() throws IOException {
        Resources resources = mock(Resources.class);
        int resourceId = 1;
        when(resources.openRawResourceFd(resourceId)).thenReturn(null);
        MediaFileReader mfr = MediaFileReader.fromResources(resources, resourceId);

        assertNotNull(mfr);
    }

    @Test
    public void testVideo() throws IOException {
        MediaExtractor extractor = mock(MediaExtractor.class);
        MediaFormat format = mock(MediaFormat.class);
        MediaCodec decoder = mock(MediaCodec.class);
        Image image = mock(Image.class);
        MediaFileReader.VideoFrameListener videoListener = mock(MediaFileReader.VideoFrameListener.class);

        when(extractor.getTrackCount()).thenReturn(1);
        when(extractor.getTrackFormat(0)).thenReturn(format);
        when(format.getString(MediaFormat.KEY_MIME)).thenReturn("video/x");
        when(decoder.getOutputImage(0)).thenReturn(image);

        MediaFileReader mfr = spy(MediaFileReader.fromPath("src/test/resources/dummy.file")
                .withVideoFrameListener(videoListener)
                .withFrameType(MediaFileReader.FrameType.video));

        doReturn(decoder).when(mfr).getMediaCodec();

        assertNotNull(mfr);

        mfr.setMediaExtractorForTest(extractor);

        mfr.start();

        verify(extractor).selectTrack(0);

        verify(videoListener, timeout(2000)).onYuvImage(image);

        mfr.stop();

        verify(extractor,timeout(2000)).release();
    }

    @Test
    public void testAudio() throws IOException {
        MediaExtractor extractor = mock(MediaExtractor.class);
        MediaFormat format = mock(MediaFormat.class);
        MediaCodec decoder = mock(MediaCodec.class);
        MediaFileReader.AudioFrameListener audioListener = mock(MediaFileReader.AudioFrameListener.class);

        when(extractor.getTrackCount()).thenReturn(1);
        when(extractor.getTrackFormat(0)).thenReturn(format);
        when(format.getString(MediaFormat.KEY_MIME)).thenReturn("audio/x");
        when(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(1);
        when(format.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(48000);
        when(decoder.getOutputBuffer(0)).thenReturn(null);

        byte[] data = new byte[1000];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        when(decoder.getOutputBuffer(0)).thenReturn(buffer);

        MediaFileReader mfr = spy(MediaFileReader.fromPath("src/test/resources/dummy.file")
                .withAudioFrameListener(audioListener)
                .withFrameType(MediaFileReader.FrameType.audio));

        doReturn(decoder).when(mfr).getMediaCodec();

        assertNotNull(mfr);

        mfr.setMediaExtractorForTest(extractor);

        mfr.start();

        verify(extractor).selectTrack(0);

        verify(audioListener, timeout(2000)).onAudioData(any());

        mfr.stop();

        verify(extractor,timeout(2000)).release();
    }
}
