package io.antmedia.webrtcandroidframework.core;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.webrtc.CapturerObserver;
import org.webrtc.JavaI420Buffer;
import org.webrtc.Logging;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class CustomVideoCapturer implements VideoCapturer {

  private final static String TAG = "CustomVideoCapturer";
  private CapturerObserver capturerObserver;

  public SurfaceTextureHelper surfaceTextureHelper;


  public void writeFrame(VideoFrame videoFrame) {
    capturerObserver.onFrameCaptured(videoFrame);
    videoFrame.release();
  }

  @Override
  public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
                         CapturerObserver capturerObserver) {
    this.surfaceTextureHelper = surfaceTextureHelper;
    this.capturerObserver = capturerObserver;
  }

  @Override
  public void startCapture(int width, int height, int framerate) {
    surfaceTextureHelper.setTextureSize(height, width);

    surfaceTextureHelper.startListening(new VideoSink() {
      @Override
      public void onFrame(VideoFrame frame) {

        capturerObserver.onFrameCaptured(frame);
        Log.i("CustomVideoCapturer****", "width:"+width+" height:"+height);

      }
    });
  }

  @Override
  public void stopCapture() throws InterruptedException {

  }

  @Override
  public void changeCaptureFormat(int width, int height, int framerate) {
    // Empty on purpose
  }

  @Override
  public void dispose() {

  }

  @Override
  public boolean isScreencast() {
    return false;
  }

  public SurfaceTextureHelper getSurfaceTextureHelper() {
    return surfaceTextureHelper;
  }
}
