/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package io.antmedia.webrtcandroidframework.core;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class ScreenCapturer implements VideoCapturer, VideoSink {
  private static final int DISPLAY_FLAGS =
          DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
  // DPI for VirtualDisplay, does not seem to matter for us.
  public static final int VIRTUAL_DISPLAY_DPI = 400;
  private int width;
  private int height;
  @Nullable private VirtualDisplay virtualDisplay;
  @Nullable private SurfaceTextureHelper surfaceTextureHelper;
  @Nullable private CapturerObserver capturerObserver;
  private long numCapturedFrames;
  @Nullable private MediaProjection mediaProjection;
  private boolean isDisposed;
  public int deviceRotation = 0;
  private static final String TAG =  ScreenCapturer.class.getSimpleName();
  private MediaProjection.Callback mediaProjectionCallback;

  public ScreenCapturer(MediaProjection mediaProjection, MediaProjection.Callback callback) {
    this.mediaProjection = mediaProjection;
    this.mediaProjectionCallback = callback;
  }

  public boolean isDisposed() {
    return isDisposed;
  }

  private void checkNotDisposed() {
    if (isDisposed) {
      throw new RuntimeException("capturer is disposed.");
    }
  }

  @Nullable
  public MediaProjection getMediaProjection() {
    return mediaProjection;
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void initialize(final SurfaceTextureHelper surfaceTextureHelper,
                                      final Context applicationContext, final CapturerObserver capturerObserver) {
    checkNotDisposed();

    if (capturerObserver == null) {
      throw new RuntimeException("capturerObserver not set.");
    }
    this.capturerObserver = capturerObserver;

    if (surfaceTextureHelper == null) {
      throw new RuntimeException("surfaceTextureHelper not set.");
    }
    this.surfaceTextureHelper = surfaceTextureHelper;
  }

  public void setMediaProjection(@Nullable MediaProjection mediaProjection) {
    this.mediaProjection = mediaProjection;
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void startCapture(
          final int width, final int height, final int ignoredFramerate) {
    checkNotDisposed();

    this.width = width;
    this.height = height;


    // Let MediaProjection callback use the SurfaceTextureHelper thread.
    mediaProjection.registerCallback(mediaProjectionCallback, surfaceTextureHelper.getHandler());

    createVirtualDisplay();
    capturerObserver.onCapturerStarted(true);
    surfaceTextureHelper.startListening(ScreenCapturer.this);
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void stopCapture() {
    checkNotDisposed();
    ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), new Runnable() {
      @Override
      public void run() {
        surfaceTextureHelper.stopListening();
        capturerObserver.onCapturerStopped();

        if (virtualDisplay != null) {
          virtualDisplay.release();
          virtualDisplay = null;
        }

        if (mediaProjection != null) {
          // Unregister the callback before stopping, otherwise the callback recursively
          // calls this method.
          mediaProjection.unregisterCallback(mediaProjectionCallback);
          mediaProjection.stop();
          mediaProjection = null;
        }
      }
    });
  }

  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void dispose() {
    Log.i(TAG, "ScreenCapturer is disposed");
    isDisposed = true;
  }

  /**
   * Changes output video format. This method can be used to scale the output
   * video, or to change orientation when the captured screen is rotated for example.
   *
   * @param width new output video width
   * @param height new output video height
   * @param ignoredFramerate ignored
   */
  @Override
  // TODO(bugs.webrtc.org/8491): Remove NoSynchronizedMethodCheck suppression.
  @SuppressWarnings("NoSynchronizedMethodCheck")
  public synchronized void changeCaptureFormat(
          final int width, final int height, final int ignoredFramerate) {
    checkNotDisposed();

    this.width = width;
    this.height = height;

    if (virtualDisplay == null) {
      // Capturer is stopped, the virtual display will be created in startCaptuer().
      return;
    }

    // Create a new virtual display on the surfaceTextureHelper thread to avoid interference
    // with frame processing, which happens on the same thread (we serialize events by running
    // them on the same thread).
    ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), new Runnable() {
      @Override
      public void run() {
        virtualDisplay.release();
        createVirtualDisplay();
      }
    });
  }

  private void createVirtualDisplay() {
    surfaceTextureHelper.setTextureSize(width, height);
    virtualDisplay = mediaProjection.createVirtualDisplay("WebRTC_ScreenCapture", width, height,
            VIRTUAL_DISPLAY_DPI, DISPLAY_FLAGS, new Surface(surfaceTextureHelper.getSurfaceTexture()),
            null /* callback */, null /* callback handler */);
  }

  public void rotateScreen(int rotation) {
    if (deviceRotation != rotation) {
      Log.w("Rotation", "onFrame: " + rotation);
      deviceRotation = rotation;

      if (deviceRotation == 0) {
        virtualDisplay.resize(width, height, VIRTUAL_DISPLAY_DPI);
        surfaceTextureHelper.setTextureSize(width, height);
      } else if (deviceRotation == 180) {
        // 180 degree is not supported by MediaProjection
      } else {
        virtualDisplay.resize(height, width, VIRTUAL_DISPLAY_DPI);
        surfaceTextureHelper.setTextureSize(height, width);
      }
    }
  }

  // This is called on the internal looper thread of {@Code SurfaceTextureHelper}.
  @Override
  public void onFrame(VideoFrame frame) {
    numCapturedFrames++;
    Log.v(TAG, "Frame received " + numCapturedFrames);
    capturerObserver.onFrameCaptured(frame);
  }

  @Override
  public boolean isScreencast() {
    return true;
  }

  public long getNumCapturedFrames() {
    return numCapturedFrames;
  }


  public void setVirtualDisplay(VirtualDisplay virtualDisplay) {
    this.virtualDisplay = virtualDisplay;
  }

  public void setSurfaceTextureHelper(SurfaceTextureHelper surfaceTextureHelper) {
    this.surfaceTextureHelper = surfaceTextureHelper;
  }

  public void setCapturerObserver(CapturerObserver capturerObserver) {
    this.capturerObserver = capturerObserver;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public MediaProjection.Callback getMediaProjectionCallback() {
    return mediaProjectionCallback;
  }
}
