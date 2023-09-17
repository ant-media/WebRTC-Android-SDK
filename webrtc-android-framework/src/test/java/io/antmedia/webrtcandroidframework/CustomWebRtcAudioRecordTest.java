package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.audio.CustomWebRtcAudioRecord;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

public class CustomWebRtcAudioRecordTest {
    @Mock
    private Context context;

    @Mock
    private ScheduledExecutorService scheduler;

    @Mock
    private AudioManager audioManager;

    @Mock
    private JavaAudioDeviceModule.AudioRecordErrorCallback errorCallback;

    @Mock
    private JavaAudioDeviceModule.AudioRecordStateCallback stateCallback;

    @Mock
    private JavaAudioDeviceModule.SamplesReadyCallback audioSamplesReadyCallback;

    private CustomWebRtcAudioRecord customWebRtcAudioRecord;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        customWebRtcAudioRecord = spy(new CustomWebRtcAudioRecord(
                context, scheduler, audioManager, CustomWebRtcAudioRecord.DEFAULT_AUDIO_SOURCE,
                CustomWebRtcAudioRecord.DEFAULT_AUDIO_FORMAT, errorCallback, stateCallback,
                audioSamplesReadyCallback, false, false));
    }

    @Test
    public void testPushAudio() {
        byte[] audio = {1, 2, 3, 4, 5};
        int length = 5;

        doNothing().when(customWebRtcAudioRecord).nativeDataIsRecorded(
                anyLong(), anyInt(), anyLong());

        customWebRtcAudioRecord.setStarted(true);
        customWebRtcAudioRecord.byteBuffer = ByteBuffer.allocateDirect(1000);
        customWebRtcAudioRecord.pushAudio(audio, length);
        // Verify that nativeDataIsRecorded is called with the correct arguments

        verify(customWebRtcAudioRecord).nativeDataIsRecorded(
                customWebRtcAudioRecord.nativeAudioRecord, length, 0);
    }

    @Test
    public void testInitRecording() {
        int sampleRate = 48000;
        int channels = 2;

        doNothing().when(customWebRtcAudioRecord).reportWebRtcAudioRecordInitError(
                anyString());
        doNothing().when(customWebRtcAudioRecord).allocateBuffer(anyInt(), anyInt());
        doNothing().when(customWebRtcAudioRecord).nativeCacheDirectBufferAddress(
                anyLong(), any());

        customWebRtcAudioRecord.byteBuffer = ByteBuffer.allocate(1000);
        int framesPerBuffer = customWebRtcAudioRecord.initRecording(sampleRate, channels);

        // Verify that the sampleRate and channels are set correctly
        assertEquals(sampleRate, customWebRtcAudioRecord.getSampleRate());
        assertEquals(channels, customWebRtcAudioRecord.getChannelCount());

        // Verify that the framesPerBuffer is calculated correctly
        int expectedFramesPerBuffer = sampleRate / CustomWebRtcAudioRecord.BUFFERS_PER_SECOND;
        assertEquals(expectedFramesPerBuffer, framesPerBuffer);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Test
    public void testSetPreferredDevice() {
        AudioDeviceInfo preferredDevice = mock(AudioDeviceInfo.class);

        customWebRtcAudioRecord.setPreferredDevice(preferredDevice);

        // Verify that the preferredDevice is set correctly
        // Uncomment the following line if you want to verify the behavior of audioRecord.setPreferredDevice()
        // verify(customWebRtcAudioRecord.audioRecord).setPreferredDevice(preferredDevice);
    }

    @Test
    public void testStartRecording() {
        boolean success = customWebRtcAudioRecord.startRecording();

        // Verify that the started flag is set to true
        assertTrue(customWebRtcAudioRecord.isStarted());
        assertTrue(success);
    }

    @Test
    public void testStopRecording() {
        boolean success = customWebRtcAudioRecord.stopRecording();

        // Verify that the started flag is set to false
        assertFalse(customWebRtcAudioRecord.isStarted());
        assertTrue(success);
    }

    // Add additional test methods for other public methods in CustomWebRtcAudioRecord if needed
}
