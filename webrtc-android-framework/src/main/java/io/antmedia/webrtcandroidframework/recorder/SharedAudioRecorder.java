package io.antmedia.webrtcandroidframework.recorder;

import android.content.Context;
import android.media.AudioManager;

import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioRecord;

import javax.annotation.Nullable;

public class SharedAudioRecorder extends WebRtcAudioRecord{
    public SharedAudioRecorder(Context context, AudioManager audioManager, int audioSource, @Nullable JavaAudioDeviceModule.AudioRecordErrorCallback errorCallback, @Nullable JavaAudioDeviceModule.SamplesReadyCallback audioSamplesReadyCallback, boolean isAcousticEchoCancelerSupported, boolean isNoiseSuppressorSupported) {
        super(context, audioManager, audioSource, errorCallback, audioSamplesReadyCallback, isAcousticEchoCancelerSupported, isNoiseSuppressorSupported);
    }
}
