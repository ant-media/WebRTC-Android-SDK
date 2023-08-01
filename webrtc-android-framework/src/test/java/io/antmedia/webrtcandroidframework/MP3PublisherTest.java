package io.antmedia.webrtcandroidframework;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.webrtc.audio.CustomWebRtcAudioRecord;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class MP3PublisherTest {

    @Mock
    private WebRTCClient mockWebRTCClient;

    @Mock
    private Activity mockActivity;

    @Mock
    private CustomWebRtcAudioRecord mockAudioInput;

    @Mock
    private MediaCodec mockMediaCodec;

    @Mock
    private MediaExtractor mockMediaExtractor;

    @Mock
    private MediaFormat mockMediaFormat;

    private MP3Publisher mp3Publisher;
    private String testFilePath;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        testFilePath = "test_audio.mp3";
        mp3Publisher = spy(new MP3Publisher(mockWebRTCClient, mockActivity, testFilePath));
        when(mockWebRTCClient.getAudioInput()).thenReturn(mockAudioInput);

        when(mockMediaExtractor.getTrackFormat(anyInt())).thenReturn(mockMediaFormat);
        when(mockMediaExtractor.getTrackCount()).thenReturn(1);
        when(mockMediaExtractor.getSampleTrackIndex()).thenReturn(0);
        doNothing().when(mockMediaExtractor).selectTrack(anyInt());
        doNothing().when(mockMediaExtractor).release();

        // Mock the MediaCodec behavior
        when(mockMediaCodec.getOutputFormat()).thenReturn(mockMediaFormat);
        doReturn(mockMediaCodec).when(mp3Publisher).getCodecByName(any());
        doNothing().when(mockMediaCodec).configure(any(MediaFormat.class), any(), any(), anyInt());
        doNothing().when(mockMediaCodec).start();
        doReturn(0).when(mockMediaCodec).dequeueInputBuffer(anyLong());
        doReturn(0).when(mockMediaCodec).dequeueOutputBuffer(any(MediaCodec.BufferInfo.class), anyLong());
        doNothing().when(mockMediaCodec).release();
        doNothing().when(mockMediaCodec).stop();
        doReturn(ByteBuffer.allocate(1024)).when(mockMediaCodec).getOutputBuffer(anyInt());

        // Set the mock MediaCodec in MP3Publisher
        doReturn(mockMediaCodec).when(mp3Publisher).getMediaCodec(any(MediaFormat.class));

        when(mockMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(1);
        when(mockMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(48000);

        when(mockAudioInput.getBufferByteLength()).thenReturn(1024);

        ByteBuffer inputBuffers[] = new ByteBuffer[] {ByteBuffer.allocate(2048)};
        when(mockMediaCodec.getInputBuffers()).thenReturn(inputBuffers);

        ByteBuffer outputBuffers[] = new ByteBuffer[] {ByteBuffer.allocate(2048)};
        when(mockMediaCodec.getOutputBuffers()).thenReturn(outputBuffers);

        when(mockMediaExtractor.readSampleData(any(), anyInt())).thenAnswer(new Answer<Integer>() {
            private int count = 0;

            @Override
            public Integer answer(InvocationOnMock invocation) {
                return count++ < 10 ? 100 : -1;
            }
        });
    }

    @After
    public void tearDown() {
        mp3Publisher = null;
    }

    @Test
    public void testStartStreaming() {
        mp3Publisher.startStreaming();
        verify(mockWebRTCClient, timeout(1000)).getAudioInput();
    }

    @Test
    public void testReadAndPublishFile() throws Exception {
        // Mock raw audio data and its buffer
        int bufferLength = 1024; // This should match the buffer length used in the MP3Publisher class
        byte[] rawAudioData = new byte[bufferLength];
        ByteBuffer rawAudioBuffer = ByteBuffer.wrap(rawAudioData);
        when(mockMediaCodec.dequeueOutputBuffer(any(MediaCodec.BufferInfo.class), anyLong())).thenReturn(0);
        when(mockMediaCodec.getOutputBuffer(0)).thenReturn(rawAudioBuffer);

        doReturn(ByteBuffer.allocate(2048)).when(mp3Publisher).getRawAudioByteBuffer(anyInt());

        // Run the readAndPublishFile method
        mp3Publisher.readAndPublishFile(mockMediaExtractor, mockMediaFormat, mockMediaCodec);

        // Verify that the output buffer is used to push audio data
        verify(mockAudioInput, atLeastOnce()).pushAudio(eq(rawAudioData), eq(bufferLength));

        // Verify that the MediaCodec methods are called as expected
        verify(mockMediaExtractor, atLeastOnce()).readSampleData(any(ByteBuffer.class), anyInt());
        verify(mockMediaExtractor, atLeastOnce()).getSampleTime();
        verify(mockMediaCodec, atLeastOnce()).dequeueInputBuffer(anyLong());
        verify(mockMediaCodec, atLeastOnce()).queueInputBuffer(anyInt(), anyInt(), anyInt(), anyLong(), anyInt());
        verify(mockMediaCodec, atLeastOnce()).dequeueOutputBuffer(any(MediaCodec.BufferInfo.class), anyLong());
        verify(mockMediaCodec, atLeastOnce()).releaseOutputBuffer(anyInt(), eq(false));
    }

}
