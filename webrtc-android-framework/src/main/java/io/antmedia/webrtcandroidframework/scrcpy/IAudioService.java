package io.antmedia.webrtcandroidframework.scrcpy;

import android.media.AudioDeviceInfo;
import android.media.AudioManager;

public interface IAudioService {
    int getMode();

    boolean isSpeakerphoneOn();

    boolean isMicrophoneMute();

    int requestAudioFocus(AudioManager.OnAudioFocusChangeListener audioFocusChangeListener, int streamVoiceCall, int audiofocusGainTransient);

    void setMode(int modeInCommunication);

    void abandonAudioFocus(AudioManager.OnAudioFocusChangeListener audioFocusChangeListener);

    void setSpeakerphoneOn(boolean on);

    void setMicrophoneMute(boolean on);

    boolean isWiredHeadsetOn();

    AudioDeviceInfo[] getDevices(int getDevicesAll);

    boolean isBluetoothScoAvailableOffCall();

    void startBluetoothSco();

    void setBluetoothScoOn(boolean b);

    boolean isBluetoothScoOn();

    void stopBluetoothSco();
}
