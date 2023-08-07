package io.antmedia.webrtcandroidframework.scrcpy;

import android.media.AudioDeviceInfo;
import android.os.Build;

import androidx.annotation.RequiresApi;

import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager;

public class AudioManagerWrapper implements IAudioService {

    private final android.media.AudioManager audioManager;

    public AudioManagerWrapper(android.media.AudioManager audioManager) {
        this.audioManager = audioManager;

    }

    @Override
    public int getMode() {
        return this.audioManager.getMode();
    }

    @Override
    public boolean isSpeakerphoneOn() {
        return this.audioManager.isSpeakerphoneOn();
    }

    @Override
    public boolean isMicrophoneMute() {
        return this.audioManager.isMicrophoneMute();
    }

    @Override
    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener audioFocusChangeListener, int streamVoiceCall, int audiofocusGainTransient) {
        return this.audioManager.requestAudioFocus(audioFocusChangeListener, streamVoiceCall, audiofocusGainTransient);
    }

    @Override
    public void setMode(int modeInCommunication) {
        this.audioManager.setMode(modeInCommunication);
    }

    @Override
    public void abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener audioFocusChangeListener) {
        this.audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    @Override
    public void setSpeakerphoneOn(boolean on) {
        this.audioManager.setSpeakerphoneOn(on);
    }

    @Override
    public void setMicrophoneMute(boolean on) {
        this.audioManager.setMicrophoneMute(on);
    }

    @Override
    public boolean isWiredHeadsetOn() {
        return this.audioManager.isWiredHeadsetOn();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public AudioDeviceInfo[] getDevices(int getDevicesAll) {
        return this.audioManager.getDevices(getDevicesAll);
    }

    @Override
    public boolean isBluetoothScoAvailableOffCall() {
        return this.audioManager.isBluetoothScoAvailableOffCall();
    }

    @Override
    public void startBluetoothSco() {
        this.audioManager.startBluetoothSco();
    }

    @Override
    public void setBluetoothScoOn(boolean b) {
        this.audioManager.setBluetoothScoOn(b);
    }

    @Override
    public boolean isBluetoothScoOn() {
        return this.audioManager.isBluetoothScoOn();
    }

    @Override
    public void stopBluetoothSco() {
        this.audioManager.stopBluetoothSco();
    }
}
