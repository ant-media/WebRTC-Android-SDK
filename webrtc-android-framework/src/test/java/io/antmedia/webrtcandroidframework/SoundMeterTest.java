package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;

import org.junit.Before;
import org.junit.Test;

import io.antmedia.webrtcandroidframework.utility.LocalAudioLevelListener;
import io.antmedia.webrtcandroidframework.utility.SoundMeter;

public class SoundMeterTest {

    private Activity mockActivity;
    private LocalAudioLevelListener mockListener;

    @Before
    public void setUp() {
        mockActivity = mock(Activity.class);
        mockListener = mock(LocalAudioLevelListener.class);
    }

    @Test
    public void testStartAndStopRecording() {
        // Mock permission granted
        when(mockActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)).thenReturn(PackageManager.PERMISSION_GRANTED);

        // Mock AudioRecord
        AudioRecord mockAudioRecord = mock(AudioRecord.class);
        when(mockAudioRecord.getState()).thenReturn(AudioRecord.STATE_INITIALIZED);
        when(mockAudioRecord.read(any(short[].class), anyInt(), anyInt())).thenReturn(3);

        SoundMeter soundMeter = spy(new SoundMeter(250L, mockActivity, mockListener));
        soundMeter.setAudioRecord(mockAudioRecord);

        doReturn(10d).when(soundMeter).calculateRMS(any(short[].class), anyInt());

        // Start recording
        soundMeter.start();

        // Verify interactions with AudioRecord
        verify(mockAudioRecord, timeout(1000)).startRecording();

        verify(mockListener, timeout(1000).atLeast(1)).onAudioLevelUpdated(anyInt());

        // Stop recording
        soundMeter.stop();

        verify(mockAudioRecord).stop();
        verify(mockAudioRecord).release();
    }

    @Test
    public void testCalculateRMS() {
        // Given
        SoundMeter soundMeter = new SoundMeter(250L, mockActivity, mockListener);

        // Test data
        short[] audioData = {100, 200, 300}; // Example audio data
        int numSamples = audioData.length;

        // Expected RMS value
        double expectedRMS = Math.sqrt((100 * 100 + 200 * 200 + 300 * 300) / numSamples);

        // When
        double actualRMS = soundMeter.calculateRMS(audioData, numSamples);

        // Then
        assertEquals(expectedRMS, actualRMS, 0.005); // Adjust delta according to your precision needs
    }
}
