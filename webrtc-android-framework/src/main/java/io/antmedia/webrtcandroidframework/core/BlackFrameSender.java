package io.antmedia.webrtcandroidframework.core;

import android.os.SystemClock;

import org.webrtc.*;

import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BlackFrameSender {
    public static int BLACK_FRAME_SENDING_FREQUENCY = 3000;
    private CustomVideoCapturer videoCapturer;
    private ScheduledExecutorService executorService;
    private boolean running;

    public BlackFrameSender(CustomVideoCapturer videoCapturer) {
        this.videoCapturer = videoCapturer;
    }

    public void start(){
        if(executorService == null){
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleWithFixedDelay(() -> {

                try{
                    VideoFrame blackFrame = createBlackFrame();
                    videoCapturer.writeFrame(blackFrame);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }, 0, BLACK_FRAME_SENDING_FREQUENCY, TimeUnit.MILLISECONDS);
            running = true;
        }


    }
    public void stop(){
        if(executorService != null){
            executorService.shutdown();
            executorService = null;
            running = false;
        }
    }

    public boolean isRunning(){
        return running;
    }

    public VideoFrame createBlackFrame() {
        int width = 240;
        int height = 426;

        int ySize = width * height;
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        int uvSize = uvWidth * uvHeight;

        ByteBuffer yBuffer = ByteBuffer.allocateDirect(ySize);
        ByteBuffer uBuffer = ByteBuffer.allocateDirect(uvSize);
        ByteBuffer vBuffer = ByteBuffer.allocateDirect(uvSize);

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

        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

        return new VideoFrame(JavaI420Buffer.wrap(width, height, yBuffer, width, uBuffer, uvWidth, vBuffer, uvWidth, null), 0, captureTimeNs);
    }


}