package io.antmedia.webrtcandroidframework.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaMuxer;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import org.webrtc.audio.JavaAudioDeviceModule;

/**
 * Created by mekya on 28/03/2017.
 */

class AudioRecordListener implements JavaAudioDeviceModule.SamplesReadyCallback {

    private static final String TAG = AudioRecordListener.class.getSimpleName();
    private final long startTime;
    private final MediaMuxer mediaMuxer;

    private AudioHandler audioHandler;
    private HandlerThread audioHandlerThread;

    public AudioRecordListener(long recordStartTime, MediaMuxer mediaMuxer, int sampleRate, int channels) {
        this.startTime = recordStartTime;
        this.mediaMuxer = mediaMuxer;

        audioHandlerThread = new HandlerThread("AudioHandlerThread", Process.THREAD_PRIORITY_AUDIO);
        audioHandlerThread.start();
        audioHandler = new AudioHandler(audioHandlerThread.getLooper());

        //todo: it's better to get buffer size from WebRTCAudioRecord
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, AudioFormat.ENCODING_PCM_16BIT);
        audioHandler.startAudioEncoder(mediaMuxer, sampleRate, channels, bufferSize);
    }


    @Override
    public void onWebRtcAudioRecordSamplesReady(JavaAudioDeviceModule.AudioSamples audioSamples) {
        byte[] data = audioSamples.getData();

        Message msg = Message.obtain(audioHandler, AudioHandler.RECORD_AUDIO, data);
        msg.arg1 = data.length;
        msg.arg2 = (int)(System.currentTimeMillis() - startTime);
        audioHandler.sendMessage(msg);
    }

    public void stopAudioRecording() {
        audioHandler.stopAudioEncoder();
        audioHandlerThread.quitSafely();
    }

    public int getTrackIndex() {
        return audioHandler.getTrackIndex();
    }

    public void setMuxerStarted(boolean muxerStarted) {
        this.audioHandler.setMuxerStarted(muxerStarted);
    }


    public boolean isFinished() {
        return this.audioHandler.isStopMessageProcessed();
    }
}
