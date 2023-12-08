package io.antmedia.webrtcandroidframework.core;

import static org.webrtc.audio.WebRtcAudioRecord.BUFFERS_PER_SECOND;

import android.content.res.Resources;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Created by Ant Media on 03.12.2023
 *
 * This file reads mp3 or mp4 files to extract video audio data
 *
 */
public class MediaFileReader {
    private static final int DESIRED_SAMPLE_RATE = 48000;


    private AtomicBoolean stopRequested = new AtomicBoolean(false);
    private MediaFormat format;

    public interface VideoFrameListener {
        void onYuvImage(Image yuvImage);
    }

    public interface AudioFrameListener {
        void onAudioData(byte[] resampledData);
    }

    public enum FrameType {
        video,
        audio
    }
    private VideoFrameListener videoFrameListener;

    private AudioFrameListener audioFrameListener;
    private FrameType frameType;

    private MediaExtractor extractor;
    private String TAG = MediaFileReader.class.getSimpleName();

    private MediaFileReader(MediaExtractor extractor) {
        this.extractor = extractor;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static MediaFileReader fromResources(Resources resources, int resourceId) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(resources.openRawResourceFd(resourceId));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new MediaFileReader(extractor);
    }

    public static MediaFileReader fromPath(String filePath) {
        MediaExtractor extractor = new MediaExtractor();
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            extractor.setDataSource(inputStream.getFD());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new MediaFileReader(extractor);
    }

    public MediaFileReader withFrameType(FrameType frameType) {
        this.frameType = frameType;
        return this;
    }

    public MediaFileReader withVideoFrameListener(VideoFrameListener videoFrameListener) {
        this.videoFrameListener = videoFrameListener;
        return this;
    }

    public MediaFileReader withAudioFrameListener(AudioFrameListener audioFrameListener) {
        this.audioFrameListener = audioFrameListener;
        return this;
    }

    public void start() {
        format = getMediaFormat();
        Thread t = new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                decodeFrames();
            }
        };
        t.start();
    }

    @Nullable
    private MediaFormat getMediaFormat() {
        MediaFormat format = null;
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(frameType.toString()+"/")) {
                extractor.selectTrack(i);
                break;
            }
        }

        return format;
    }


    private void decodeFrames() {
        try {
            MediaCodec decoder = getMediaCodec();
            decoder.configure(format, null, null, 0);
            decoder.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isEOS = false;

            ByteBuffer rawAudioBuffer = ByteBuffer.allocate(get10MsBufferLength() *20);


            while (!Thread.interrupted() && !isEOS && !stopRequested.get()) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                System.out.println("inputIndex: " + inputIndex);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        isEOS = true;
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
                System.out.println("outputIndex: " + outputIndex);

                if (outputIndex >= 0) {
                    if(frameType == FrameType.video) {
                        Image yuvImage = decoder.getOutputImage(outputIndex);
                        videoFrameListener.onYuvImage(yuvImage);
                        yuvImage.close();
                    }
                    else if(frameType == FrameType.audio){
                        ByteBuffer outputBuffer = decoder.getOutputBuffer(outputIndex);
                        rawAudioBuffer.put(outputBuffer);
                        processAudio(rawAudioBuffer);
                    }
                    decoder.releaseOutputBuffer(outputIndex, false);
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Handle format change if needed
                }

                Thread.sleep(sleepTime(frameType));
            }

            decoder.stop();
            decoder.release();
            extractor.release();
            stopRequested.set(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MediaCodec getMediaCodec() throws IOException {
        MediaCodec decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        return decoder;
    }

    private void processAudio(ByteBuffer rawAudioBuffer) {

        int bufferLength = get10MsBufferLength();


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

            audioFrameListener.onAudioData(resampledData);

            //Log.i("Audio", "push audio: " + pcmData[0] + " : " + pcmData[1] + " : " + pcmData[2] + " : " + pcmData[3] + " : ");
            //emulate real time streaming by waiting 10ms because we're reading from the file directly
            //When you decode the audio from incoming RTSP stream, you don't need to sleep, just send it immediately when you get
        }

        byte[] moreData = new byte[length - rawAudioBuffer.position()];
        rawAudioBuffer.get(moreData);
        rawAudioBuffer.clear();
        rawAudioBuffer.put(moreData);
    }

    private int get10MsBufferLength() {
        int channels = 1;
        int bytesPerSample = 2; //WebRtcAudioRecord.getBytesPerSample(WebRtcAudioRecord.DEFAULT_AUDIO_FORMAT)

        int bytesPerFrame = channels * bytesPerSample;
        int framesPerBuffer = 44100 / BUFFERS_PER_SECOND;
        int bufferLength = bytesPerFrame * framesPerBuffer;
        return bufferLength;
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

    private long sleepTime(FrameType frameType) {
        return frameType == FrameType.video ? 50 : 10;
    }

    public void stop() {
        stopRequested.set(true);
    }

    public void setMediaExtractorForTest(MediaExtractor extractor) {
        this.extractor = extractor;
    }
}
