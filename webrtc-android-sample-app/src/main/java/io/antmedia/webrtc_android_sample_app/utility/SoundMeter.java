package io.antmedia.webrtc_android_sample_app.utility;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SoundMeter {
    private final int SAMPLE_RATE = 44100;
    private final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private Activity activity;
    private AudioRecord audioRecord;
    private ScheduledExecutorService executorService;
    private long updateAudioLevelFrequencyMs = 250L;
    private LocalAudioLevelListener audioLevelListener;


    public SoundMeter(long updateAudioLevelFrequencyMs, Activity activity, LocalAudioLevelListener listener) {
        this.activity = activity;
        this.audioLevelListener = listener;
        this.updateAudioLevelFrequencyMs = updateAudioLevelFrequencyMs;
        init();

    }

    private void init(){
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("RECORD_AUDIO permission not granted.");

        }
        setAudioRecord(new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE));

    }

    public void start(){
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();

            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(() -> {
                short[] buffer = new short[BUFFER_SIZE / 2];
                int numSamples = audioRecord.read(buffer, 0, buffer.length);
                if (numSamples > 0) {
                    double rms = calculateRMS(buffer, numSamples);
                    double db = 20 * Math.log10(rms);
                    audioLevelListener.onAudioLevelUpdated(db);
                }
            }, 0, updateAudioLevelFrequencyMs, TimeUnit.MILLISECONDS);

        }

    }

    public void stop(){
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;

        }
        if(executorService != null){
            executorService.shutdown();
        }
    }

    public double calculateRMS(short[] audioData, int numSamples) {
        double sum = 0;
        for (int i = 0; i < numSamples; i++) {
            sum += audioData[i] * audioData[i];
        }
        double mean = sum / numSamples;
        return Math.sqrt(mean);
    }

    public void setAudioRecord(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
    }
}
