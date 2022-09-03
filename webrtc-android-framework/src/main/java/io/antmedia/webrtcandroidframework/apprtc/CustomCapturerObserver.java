/*
 * This class is used to get frames from the video captures,
 * provide the frames to the application layer via IFrameListener
 * and feed the processed frames to the real capturer back
 *
 */

package io.antmedia.webrtcandroidframework.apprtc;

import org.webrtc.CapturerObserver;
import org.webrtc.VideoFrame;

class CustomCapturerObserver implements CapturerObserver {

  private CapturerObserver realCapturerObserver;

  private IFrameListener frameListener;

  public CustomCapturerObserver(CapturerObserver realCapturerObserver) {
    this.realCapturerObserver = realCapturerObserver;
  }

  @Override
  public void onCapturerStarted(boolean success) { realCapturerObserver.onCapturerStarted(success); }

  @Override
  public void onCapturerStopped() {
    realCapturerObserver.onCapturerStopped();
  }

  @Override
  public void onFrameCaptured(VideoFrame frame) {
    VideoFrame sendingFrame = frame;
    if(frameListener != null) {
      sendingFrame = frameListener.onFrame(frame);
    }
    if(sendingFrame != null) {
      realCapturerObserver.onFrameCaptured(sendingFrame);
    }
  }

  public void setFrameListener(IFrameListener frameListener) {
    this.frameListener = frameListener;
  }
}
