/*
 *  Copyright (c) 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc.audio;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.AudioTimestamp;
import android.media.MediaRecorder.AudioSource;
import android.media.projection.MediaProjection;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.webrtc.CalledByNative;
import org.webrtc.Logging;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStartErrorCode;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStateCallback;
import org.webrtc.audio.JavaAudioDeviceModule.SamplesReadyCallback;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CustomWebRtcAudioRecord extends WebRtcAudioRecord{

  private static final String TAG = "CustomWebRtcAudioRecordExternal";
  private int sampleRate = 0;
  private int channelCount = 0;
  private int bufferByteLength;
  private boolean started;


  public void pushAudio(byte[] audio, int length) {
    if (started) {
      byteBuffer.clear();
      byteBuffer.put(audio, 0, length);
      long captureTimeNs = 0;
      if (Build.VERSION.SDK_INT >= 240) {
        AudioTimestamp audioTimestamp = new AudioTimestamp();
        if (audioRecord.getTimestamp(audioTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC)
                == AudioRecord.SUCCESS) {
          captureTimeNs = audioTimestamp.nanoTime;
        }
      }
      nativeDataIsRecorded(nativeAudioRecord, length, captureTimeNs);
    }
  }

  @CalledByNative
  CustomWebRtcAudioRecord(Context context, AudioManager audioManager) {
    this(context, newDefaultScheduler() /* scheduler */, audioManager, DEFAULT_AUDIO_SOURCE,
        DEFAULT_AUDIO_FORMAT, null /* errorCallback */, null /* stateCallback */,
        null /* audioSamplesReadyCallback */, WebRtcAudioEffects.isAcousticEchoCancelerSupported(),
        WebRtcAudioEffects.isNoiseSuppressorSupported());
  }

  public CustomWebRtcAudioRecord(Context context, ScheduledExecutorService scheduler,
                                 AudioManager audioManager, int audioSource, int audioFormat,
                                 @Nullable AudioRecordErrorCallback errorCallback,
                                 @Nullable AudioRecordStateCallback stateCallback,
                                 @Nullable SamplesReadyCallback audioSamplesReadyCallback,
                                 boolean isAcousticEchoCancelerSupported, boolean isNoiseSuppressorSupported) {
    super(context, scheduler, audioManager, audioSource, audioFormat, errorCallback,
        stateCallback, audioSamplesReadyCallback, isAcousticEchoCancelerSupported,
        isNoiseSuppressorSupported);
  }

  @CalledByNative
  public int initRecording(int sampleRate, int channels) {
    Logging.d(TAG, "initRecording(sampleRate=" + sampleRate + ", channels=" + channels + ")");

    //we don't use audio record here
    //if (audioRecord != null) {
    //  reportWebRtcAudioRecordInitError("InitRecording called twice without StopRecording.");
    //  return -1;
    //}
    final int bytesPerFrame = channels * getBytesPerSample(audioFormat);
    final int framesPerBuffer = sampleRate / BUFFERS_PER_SECOND;

    allocateBuffer(bytesPerFrame, framesPerBuffer);
    if (!(byteBuffer.hasArray())) {
      reportWebRtcAudioRecordInitError("ByteBuffer does not have backing array.");
      return -1;
    }
    Logging.d(TAG, "byteBuffer.capacity: " + byteBuffer.capacity());
    emptyBytes = new byte[byteBuffer.capacity()];
    // Rather than passing the ByteBuffer with every callback (requiring
    // the potentially expensive GetDirectBufferAddress) we simply have the
    // the native class cache the address to the memory once.
    nativeCacheDirectBufferAddress(nativeAudioRecord, byteBuffer);

    // Get the minimum buffer size required for the successful creation of
    // an AudioRecord object, in byte units.
    // Note that this size doesn't guarantee a smooth recording under load.
    final int channelConfig = channelCountToConfiguration(channels);
    int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
    if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
      reportWebRtcAudioRecordInitError("AudioRecord.getMinBufferSize failed: " + minBufferSize);
      return -1;
    }
    Logging.d(TAG, "AudioRecord.getMinBufferSize: " + minBufferSize);

    // Use a larger buffer size than the minimum required when creating the
    // AudioRecord instance to ensure smooth recording under load. It has been
    // verified that it does not increase the actual recording latency.
    int bufferSizeInBytes = Math.max(BUFFER_SIZE_FACTOR * minBufferSize, byteBuffer.capacity());
    Logging.d(TAG, "bufferSizeInBytes: " + bufferSizeInBytes);
    /*
    No need to init audio record
    try {
      if(mediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        audioRecord = createAudioRecordOnQOrHigher(
                audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes,mediaProjection);
      }
      else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // Use the AudioRecord.Builder class on Android M (23) and above.
        // Throws IllegalArgumentException.
        audioRecord = createAudioRecordOnMOrHigher(
            audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes);
        audioSourceMatchesRecordingSessionRef.set(null);
        if (preferredDevice != null) {
          setPreferredDevice(preferredDevice);
        }
      } else {
        // Use the old AudioRecord constructor for API levels below 23.
        // Throws UnsupportedOperationException.
        audioRecord = createAudioRecordOnLowerThanM(
            audioSource, sampleRate, channelConfig, audioFormat, bufferSizeInBytes);
        audioSourceMatchesRecordingSessionRef.set(null);
      }
    } catch (IllegalArgumentException | UnsupportedOperationException e) {
      // Report of exception message is sufficient. Example: "Cannot create AudioRecord".
      reportWebRtcAudioRecordInitError(e.getMessage());
      releaseAudioResources();
      return -1;
    }
    if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
      reportWebRtcAudioRecordInitError("Creation or initialization of audio recorder failed.");
      releaseAudioResources();
      return -1;
    }
    effects.enable(audioRecord.getAudioSessionId());
    */
    //logMainParameters();
    //logMainParametersExtended();
    // Check number of active recording sessions. Should be zero but we have seen conflict cases
    // and adding a log for it can help us figure out details about conflicting sessions.
    //final int numActiveRecordingSessions =
    //      logRecordingConfigurations(audioRecord, false /* verifyAudioConfig */);
    //if (numActiveRecordingSessions != 0) {
    // Log the conflict as a warning since initialization did in fact succeed. Most likely, the
    // upcoming call to startRecording() will fail under these conditions.
    //  Logging.w(
    //        TAG, "Potential microphone conflict. Active sessions: " + numActiveRecordingSessions);
    //}
    this.sampleRate = sampleRate;
    this.channelCount = channels;
    this.bufferByteLength = emptyBytes.length;
    return framesPerBuffer;
  }

  @Nullable
  public void allocateBuffer(int bytesPerFrame, int framesPerBuffer) {
    byteBuffer = ByteBuffer.allocateDirect(bytesPerFrame * framesPerBuffer);
  }

  /**
   * Prefer a specific {@link AudioDeviceInfo} device for recording. Calling after recording starts
   * is valid but may cause a temporary interruption if the audio routing changes.
   */
  @RequiresApi(Build.VERSION_CODES.M)
  @TargetApi(Build.VERSION_CODES.M)
  public void setPreferredDevice(@Nullable AudioDeviceInfo preferredDevice) {
    Logging.d(
            TAG, "setPreferredDevice " + (preferredDevice != null ? preferredDevice.getId() : null));
    this.preferredDevice = preferredDevice;
    /*
    if (audioRecord != null) {
      if (!audioRecord.setPreferredDevice(preferredDevice)) {
        Logging.e(TAG, "setPreferredDevice failed");
      }
    }

     */
  }

  @CalledByNative
  public boolean startRecording() {
    Logging.d(TAG, "startRecording");
    /*
    assertTrue(audioRecord != null);
    assertTrue(audioThread == null);
    try {
      audioRecord.startRecording();
    } catch (IllegalStateException e) {
      reportWebRtcAudioRecordStartError(AudioRecordStartErrorCode.AUDIO_RECORD_START_EXCEPTION,
              "AudioRecord.startRecording failed: " + e.getMessage());
      return false;
    }
    if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
      reportWebRtcAudioRecordStartError(AudioRecordStartErrorCode.AUDIO_RECORD_START_STATE_MISMATCH,
              "AudioRecord.startRecording failed - incorrect state: "
                      + audioRecord.getRecordingState());
      return false;
    }
    audioThread = new AudioRecordThread("AudioRecordJavaThread");
    audioThread.start();
    scheduleLogRecordingConfigurationsTask(audioRecord);

     */
    this.started = true;
    return true;
  }

  public boolean isStarted() {
    return started;
  }

  @CalledByNative
  public synchronized boolean stopRecording() {
    Logging.d(TAG, "stopRecording");
    started = false;
    //assertTrue(audioThread != null);
    if (future != null) {
      if (!future.isDone()) {
        // Might be needed if the client calls startRecording(), stopRecording() back-to-back.
        future.cancel(true /* mayInterruptIfRunning */);
      }
      future = null;
    }

    //audioThread = null;
    effects.release();
    releaseAudioResources();
    return true;
  }

  // Releases the native AudioRecord resources.
  private void releaseAudioResources() {
    Logging.d(TAG, "releaseAudioResources");
  }


  public int getSampleRate() {
    return sampleRate;
  }

  public int getChannelCount() {
    return channelCount;
  }

  public int getAudioFormat() {
    return audioFormat;
  }

  public int getBufferByteLength() {
    return bufferByteLength;
  }

  public void setStarted(boolean started) {
    this.started = started;
  }
}
