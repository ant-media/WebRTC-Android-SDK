package io.antmedia.webrtcandroidframework.recorder;

import android.media.AudioFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.util.Log;

/**
 * Created by mekya on 28/03/2017.
 */

class AudioRecorderThread extends Thread {

    private static final String TAG = AudioRecorderThread.class.getSimpleName();
    private final int mSampleRate;
    private final long startTime;
    private final MediaMuxer mediaMuxer;
    private volatile boolean stopThread = false;

    private android.media.AudioRecord audioRecord;
    private AudioHandler audioHandler;
    private HandlerThread audioHandlerThread;

    public AudioRecorderThread(int sampleRate, long recordStartTime, MediaMuxer mediaMuxer) {
        this.mSampleRate = sampleRate;
        this.startTime = recordStartTime;
        this.mediaMuxer = mediaMuxer;

        audioHandlerThread = new HandlerThread("AudioHandlerThread", Process.THREAD_PRIORITY_AUDIO);
        audioHandlerThread.start();
        audioHandler = new AudioHandler(audioHandlerThread.getLooper());
    }


    @Override
    public void run() {
        //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = android.media.AudioRecord
                .getMinBufferSize(mSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);
        byte[][] audioData;
        int bufferReadResult;

        audioHandler.startAudioEncoder(mediaMuxer, mSampleRate, bufferSize);

        audioRecord = new android.media.AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        // divide byte buffersize to 2 to make it short buffer
        audioData = new byte[1000][bufferSize];

        audioRecord.startRecording();

        int i = 0;
        byte[] data;
        while ((bufferReadResult = audioRecord.read(audioData[i], 0, audioData[i].length)) > 0) {

            data = audioData[i];

            Message msg = Message.obtain(audioHandler, AudioHandler.RECORD_AUDIO, data);
            msg.arg1 = bufferReadResult;
            msg.arg2 = (int)(System.currentTimeMillis() - startTime);
            audioHandler.sendMessage(msg);


            i++;
            if (i == 1000) {
                i = 0;
            }
            if (stopThread) {
                break;
            }
        }

        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;

        audioHandler.stopAudioEncoder();
        audioHandlerThread.quitSafely();

        Log.d(TAG, "AudioThread Finished, release audioRecord");

    }

    public void stopAudioRecording() {

        if (audioRecord != null && audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
            stopThread = true;
        }
    }

    public int getTrackIndex() {
        return audioHandler.getTrackIndex();
    }

    public void setMuxerStarted(boolean muxerStarted) {
        this.audioHandler.setMuxerStarted(muxerStarted);
    }
}
