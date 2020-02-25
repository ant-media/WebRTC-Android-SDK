/*
 *  Copyright 2013 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.content.Context;
import java.util.List;

// Base interface for all VideoCapturers to implement.
public interface VideoCapturer {
  // Interface used for providing callbacks to an observer.
  @Deprecated
  public interface CapturerObserver {
    // Notify if the camera have been started successfully or not.
    // Called on a Java thread owned by VideoCapturer.
    void onCapturerStarted(boolean success);
    void onCapturerStopped();

    // Delivers a captured frame. Called on a Java thread owned by VideoCapturer.
    void onFrameCaptured(VideoFrame frame);
  }

  /** Deprecated, implementations should be update to implement the version below. */
  @Deprecated
  default void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
                          CapturerObserver capturerObserver) {
    throw new UnsupportedOperationException("Not implemented.");
  }

  /**
   * This function is used to initialize the camera thread, the android application context, and the
   * capture observer. It will be called only once and before any startCapture() request. The
   * camera thread is guaranteed to be valid until dispose() is called. If the VideoCapturer wants
   * to deliver texture frames, it should do this by rendering on the SurfaceTexture in
   * {@code surfaceTextureHelper}, register itself as a listener, and forward the frames to
   * CapturerObserver.onFrameCaptured(). The caller still has ownership of {@code
   * surfaceTextureHelper} and is responsible for making sure surfaceTextureHelper.dispose() is
   * called. This also means that the caller can reuse the SurfaceTextureHelper to initialize a new
   * VideoCapturer once the previous VideoCapturer has been disposed.
   */
  // Our version of clang format doesn't understand default and messes up.
  // clang-format off
  default void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext,
                          org.webrtc.CapturerObserver capturerObserver) {
    initialize(surfaceTextureHelper, applicationContext, (CapturerObserver) capturerObserver);
  }

  /**
   * Start capturing frames in a format that is as close as possible to {@code width x height} and
   * {@code framerate}.
   */
  void startCapture(int width, int height, int framerate);

  /**
   * Stop capturing. This function should block until capture is actually stopped.
   */
  void stopCapture() throws InterruptedException;

  void changeCaptureFormat(int width, int height, int framerate);

  /**
   * Perform any final cleanup here. No more capturing will be done after this call.
   */
  void dispose();

  /**
   * @return true if-and-only-if this is a screen capturer.
   */
  boolean isScreencast();
}
