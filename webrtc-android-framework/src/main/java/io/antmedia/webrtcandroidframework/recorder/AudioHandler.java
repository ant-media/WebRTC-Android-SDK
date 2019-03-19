package io.antmedia.webrtcandroidframework.recorder;

import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by mekya on 28/03/2017.
 */
public class AudioHandler extends Handler {

    public static final int RECORD_AUDIO = 0;
    public static final int END_OF_STREAM = 2;

    private AudioEncoder audioEncoder = null;
    private boolean stopMessageProcessed = false;

    public AudioEncoder getAudioEncoder() {
        return audioEncoder;
    }

    public AudioHandler(Looper looper) {
        super(looper);

    }

    public boolean startAudioEncoder(MediaMuxer mediaMuxer, int sampleRate,  int channels, int bufferSize) {
        boolean result = false;

        audioEncoder = new AudioEncoder();
        try {
            result = audioEncoder.startAudioEncoder(sampleRate, channels, 64000, bufferSize, mediaMuxer);
        } catch (Exception e) {
            e.printStackTrace();
            audioEncoder = null;
        }
        return result;
    }

    public void stopAudioEncoder() {
        sendEmptyMessage(END_OF_STREAM);
    }

    public int getTrackIndex() {
        return audioEncoder.getTrackIndex();
    }


    @Override
    public void handleMessage(Message msg) {
        if (audioEncoder == null) {
            return;
        }

        switch (msg.what) {
            case END_OF_STREAM:
                if (audioEncoder.getState() == Thread.State.RUNNABLE) {
                    Log.d("audio handler", "stop audio encoding...");
                    audioEncoder.stopEncoding();
                    removeMessages(RECORD_AUDIO);
                    stopMessageProcessed = true;
                }
                break;
            case RECORD_AUDIO:
                Log.d("audio handler", "record audio encoding...");

                /* msg.obj is the byte array buffer
                         * msg.arg1 is the length of the byte array
                         * msg.arg2 is the timestamp of frame in milliseconds
                         */
                audioEncoder.encodeAudio((byte[]) msg.obj, msg.arg1, msg.arg2 * 1000);
                break;
        }
    }

    public void setMuxerStarted(boolean muxerStarted) {
        this.audioEncoder.setMuxerStarted(muxerStarted);
    }

    public boolean isStopMessageProcessed() {
        return stopMessageProcessed;
    }
}
