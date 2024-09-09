package io.antmedia.webrtcandroidframework.core;

import android.os.SystemClock;

import org.webrtc.*;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BlackFrameSender {
    public static int BLACK_FRAME_SENDING_FREQUENCY_MS = 3000;
    private CustomVideoCapturer videoCapturer;
    private ScheduledExecutorService executorService;
    private boolean running;
    private int frameWidth = 240;
    private int frameHeight = 426;
    private ByteBuffer yBuffer;
    private ByteBuffer uBuffer;
    private ByteBuffer vBuffer;

    public BlackFrameSender(CustomVideoCapturer videoCapturer) {
        this.videoCapturer = videoCapturer;
        allocateBlackFrameBuffers();
    }

    public void start(){
        if(executorService == null){
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleWithFixedDelay(() -> {

                try{
                    videoCapturer.writeFrame(createBlackFrame());
                }catch (Exception e){
                    e.printStackTrace();
                }

            }, 0, BLACK_FRAME_SENDING_FREQUENCY_MS, TimeUnit.MILLISECONDS);
            running = true;
        }


    }
    public void stop(){
        if(executorService != null){
            executorService.shutdown();
            executorService = null;
            running = false;
            releaseByteBuffers();
        }
    }

    public void releaseByteBuffers(){
        if (yBuffer != null) {
            yBuffer.clear();
            yBuffer = null;
        }
        if (uBuffer != null) {
            uBuffer.clear();
            uBuffer = null;
        }
        if (vBuffer != null) {
            vBuffer.clear();
            vBuffer = null;
        }
    }

    public boolean isRunning(){
        return running;
    }

    //capture time needs to be incremented. Otherwise server does not ingest the stream.
    public VideoFrame createBlackFrame() {
        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
        return new VideoFrame(createBlackFrameBuffer(), 0, captureTimeNs);
    }

    public void allocateBlackFrameBuffers(){

        int ySize = frameWidth * frameHeight;
        int uvWidth = frameWidth / 2;
        int uvHeight = frameHeight / 2;
        int uvSize = uvWidth * uvHeight;

         yBuffer = ByteBuffer.allocateDirect(ySize);
         uBuffer = ByteBuffer.allocateDirect(uvSize);
         vBuffer = ByteBuffer.allocateDirect(uvSize);

        for (int i = 0; i < ySize; i++) {
            yBuffer.put((byte) 0);
        }

        for (int i = 0; i < uvSize; i++) {
            uBuffer.put((byte) 128);
            vBuffer.put((byte) 128);
        }

        yBuffer.flip();
        uBuffer.flip();
        vBuffer.flip();

    }

    public JavaI420Buffer createBlackFrameBuffer(){
        int uvWidth = frameWidth / 2;
        return JavaI420Buffer.wrap(frameWidth, frameHeight, yBuffer, frameWidth, uBuffer, uvWidth, vBuffer, uvWidth, null);
    }

}