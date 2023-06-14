package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import org.webrtc.audio.CustomWebRtcAudioRecord;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.antmedia.webrtcandroidframework.WebRTCClient;

public class MP3Publisher {

    private static final int DESIRED_SAMPLE_RATE = 48000;
    private final Activity activity;
    private final String filePath;
    private WebRTCClient webRTCClient;
    private boolean stoppedStream = false;
    private String TAG = MP3Publisher.class.getSimpleName();
    private boolean audioPushingEnabled = false;
    private Thread publisherThread;

    public MP3Publisher(WebRTCClient webRTCClient, Activity activity, String filePath) {
        this.webRTCClient = webRTCClient;
        this.activity = activity;
        this.filePath = filePath;

        webRTCClient.setInputSampleRate(48000);
        webRTCClient.setStereoInput(false);
        //default AudioFormat.ENCODING_PCM_16BIT
        webRTCClient.setAudioInputFormat(CustomWebRtcAudioRecord.DEFAULT_AUDIO_FORMAT);
        webRTCClient.setCustomAudioFeed(true);
    }

    public void startStreaming() {
        publisherThread = new Thread() {
            @Override
            public void run() {
                CustomWebRtcAudioRecord audioInput = webRTCClient.getAudioInput();
                while (audioInput.isStarted() == false) {
                    //It means that it's not initialized
                    try {
                        Thread.sleep(10);
                        Log.i("Audio", "Audio input is not initialized");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                MP3Publisher.this.audioPushingEnabled = true;
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                Log.i("Audio ", "Audio input is started");
                pushAudio();
            }
        };
        publisherThread.start();
    }



    public void pushAudio() {
            try {
                CustomWebRtcAudioRecord audioInput = webRTCClient.getAudioInput();

                int bufferLength = audioInput.getBufferByteLength(); // this is the length of 10ms data

                ByteBuffer rawAudioBuffer = ByteBuffer.allocate(bufferLength*20);

                /*
                final String uriPath="android.resource://"+activity.getPackageName()+"/raw/"+R.raw.sample_44100_stereo;
                final Uri uri= Uri.parse(uriPath);
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(activity, uri, null);
                 */


                // Read input file
                FileInputStream inputStream = new FileInputStream(filePath);

                // Create a MediaExtractor to extract audio data from the file
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(inputStream.getFD());


                    // Find and select the MP3 track
                MediaFormat format = null;
                int trackCount = extractor.getTrackCount();
                for (int i = 0; i < trackCount; i++) {
                    format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        extractor.selectTrack(i);
                        break;
                    }
                }

                if (format == null) {
                    Log.e(TAG, "No audio track found in MP3 file");
                    return;
                }

                // Create a MediaCodec to decode the MP3 file
                MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
                codec.configure(format, null, null, 0);
                codec.start();

                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                boolean isEOS = false;
                long presentationTimeUs = 0;


                // Decode the MP3 file to PCM
                while (!isEOS && stoppedStream == false) {
                    int inputBufferIndex = codec.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }

                    int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                    if (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        rawAudioBuffer.put(outputBuffer);
                        int length = rawAudioBuffer.position();

                        rawAudioBuffer.position(0);
                        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        int readBufferLength = bufferLength * format.getInteger(MediaFormat.KEY_SAMPLE_RATE) / DESIRED_SAMPLE_RATE*channelCount;
                        Log.d(TAG, "pushAudio: length: " + length + " bufferLength: " + bufferLength);
                        while(length - rawAudioBuffer.position() >= readBufferLength) {
                            byte[] pcmData = new byte[readBufferLength];
                            rawAudioBuffer.get(pcmData);
                            byte[] resampledData = modifySampleRate(pcmData, format.getInteger(MediaFormat.KEY_SAMPLE_RATE), DESIRED_SAMPLE_RATE, channelCount);

                            Log.d(TAG, "length: " + length+ " position: " + rawAudioBuffer.position());


                            audioInput.pushAudio(resampledData, resampledData.length);
                            Log.i("Audio", "push audio: " + pcmData[0] + " : " + pcmData[1] + " : " + pcmData[2] + " : " + pcmData[3] + " : ");
                            //emulate real time streaming by waiting 10ms because we're reading from the file directly
                            //When you decode the audio from incoming RTSP stream, you don't need to sleep, just send it immediately when you get
                            Thread.sleep(10);
                        }

                        byte[] moreData = new byte[length - rawAudioBuffer.position()];
                        rawAudioBuffer.get(moreData);
                        rawAudioBuffer.clear();
                        rawAudioBuffer.put(moreData);



                        codec.releaseOutputBuffer(outputBufferIndex, false);
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        format = codec.getOutputFormat();
                    }
                }

                // Release resources
                codec.stop();
                codec.release();
                extractor.release();
            } catch (IOException e) {
                Log.e(TAG, "Error decoding MP3 to PCM: " + e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
    }

    private byte[] modifySampleRate(byte[] pcmData, int inputSampleRate, int outputSampleRate, int channelCount) {
        int inputLength = pcmData.length / 2; // Dividing by 2 assuming 16-bit PCM data
        int outputLength = (int) ((inputLength / (float) inputSampleRate) * outputSampleRate)/channelCount;
        short[] inputSamples = new short[inputLength];
        short[] outputSamples = new short[outputLength];

        // Convert byte array to short array
        ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(inputSamples);

        // Perform linear interpolation and stereo to mono conversion
        float ratio = (float) inputSampleRate / outputSampleRate;
        for (int i = 0; i < outputLength; i++) {
            float index = i * ratio;
            int leftSampleIndex = (int) index;
            int rightSampleIndex = Math.min(leftSampleIndex + 1, inputLength - channelCount);
            float fraction = index - leftSampleIndex;

            short leftSample = inputSamples[leftSampleIndex * channelCount];
            short rightSample = inputSamples[Math.min(rightSampleIndex * channelCount, inputLength - 1)];

            // Linear interpolation and stereo to mono conversion
            outputSamples[i] = (short) (((1 - fraction) * leftSample + fraction * rightSample) / 2);
        }

        // Convert short array to byte array
        byte[] outputData = new byte[outputLength * 2]; // Multiplying by 2 assuming 16-bit PCM data
        ByteBuffer.wrap(outputData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outputSamples);

        return outputData;
    }

    public void stopStreaming() {
        stoppedStream = true;
    }
}
