package io.antmedia.webrtcandroidframework.recorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by faraklit on 03.02.2016.
 */
public class AudioEncoder extends Thread {

    private static final String TAG = AudioEncoder.class.getSimpleName();
    final int TIMEOUT_USEC = 10000;

    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    private ByteBuffer[] mAudioInputBuffers;
    private ByteBuffer[] mAudioOutputBuffers;
    private MediaCodec mAudioEncoder;
    private MediaMuxer mMuxerHandler;
    private Map<Integer, Object> reservedBuffers = new HashMap<Integer, Object>();
    private static int roundTimes;
    private static long roundOffset;
    private int trackIndex = -1;
    private boolean mMuxerStarted;
    private volatile boolean threadIsAlive = false;


    /**
     *
     * @param sampleRate recommended setting is 44100
     * @param channelCount recommended setting is 1
     * @param bitrate  recommended setting is 64000
     * @return
     */
    public boolean startAudioEncoder(int sampleRate, int channelCount, int bitrate, int maxInputSize, MediaMuxer muxerHandler) {
        mMuxerStarted = false;
        mMuxerHandler = muxerHandler;
        MediaFormat audioFormat = MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, sampleRate, channelCount);
        audioFormat.setInteger(
                MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize);


        try {
            mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
            mAudioEncoder.configure(
                    audioFormat,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);

            mAudioEncoder.start();
            mAudioInputBuffers = mAudioEncoder.getInputBuffers();
            mAudioOutputBuffers = mAudioEncoder.getOutputBuffers();
            start();
            return true;
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
            mAudioEncoder = null;
        }
        return false;
    }
    /*
     * @param data
     * @param pts presentation time stamp in microseconds
     */
    public void encodeAudio(byte[] data, int length, long pts) {
        if (mAudioEncoder == null) {
            return;
        }
        int bufferRemaining;

        for (int i = 0; i < 3 ; i++) {
            int inputBufferId = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferId >= 0) {
                ByteBuffer inputBuf = mAudioInputBuffers[inputBufferId];
                inputBuf.clear();
                bufferRemaining = inputBuf.remaining();
                if (bufferRemaining < length) {
                    inputBuf.put(data, 0, bufferRemaining);
                } else {
                    inputBuf.put(data, 0, length);
                }

                //length equals to inputbuffer position
                mAudioEncoder.queueInputBuffer(inputBufferId, 0, inputBuf.position(), pts, 0);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void stopEncoding()
    {
        Log.d("audio encoder", "starting stop audio encoding...");

        //////////////////////// Stop Signal For Audio Encoder ///////////////////////////
        for (int i = 0; i < 3; i++) {
            if (mAudioEncoder == null) {
                break;
            }

            int inputBufferId = mAudioEncoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferId >= 0) {
                Log.d("audio encoder", "inputBuffer is greater than 0");

                mAudioEncoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                break;
            }

            /**
             * This waiting time ensures input buffer to be empty.
             */
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        while (threadIsAlive) {
            Log.d("audio encoder", "thread a live");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Log.d("audio encoder", "exiting stop audio encoding...");

    }

    private static long previousPresentationTimeUs;
    public static long getUnsignedInt(long x) {
        return x & 0xffffffffL;
    }

    public void run() {

        threadIsAlive = true;
        //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        roundTimes = 0;
        roundOffset = 0;
        previousPresentationTimeUs = 0;

        for (;;) {
            /******************** AUDIO **************************/
            int outputBufferId = mAudioEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
            if (outputBufferId >= 0) {
                // mVideoOutputBuffers[outputBufferId] is ready to be processed or rendered.
                ByteBuffer encodedData = mAudioOutputBuffers[outputBufferId];
                if (encodedData == null) {
                    System.out.println(" encoded data null audio");
                    continue;
                }
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {


                    trackIndex = mMuxerHandler.addTrack(mAudioEncoder.getOutputFormat());
                    Log.i(TAG, "Audio track added " + trackIndex);
                    info.size = 0;
                }

                if (info.size != 0) {
                    if (mMuxerStarted) {
                        encodedData.position(info.offset);
                        encodedData.limit(info.offset + info.size);
                        // first packet is 2 byte audio specific config

                        //there is a bug in audio encoder- it starts to give negative values after integer max size is exceeded
                        //so getUnSignedInt need to be used

                        info.presentationTimeUs = getPTSUs();
                        mMuxerHandler.writeSampleData(trackIndex, encodedData, info);
                        prevOutputPTSUs = info.presentationTimeUs;

                        previousPresentationTimeUs = info.presentationTimeUs;
                    } else {
                        Log.i(TAG, "Muxer not started");
                    }
                }

                mAudioEncoder.releaseOutputBuffer(outputBufferId, false);
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                mAudioOutputBuffers = mAudioEncoder.getOutputBuffers();
                trackIndex = mMuxerHandler.addTrack(mAudioEncoder.getOutputFormat());
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            }
            else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {

            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                //end of stream
                // do not break here, let video break the loop
                reservedBuffers.clear();
                break;
            }
            else {

            }
        }
        release();

        threadIsAlive = false;
        Log.d("audio encoder", "leaving audio writer thread");
    }


    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }


    public byte[] getBuffer(int size, int lastSentFrameTimestamp, int currentTimeStamp)
    {
        /**
         * how does it work?
         * we put byte array with their timestamp value to a hash map
         * when there is a new output buffer array, we check the last frame timestamp of mediamuxer
         * if the byte buffer timestamp is less than the value of last frame timestamp of mediamuxer
         * it means that we can use that byte buffer again because it is already written to network
         */
        Iterator<Map.Entry<Integer, Object>> iterator = reservedBuffers.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<Integer, Object> next = iterator.next();
            if (next.getKey() <= lastSentFrameTimestamp)
            {
                // it means this frame is sent
                byte[] value = (byte[]) next.getValue();
                iterator.remove();
                if (value.length >= size)
                {
                    reservedBuffers.put(currentTimeStamp, value);
                    return value;
                }
                // if byte array length is not bigger than requested size,
                // we give this array to soft hands of GC
            }
        }

        // no eligible data found, create a new byte
        byte[] data = new byte[size];
        reservedBuffers.put(currentTimeStamp, data);
        return data;
    }

    private void release()
    {
        try {
            if (mAudioEncoder != null) {
                mAudioEncoder.stop();
                mAudioEncoder.release();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setMuxerStarted(boolean muxerStarted) {
        this.mMuxerStarted = muxerStarted;
    }

    public int getTrackIndex() {
        return trackIndex;
    }
}
