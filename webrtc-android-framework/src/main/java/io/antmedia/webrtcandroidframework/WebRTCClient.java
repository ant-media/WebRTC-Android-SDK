/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package io.antmedia.webrtcandroidframework;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.audio.WebRtcAudioRecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.apprtc.PeerConnectionClient;
import io.antmedia.webrtcandroidframework.recorder.RecorderSurfaceDrawer;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_URLPARAMETERS;


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class WebRTCClient implements IWebRTCClient ,AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents
{


    private static final String TAG = "WebRTCClient69";


    private final CallActivity.ProxyVideoSink remoteProxyRenderer = new CallActivity.ProxyVideoSink();
    private final CallActivity.ProxyVideoSink localProxyVideoSink = new CallActivity.ProxyVideoSink();
    private final IWebRTCListener webRTCListener;
    @Nullable
    private PeerConnectionClient peerConnectionClient = null;
    @Nullable
    private AppRTCClient appRtcClient;
    @Nullable
    private AppRTCClient.SignalingParameters signalingParameters;
    @Nullable
    private AppRTCAudioManager audioManager = null;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    @Nullable
    private VideoFileRenderer videoFileRenderer;
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    private Toast logToast;
    private boolean commandLineRun;
    private boolean activityRunning;
    private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    @Nullable
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    private boolean iceConnected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs = 0;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled = false;
    private static Intent mediaProjectionPermissionResultData;
    private static int mediaProjectionPermissionResultCode;
    // True if local view is in the fullscreen renderer.
    private boolean isSwappedFeeds;

    private Activity activity;


    EglBase eglBase; // = EglBase.create();

    private String saveRemoteVideoToFile = null;
    private int videoOutWidth, videoOutHeight;
    private String errorString = null;
    private String streamMode;
    private boolean openFrontCamera = true;
    private VideoCapturer videoCapturer;
    private MediaRecorder mediaRecorder;
    private boolean recording;
    private CameraEnumerator cameraEnumerator;

    private HandlerThread handlerThread;
    private RecorderSurfaceDrawer recorderSurfaceDrawer;
    private VideoSink recorderVideoSink = new VideoSink() {
        @Override
        public void onFrame(VideoFrame videoFrame) {
            recorderSurfaceDrawer.drawTextureBuffer(videoFrame);

        }
    };


    public WebRTCClient(IWebRTCListener webRTCListener, Activity context) {
        this.webRTCListener = webRTCListener;
        this.activity = context;

    }


    @Nullable
    public SurfaceViewRenderer getPipRenderer() {
        return pipRenderer;
    }


    @Nullable
    public SurfaceViewRenderer getFullscreenRenderer() {
        return fullscreenRenderer;
    }



    @Override
    public void init(String url, String streamId, String mode, String token){

        iceConnected = false;
        signalingParameters = null;

        this.streamMode = mode;

        remoteSinks.add(remoteProxyRenderer);

        eglBase = EglBase.create();

        // Create video renderers.
        if (pipRenderer != null) {
            pipRenderer.init(eglBase.getEglBaseContext(), null);
            pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
        }

        // When saveRemoteVideoToFile is set we save the video from the remote to a file.
        if (saveRemoteVideoToFile != null) {
            try {
                videoFileRenderer = new VideoFileRenderer(
                        saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
                remoteSinks.add(videoFileRenderer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open video file for output: " + saveRemoteVideoToFile, e);
            }
        }
        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

        if (pipRenderer != null) {
            pipRenderer.setZOrderMediaOverlay(true);
            pipRenderer.setEnableHardwareScaler(true /* enabled */);
        }
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);


        //Uri roomUri = this.activity.getIntent().getData();
        if (url == null) {
            logAndToast(this.activity.getString(R.string.missing_url));
            Log.e(TAG, "Didn't get any URL in intent!");
            return;
        }

        // Get Intent parameters.
        //String roomId = this.activity.getIntent().getStringExtra(CallActivity.EXTRA_ROOMID);
        //Log.d(TAG, "Room ID: " + roomId);
        if (streamId == null || streamId.length() == 0) {
            logAndToast(this.activity.getString(R.string.missing_url));
            Log.e(TAG, "Incorrect room ID in intent!");
            return;
        }

        Intent intent = this.activity.getIntent();
        boolean loopback = intent.getBooleanExtra(CallActivity.EXTRA_LOOPBACK, false);
        boolean tracing = intent.getBooleanExtra(CallActivity.EXTRA_TRACING, false);

        int videoWidth = intent.getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
        int videoHeight = intent.getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);

        screencaptureEnabled = intent.getBooleanExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            videoWidth = displayMetrics.widthPixels;
            videoHeight = displayMetrics.heightPixels;
        }
        PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
        if (intent.getBooleanExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, false)) {
            dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(CallActivity.EXTRA_ORDERED, true),
                    intent.getIntExtra(CallActivity.EXTRA_MAX_RETRANSMITS_MS, -1),
                    intent.getIntExtra(CallActivity.EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(CallActivity.EXTRA_PROTOCOL),
                    intent.getBooleanExtra(CallActivity.EXTRA_NEGOTIATED, false), intent.getIntExtra(CallActivity.EXTRA_ID, -1));
        }

        String videoCodec = intent.getStringExtra(CallActivity.EXTRA_VIDEOCODEC);
        if (videoCodec == null) {
            videoCodec = this.activity.getString(R.string.pref_videocodec_default);
        }
        int videoStartBitrate = this.activity.getIntent().getIntExtra(CallActivity.EXTRA_VIDEO_BITRATE, 0);

        if (videoStartBitrate == 0) {
            videoStartBitrate = Integer.parseInt(this.activity.getString(R.string.pref_maxvideobitratevalue_default));
        }

        int audioStartBitrate = this.activity.getIntent().getIntExtra(CallActivity.EXTRA_AUDIO_BITRATE, 0);
        if (audioStartBitrate == 0) {
            audioStartBitrate = Integer.parseInt(this.activity.getString(R.string.pref_startaudiobitratevalue_default));
        }

        boolean videoCallEnabled = intent.getBooleanExtra(CallActivity.EXTRA_VIDEO_CALL, true);
        boolean audioCallEnabled = true;
        if (mode.equals(MODE_PLAY)) {
            videoCallEnabled = false;
            audioCallEnabled = false;
        }

        peerConnectionParameters =
                new PeerConnectionClient.PeerConnectionParameters(videoCallEnabled, loopback,
                        tracing, videoWidth, videoHeight, intent.getIntExtra(CallActivity.EXTRA_VIDEO_FPS, 0),
                        videoStartBitrate, videoCodec,
                        intent.getBooleanExtra(CallActivity.EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(CallActivity.EXTRA_FLEXFEC_ENABLED, false),
                        audioStartBitrate, intent.getStringExtra(CallActivity.EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_ENABLE_RTCEVENTLOG, false),
                        intent.getBooleanExtra(CallActivity.EXTRA_USE_LEGACY_AUDIO_DEVICE, false), dataChannelParameters,
                        audioCallEnabled);
        commandLineRun = intent.getBooleanExtra(CallActivity.EXTRA_CMDLINE, false);
        int runTimeMs = intent.getIntExtra(CallActivity.EXTRA_RUNTIME, 0);

        Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

        // Create connection client. Use DirectRTCClient if room name is an IP otherwise use the
        // standard WebSocketRTCClient.
        //if (loopback || !DirectRTCClient.IP_PATTERN.matcher(roomId).matches()) {
        //  appRtcClient = new WebSocketRTCClient(this);
        //} else {
        //  Log.i(TAG, "Using DirectRTCClient because room name looks like an IP.");
        //  appRtcClient = new DirectRTCClient(this);
        //}



        // Create connection parameters.
        String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);
        roomConnectionParameters =
                new AppRTCClient.RoomConnectionParameters(url, streamId, loopback, urlParameters, mode ,token);


        // For command line execution run connection for <runTimeMs> and exit.
        if (commandLineRun && runTimeMs > 0) {
            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, runTimeMs);
        }



        // Create peer connection client.
        peerConnectionClient = new PeerConnectionClient(
                this.activity.getApplicationContext(), eglBase, peerConnectionParameters, WebRTCClient.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (loopback) {
            options.networkIgnoreMask = 0;
        }
        peerConnectionClient.createPeerConnectionFactory(options);


        videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }

        peerConnectionClient.init(videoCapturer, localProxyVideoSink);

    }

    /**
     * This method can be callable after init method
     * @param fullFilePath
     * @param videoBitrate
     * @param audioBitrate
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startRecording(String fullFilePath, int videoBitrate, int audioBitrate) {

        if (peerConnectionClient.getSurfaceWidth() == 0 || peerConnectionClient.getSurfaceHeight() == 0) {
            throw new IllegalArgumentException("Surface is not initialized. Surface width or height is zero. " +
                    "Please try again a few milliseconds later next time");
        }

        PeerConnectionClient.getExecutor().execute(()-> {
            File file;
            if (fullFilePath != null) {
                file = new File(fullFilePath);
            }
            else {
                file = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES), "record" + System.currentTimeMillis() + ".mp4");
            }

            handlerThread = new HandlerThread("recorder surface handler");
            handlerThread.start();
            recorderSurfaceDrawer = new RecorderSurfaceDrawer(getEglBase(), handlerThread.getLooper(), videoBitrate, file, peerConnectionClient.getWebRtcAudioRecord()
                    ,peerConnectionClient.getSampleRate(), 1);


            recorderSurfaceDrawer.startRecording(peerConnectionClient.getSurfaceWidth(), peerConnectionClient.getSurfaceHeight());

            peerConnectionClient.getLocalVideoTrack().addSink(recorderVideoSink);

            recording = true;
        });


    }

    public void stopRecording() {
        PeerConnectionClient.getExecutor().execute(()-> {
            peerConnectionClient.getLocalVideoTrack().removeSink(recorderVideoSink);
            recorderSurfaceDrawer.stopRecording();
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
            }

            if (recorderSurfaceDrawer != null) {
                recorderSurfaceDrawer.release();
            }

            if (handlerThread != null) {
                handlerThread.quitSafely();
            }
            recording = false;
        });
    }

    public void startStream() {
        appRtcClient = new WebSocketRTCAntMediaClient(this);
        if (screencaptureEnabled) {
            startScreenCapture();
        } else {
            startCall();
        }
    }

    @TargetApi(17)
    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) this.activity.getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    @TargetApi(21)
    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) this.activity.getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        this.activity.startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CallActivity.CAPTURE_PERMISSION_REQUEST_CODE);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CallActivity.CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;
        startCall();
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this.activity) && this.activity.getIntent().getBooleanExtra(CallActivity.EXTRA_CAMERA2, true);
    }

  private boolean captureToTexture() {
    return this.activity.getIntent().getBooleanExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true);
  }

    public void setOpenFrontCamera(boolean openFrontCamera) {
        this.openFrontCamera = openFrontCamera;
    }

    private @Nullable VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();


        if (openFrontCamera) {
            // First, try to find front facing camera
            Logging.d(TAG, "Looking for front facing cameras.");
            for (String deviceName : deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    Logging.d(TAG, "Creating front facing camera capturer.");
                    VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                    if (videoCapturer != null) {
                        return videoCapturer;
                    }
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras to open again.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    @TargetApi(21)
    private @Nullable VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(
                mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError("User revoked permission to capture the screen.");
            }
        });
    }

    @Override
    public void stopVideoSource() {
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.stopVideoSource();
        }

    }

    @Override
    public void startVideoSource() {
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null && !screencaptureEnabled) {
            peerConnectionClient.startVideoSource();
        }

    }


    @Override
    public void stopStream() {
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        Log.i(TAG, "onCallHangUp");
        disconnect();
    }

    @Override
    public void switchCamera() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onCameraSwitch() {
        switchCamera();
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        fullscreenRenderer.setScalingType(scalingType);
    }


    public void switchVideoScaling(RendererCommon.ScalingType scalingType) {
        onVideoScalingSwitch(scalingType);
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    @Override
    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    @Override
    public boolean toggleMic() {
        return onToggleMic();
    }

    private void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        logAndToast(this.activity.getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
        appRtcClient.connectToRoom(roomConnectionParameters);


        if (peerConnectionParameters.audioCallEnabled) {
            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(this.activity.getApplicationContext());
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Starting the audio manager...");
            audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
                // This method will be called each time the number of available audio
                // devices has changed.
                @Override
                public void onAudioDeviceChanged(
                        AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
                    onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
                }
            });

        }
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        peerConnectionClient.enableStatsEvents(true, CallActivity.STAT_CALLBACK_PERIOD);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        if (appRtcClient != null) {
            appRtcClient.disconnectFromRoom();
            appRtcClient = null;
        }
        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            videoFileRenderer = null;
        }
        if (fullscreenRenderer != null) {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
    }

    private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this.activity)
                    .setTitle(this.activity.getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    disconnect();
                                }
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        //logToast = Toast.makeText(this.activity, msg, Toast.LENGTH_SHORT);
        //logToast.show();
    }

    private void reportError(final String description) {
        this.activity.runOnUiThread(() -> {

            if (!isError) {
                isError = true;
                errorString = description;

                disconnectWithErrorMessage(description);
                if (webRTCListener != null) {
                    webRTCListener.onError(description);
                }
            }

        });
    }

  public void setCameraEnumerator(CameraEnumerator cameraEnumerator) {

    this.cameraEnumerator = cameraEnumerator;
  }

  private @Nullable VideoCapturer createVideoCapturer() {
    final VideoCapturer videoCapturer;
    String videoFileAsCamera = this.activity.getIntent().getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);
    if (videoFileAsCamera != null) {
      try {
        videoCapturer = new FileVideoCapturer(videoFileAsCamera);
      } catch (IOException e) {
        reportError("Failed to open video file for emulated camera");
        return null;
      }
    } else if (screencaptureEnabled) {
      return createScreenCapturer();
    }

    else if (cameraEnumerator != null)
    {
      Logging.d(TAG, "Creating capturer using USB  Camera .");
      videoCapturer = createCameraCapturer(cameraEnumerator);
    }
    else if (useCamera2())
    {
      if (!captureToTexture()) {
        reportError(this.activity.getString(R.string.camera2_texture_only_error));
        return null;
      }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this.activity));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    public void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        if (this.streamMode.equals(MODE_PUBLISH)) {
            localProxyVideoSink.setTarget(fullscreenRenderer);
        }
        else if (this.streamMode.equals(MODE_PLAY)) {
            remoteProxyRenderer.setTarget(fullscreenRenderer);
        }
        else {
            this.isSwappedFeeds = isSwappedFeeds;
            localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
            remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
            fullscreenRenderer.setMirror(isSwappedFeeds);
            pipRenderer.setMirror(!isSwappedFeeds);
        }
    }

    @Override
    public void setVideoRenderers(SurfaceViewRenderer pipRenderer, SurfaceViewRenderer fullscreenRenderer) {
        this.pipRenderer = pipRenderer;
        this.fullscreenRenderer = fullscreenRenderer;
    }

    @Override
    public String getError() {
        return errorString;
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParameters = params;
        logAndToast("Creating peer connection, delay=" + delta + "ms");
        VideoCapturer videoCapturer = null;
        if (peerConnectionParameters.videoCallEnabled) {
            videoCapturer = createVideoCapturer();
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.createPeerConnection(
                    localProxyVideoSink, remoteSinks, videoCapturer, signalingParameters);

            if (signalingParameters.initiator) {
                logAndToast("Creating OFFER...");
                // Create offer. Offer SDP will be sent to answering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createOffer();
            } else {
                if (params.offerSdp != null) {
                    peerConnectionClient.setRemoteDescription(params.offerSdp);
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
                if (params.iceCandidates != null) {
                    // Add remote ICE candidates from room.
                    for (IceCandidate iceCandidate : params.iceCandidates) {
                        peerConnectionClient.addRemoteIceCandidate(iceCandidate);
                    }
                }
            }
        }
    }

    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                peerConnectionClient.setRemoteDescription(sdp);
                if (!signalingParameters.initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        });
    }

    @Override
    public void onChannelClose(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                if (webRTCListener != null) {
                    webRTCListener.onSignalChannelClosed(code);
                }
                disconnect();
            }
        });
    }

    @Override
    public void onChannelError(final String description) {
        reportError(description);
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                    if (signalingParameters.initiator) {
                        appRtcClient.sendOfferSdp(sdp);
                    } else {
                        appRtcClient.sendAnswerSdp(sdp);
                    }
                }
                if (peerConnectionParameters.videoMaxBitrate > 0) {
                    Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                    peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
                iceConnected = true;
                callConnected();

                if (webRTCListener != null) {
                    webRTCListener.onConnected();
                }
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
                iceConnected = false;
                disconnect();

            }
        });
    }

    @Override
    public void onPeerConnectionClosed() {}

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports) {
        this.activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isError && iceConnected) {
                    //hudFragment.updateEncoderStatistics(reports);
                    Log.i(TAG, reports.toString());
                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }

    @Override
    public void onPublishFinished() {

        this.activity.runOnUiThread(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPublishFinished();
            }
        });

    }

    @Override
    public void onPlayFinished() {
        this.activity.runOnUiThread(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPlayFinished();
            }
        });

    }

    @Override
    public void onPublishStarted() {
        this.activity.runOnUiThread(() -> {

            if (webRTCListener != null) {
                webRTCListener.onPublishStarted();
            }

        });

    }

    @Override
    public void onPlayStarted() {
        this.activity.runOnUiThread(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPlayStarted();
            }
        });

    }

    @Override
    public void noStreamExistsToPlay() {
        this.activity.runOnUiThread(() -> {
            if (webRTCListener != null) {
                webRTCListener.noStreamExistsToPlay();
            }
        });
    }

    @Override
    public void onDisconnected() {
        this.activity.runOnUiThread(() -> {
            if (webRTCListener != null) {
                webRTCListener.onDisconnected();
            }
        });
    }

    public boolean isRecording() {
        return recording;
    }

    public EglBase getEglBase() {
        return eglBase;
    }
}
