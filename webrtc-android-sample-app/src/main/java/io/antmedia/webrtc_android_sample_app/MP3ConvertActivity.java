package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MP3ConvertActivity extends Activity {
    private static final int TARGET_SAMPLE_RATE = 48000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 30){
            if (!Environment.isExternalStorageManager()){
                Intent getpermission = new Intent();
                getpermission.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(getpermission);
            }
        }


        // Path to the input and output files
        String inputFile = Environment.getExternalStorageDirectory() + "/input.mp3";
        String outputFile = Environment.getExternalStorageDirectory() + "/output.mp3";

        try {
            // Read input file
            FileInputStream inputStream = new FileInputStream(inputFile);

            // Create a MediaExtractor to extract audio data from the file
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(inputStream.getFD());

            // Find the audio track in the file
            int audioTrackIndex = findAudioTrackIndex(extractor);
            extractor.selectTrack(audioTrackIndex);

            // Get the original audio format
            MediaFormat originalFormat = extractor.getTrackFormat(audioTrackIndex);

            // Get the original sample rate
            int originalSampleRate = originalFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            // Get the original channel count
            int originalChannelCount = originalFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            // Get the original bit rate
            int originalBitRate = originalFormat.getInteger(MediaFormat.KEY_BIT_RATE);

            // Calculate the number of output samples based on the original and target sample rates
            long targetSampleCount = getTargetSampleCount(originalSampleRate, TARGET_SAMPLE_RATE, inputStream.getFD());

            // Calculate the duration of the output file
            long targetDurationUs = (targetSampleCount * 1000000) / TARGET_SAMPLE_RATE;

            // Create a MediaFormat for the output file with the desired sample rate, channel count, and bit rate
            MediaFormat targetFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_MPEG, TARGET_SAMPLE_RATE, originalChannelCount);
            targetFormat.setInteger(MediaFormat.KEY_BIT_RATE, originalBitRate);
            targetFormat.setInteger(MediaFormat.KEY_DURATION, (int) targetDurationUs);


            // Create a MediaCodec to encode the audio data with the desired format
            MediaCodec codec = MediaCodec.createByCodecName("OMX.google.mp3.decoder");
            //MediaCodec codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_MPEG);
            codec.configure(targetFormat, null, null, 0);//MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            // Create a FileOutputStream to write the output file
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            // Create a ByteBuffer to hold the encoded audio data
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

            // Create a ByteBuffer to hold the input audio data
            ByteBuffer[] codecInputBuffers = codec.getInputBuffers();

            // Start processing the audio data
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isEOS = false;
            long presentationTimeUs = 0;

            while (!isEOS) {
                // Read audio data from the input file
                int inputBufferIndex = codec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = codecInputBuffers[inputBufferIndex];
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        // End of input file
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }

                // Retrieve encoded audio data from the codec
                int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer;
                    final int version = Build.VERSION.SDK_INT;

                    if (version >= 21) {
                        outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                    } else {
                        outputBuffer = codecOutputBuffers[outputBufferIndex];
                    }
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    // Write the encoded audio data to the output file
                    byte[] chunk = new byte[bufferInfo.size];
                    outputBuffer.get(chunk);
                    outputStream.write(chunk);

                    codec.releaseOutputBuffer(outputBufferIndex, false);
                }

                // End processing if reaching the end of the input file
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }

            // Release resources
            extractor.release();
            codec.stop();
            codec.release();
            outputStream.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Find the index of the audio track in the MediaExtractor
    private int findAudioTrackIndex(MediaExtractor extractor) {
        int trackCount = extractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                return i;
            }
        }
        return -1;
    }

    // Calculate the number of output samples based on the original and target sample rates
    private long getTargetSampleCount(int originalSampleRate, int targetSampleRate, FileDescriptor fd) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(fd);

        String durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long durationMs = Long.parseLong(durationString);

        return (durationMs * targetSampleRate) / originalSampleRate;
    }
}
