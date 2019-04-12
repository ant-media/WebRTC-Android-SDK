package io.antmedia.webrtcandroidframework.recorder;

import android.content.Context;
import android.media.AudioManager;

import org.webrtc.Logging;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioEffects;
import org.webrtc.audio.WebRtcAudioManager;
import org.webrtc.audio.WebRtcAudioRecord;
import org.webrtc.audio.WebRtcAudioTrack;


public class SharedAudioRecorderBuilder  {

    private final Context context;
    private final AudioManager audioManager;
    private int sampleRate;
    private int audioSource;
    private JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback;
    private JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback;
    private JavaAudioDeviceModule.SamplesReadyCallback samplesReadyCallback;
    private boolean useHardwareAcousticEchoCanceler;
    private boolean useHardwareNoiseSuppressor;
    private boolean useStereoInput;
    private boolean useStereoOutput;
    private WebRtcAudioRecord audioInput;
    private WebRtcAudioRecord.IAudioRecordStatusListener recordStatusListener = null;

    public SharedAudioRecorderBuilder(Context context) {
        this.audioSource = 7;
        this.useHardwareAcousticEchoCanceler = WebRtcAudioEffects.isAcousticEchoCancelerSupported();
        this.useHardwareNoiseSuppressor = WebRtcAudioEffects.isNoiseSuppressorSupported();
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService("audio");
        this.sampleRate = WebRtcAudioManager.getSampleRate(this.audioManager);
    }

    public SharedAudioRecorderBuilder setSampleRate(int sampleRate) {
        Logging.d("JavaAudioDeviceModule", "Sample rate overridden to: " + sampleRate);
        this.sampleRate = sampleRate;
        return this;
    }

    public SharedAudioRecorderBuilder setAudioSource(int audioSource) {
        this.audioSource = audioSource;
        return this;
    }

    public SharedAudioRecorderBuilder setAudioTrackErrorCallback(JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback) {
        this.audioTrackErrorCallback = audioTrackErrorCallback;
        return this;
    }

    public SharedAudioRecorderBuilder setAudioRecordErrorCallback(JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback) {
        this.audioRecordErrorCallback = audioRecordErrorCallback;
        return this;
    }

    public SharedAudioRecorderBuilder setSamplesReadyCallback(JavaAudioDeviceModule.SamplesReadyCallback samplesReadyCallback) {
        this.samplesReadyCallback = samplesReadyCallback;
        return this;
    }

    public SharedAudioRecorderBuilder setUseHardwareNoiseSuppressor(boolean useHardwareNoiseSuppressor) {
        if (useHardwareNoiseSuppressor && !WebRtcAudioEffects.isNoiseSuppressorSupported()) {
            Logging.e("JavaAudioDeviceModule", "HW NS not supported");
            useHardwareNoiseSuppressor = false;
        }

        this.useHardwareNoiseSuppressor = useHardwareNoiseSuppressor;
        return this;
    }

    public SharedAudioRecorderBuilder setUseHardwareAcousticEchoCanceler(boolean useHardwareAcousticEchoCanceler) {
        if (useHardwareAcousticEchoCanceler && !WebRtcAudioEffects.isAcousticEchoCancelerSupported()) {
            Logging.e("JavaAudioDeviceModule", "HW AEC not supported");
            useHardwareAcousticEchoCanceler = false;
        }

        this.useHardwareAcousticEchoCanceler = useHardwareAcousticEchoCanceler;
        return this;
    }

    public SharedAudioRecorderBuilder setUseStereoInput(boolean useStereoInput) {
        this.useStereoInput = useStereoInput;
        return this;
    }

    public SharedAudioRecorderBuilder setUseStereoOutput(boolean useStereoOutput) {
        this.useStereoOutput = useStereoOutput;
        return this;
    }

    public SharedAudioRecorderBuilder setAudioRecordStatusListener(WebRtcAudioRecord.IAudioRecordStatusListener recordStatusListener) {
        this.recordStatusListener = recordStatusListener;
        return this;
    }

    public AudioDeviceModule createAudioDeviceModule() {
        Logging.d("JavaAudioDeviceModule", "createAudioDeviceModule");
        if (this.useHardwareNoiseSuppressor) {
            Logging.d("JavaAudioDeviceModule", "HW NS will be used.");
        } else {
            if (WebRtcAudioEffects.isNoiseSuppressorSupported()) {
                Logging.d("JavaAudioDeviceModule", "Overriding default behavior; now using WebRTC NS!");
            }

            Logging.d("JavaAudioDeviceModule", "HW NS will not be used.");
        }

        if (this.useHardwareAcousticEchoCanceler) {
            Logging.d("JavaAudioDeviceModule", "HW AEC will be used.");
        } else {
            if (WebRtcAudioEffects.isAcousticEchoCancelerSupported()) {
                Logging.d("JavaAudioDeviceModule", "Overriding default behavior; now using WebRTC AEC!");
            }

            Logging.d("JavaAudioDeviceModule", "HW AEC will not be used.");
        }

        audioInput = new WebRtcAudioRecord(this.context, this.audioManager, this.audioSource, this.audioRecordErrorCallback,
                this.samplesReadyCallback, this.useHardwareAcousticEchoCanceler, this.useHardwareNoiseSuppressor, this.recordStatusListener);
        WebRtcAudioTrack audioOutput = new WebRtcAudioTrack(this.context, this.audioManager, this.audioTrackErrorCallback);
        return new JavaAudioDeviceModule(this.context, this.audioManager, audioInput, audioOutput, this.sampleRate, this.useStereoInput, this.useStereoOutput);
    }

    public WebRtcAudioRecord getAudioInput() {
        return audioInput;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
