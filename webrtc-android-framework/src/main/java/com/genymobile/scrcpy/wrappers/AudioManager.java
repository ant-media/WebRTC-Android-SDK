package com.genymobile.scrcpy.wrappers;

import android.media.AudioDeviceInfo;
import android.os.IInterface;

import io.antmedia.webrtcandroidframework.scrcpy.IAudioService;

public class AudioManager implements IAudioService {

    private final IInterface manager;

    public AudioManager(IInterface manager) {
        this.manager = manager;
    }

    @Override
    public int getMode() {
        return 0;
    }

    @Override
    public boolean isSpeakerphoneOn() {
        return false;
    }

    @Override
    public boolean isMicrophoneMute() {
        return false;
    }

    @Override
    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener audioFocusChangeListener, int streamVoiceCall, int audiofocusGainTransient) {
        return 0;
    }

    @Override
    public void setMode(int modeInCommunication) {

    }

    @Override
    public void abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener audioFocusChangeListener) {

    }

    @Override
    public void setSpeakerphoneOn(boolean on) {

    }

    @Override
    public void setMicrophoneMute(boolean on) {

    }

    @Override
    public boolean isWiredHeadsetOn() {
        return false;
    }

    @Override
    public AudioDeviceInfo[] getDevices(int getDevicesAll) {
        return new AudioDeviceInfo[0];
    }

    @Override
    public boolean isBluetoothScoAvailableOffCall() {
        return false;
    }

    @Override
    public void startBluetoothSco() {

    }

    @Override
    public void setBluetoothScoOn(boolean b) {

    }

    @Override
    public boolean isBluetoothScoOn() {
        return false;
    }

    @Override
    public void stopBluetoothSco() {

    }
}
