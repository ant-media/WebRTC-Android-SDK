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
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.webrtc.AddIceObserver;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.IceCandidateErrorEvent;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStatsReport;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.CustomWebRtcAudioRecord;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.apprtc.RtcEventLog;


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class WebRTCClient implements IWebRTCClient, AntMediaSignallingEvents, IDataChannelMessageSender {
    private static final String TAG = "WebRTCClient";
    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_SCREEN = "SCREEN";
    public static final String SOURCE_FRONT = "FRONT";
    public static final String SOURCE_REAR = "REAR";
    public static final String SOURCE_CUSTOM = "CUSTOM";


    public static final String ERROR_USER_REVOKED_CAPTURE_SCREEN_PERMISSION = "USER_REVOKED_CAPTURE_SCREEN_PERMISSION";
    public static final String VIDEO_ROTATION_EXT_LINE = "a=extmap:3 urn:3gpp:video-orientation\r\n";

    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    //private final List<CallActivity.ProxyVideoSink> remoteProxyRendererList = new ArrayList<>();
    private final IWebRTCListener webRTCListener;
    private final Handler mainHandler;
    @Nullable
    public AppRTCAudioManager audioManager = null;
    @Nullable
    private SurfaceViewRenderer pipRenderer;
    @Nullable
    private SurfaceViewRenderer fullscreenRenderer;
    @Nullable
    private VideoFileRenderer videoFileRenderer;
    private List<VideoSink> remoteSinks = new ArrayList<>();
    private boolean isError;
    private final long callStartedTimeMs = 0;
    private boolean micEnabled = true;
    private boolean screencaptureEnabled = false;
    private static Intent mediaProjectionPermissionResultData;
    private int mediaProjectionPermissionResultCode;
    private final Context context;
    private EglBase eglBase;
    private final String saveRemoteVideoToFile = null;
    private String errorString = null;
    private String streamMode;
    private boolean openFrontCamera = true;
    private boolean streamStoppedByUser = false;

    private boolean reconnectionInProgress = false;

    private boolean autoPlayTracks = false;
    private boolean renderersInitiated = false;
    private boolean checkStreamIdValidity = true;

    private boolean renderersProvidedAtStart = false;
    //dynamic means created after stream started on the fly
    private ConcurrentLinkedQueue<SurfaceViewRenderer> dynamicRenderers = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<VideoSink>  dynamicRemoteSinks = new ConcurrentLinkedQueue<>();
    private boolean streamStarted = false;
    private long TRACK_CHECK_PERIDOD_MS = 3000;
    private boolean waitingForPlay = false;

    private int inputSampleRate = 48000;
    private boolean stereoInput = false;
    private int audioInputFormat;
    private boolean customAudioFeed;

    private boolean customCapturerEnabled = false;

    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private Intent intent = new Intent();
    private Handler handler = new Handler();
    private WebSocketHandler wsHandler;
    private final String stunServerUri = "stun:stun1.l.google.com:19302";
    private final ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private boolean videoOn = true;
    private boolean audioOn = true;
    private List<SurfaceViewRenderer> remoteRendererList = null;
    @Nullable
    private IDataChannelObserver dataChannelObserver;

    private String initialStreamId;
    private String url;
    private String token;
    private boolean dataChannelOnly = false;
    private String subscriberId = "";
    private String subscriberCode = "";
    private String streamName = "";
    private String viewerInfo = "";
    private String currentSource;
    private boolean screenPermissionNeeded = true;

    public MediaProjection mediaProjection;
    public MediaProjectionManager mediaProjectionManager;
    private String mainTrackId;

    private StatsCollector statsCollector = new StatsCollector();

    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String VIDEO_TRACK_TYPE = "video";
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_H264 = "H264";
    private static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    private static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    private static final String VIDEO_CODEC_AV1 = "AV1";
    private static final String AUDIO_CODEC_OPUS = "opus";
    private static final String AUDIO_CODEC_ISAC = "ISAC";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    private static final String VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    private static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    private static final String DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    private static final int HD_VIDEO_WIDTH = 1280;
    private static final int HD_VIDEO_HEIGHT = 720;
    private static final int BPS_IN_KBPS = 1000;
    private static final String RTCEVENTLOG_OUTPUT_DIR_NAME = "rtc_event_log";

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Timer statsTimer;
    @androidx.annotation.Nullable
    private PeerConnectionFactory factory;

    public void setStreamId(String streamId) {
        setSelfStreamId(streamId);
    }

    class PeerInfo {

        public PeerInfo(String id, String mode) {
            this.id = id;
            this.mode = mode;
        }

        public String id;
        public PeerConnection peerConnection;
        public DataChannel dataChannel;
        public String mode;
        public String token;
        public boolean videoCallEnabled;
        public boolean audioCallEnabled;
        public String subscriberId;
        public String subscriberCode;
        public String streamName;
        public String mainTrackId;
        public String metaData;

    }
    public Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
    @androidx.annotation.Nullable
    private AudioSource audioSource;
    @androidx.annotation.Nullable
    private SurfaceTextureHelper surfaceTextureHelper;
    @androidx.annotation.Nullable
    private VideoSource videoSource;
    private boolean preferIsac;
    private boolean videoCapturerStopped;
    private MediaConstraints audioConstraints;
    private MediaConstraints sdpMediaConstraints;
    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    @androidx.annotation.Nullable
    private List<IceCandidate> queuedRemoteCandidates;
    private boolean isInitiator;

    @androidx.annotation.Nullable
    private SessionDescription localDescription; // either offer or answer description
    // enableVideo is set to true if video should be rendered and sent.
    private boolean renderVideo = true;
    @androidx.annotation.Nullable
    private VideoTrack remoteVideoTrack;
    @androidx.annotation.Nullable
    private RtpSender localVideoSender;
    // enableAudio is set to true if audio should be sent.
    private boolean enableAudio = true;
    @androidx.annotation.Nullable
    private AudioTrack localAudioTrack;

    private boolean dataChannelEnabled;
    // Enable RtcEventLog.
    @androidx.annotation.Nullable
    private RtcEventLog rtcEventLog;

    @androidx.annotation.Nullable
    public JavaAudioDeviceModule adm;

    private String selfStreamId;

    private static final Map<Long, Long> captureTimeMsMap = new ConcurrentHashMap<>();
    //PeerConnection Parameters
    private boolean videoCallEnabled;
    public boolean loopback;
    public boolean tracing;
    private int videoWidth;
    private int videoHeight;
    private int videoFps;
    private int videoStartBitrate;
    private String videoCodec;
    private boolean hwCodecAcceleration;
    private boolean videoFlexfecEnabled;
    private int audioStartBitrate;
    private String audioCodec;
    private boolean noAudioProcessing;
    private boolean aecDump;
    private boolean useOpenSLES;
    private boolean disableBuiltInAEC;
    private boolean disableBuiltInAGC;
    private boolean disableBuiltInNS;
    private boolean disableWebRtcAGCAndHPF;
    private boolean enableRtcEventLog;
    private boolean audioCallEnabled = true;
    private boolean captureToTexture;

    //DataChannel Parameters
    private boolean dataChannelOrdered;
    private int dataChannelMaxRetransmitTimeMs;
    private int dataChannelMaxRetransmits;
    private String dataChannelProtocol = "";
    private boolean dataChannelNegotiated;
    private int dataChannelId;
    private boolean dataChannelCreator;

    private boolean removeVideoRotationExtention = false;

    //signaling parameters
    private ArrayList<IceCandidate> iceCandidates = new ArrayList<>();

    //reconnection parameters
    private Handler reconnectionHandler = new Handler();

    private boolean reconnectionEnabled = false;

    final int RECONNECTION_PERIOD_MLS = 1000;

    public static final int RECONNECTION_CONTROL_PERIOD_MLS = 10000;

    Runnable reconnectionRunnable;

    public void createReconnectionRunnable() {
        reconnectionRunnable = new Runnable() {
            @Override
            public void run() {
                if (!streamStoppedByUser) {
                    boolean noNeedToRetry = true;
                    for (PeerInfo peerInfo : peers.values()) {
                        PeerConnection pc = peerInfo.peerConnection;

                        if (pc == null ||
                                (pc != null
                                        && pc.iceConnectionState() != PeerConnection.IceConnectionState.CHECKING
                                        && pc.iceConnectionState() != PeerConnection.IceConnectionState.CONNECTED
                                        && pc.iceConnectionState() != PeerConnection.IceConnectionState.COMPLETED)) {

                            noNeedToRetry = false;

                            if (pc != null) {
                                pc.dispose();
                            }

                            if (peerInfo.mode.equals(MODE_PUBLISH)) {
                                publish(peerInfo.id,
                                        peerInfo.token,
                                        peerInfo.videoCallEnabled,
                                        peerInfo.audioCallEnabled,
                                        peerInfo.subscriberId,
                                        peerInfo.subscriberCode,
                                        peerInfo.streamName,
                                        peerInfo.mainTrackId);
                            } else if (peerInfo.mode.equals(MODE_PLAY)) {
                                play(peerInfo.id,
                                        peerInfo.token,
                                        null,
                                        peerInfo.subscriberId,
                                        peerInfo.subscriberCode,
                                        peerInfo.metaData);
                            }
                        }
                    }
                    Log.i(TAG, "Try to reconnect in reconnectionRunnable " + streamMode);
                    webRTCListener.onReconnectionAttempt(initialStreamId);
                    if (streamMode == IWebRTCClient.MODE_JOIN) {
                        pipRenderer.setZOrderOnTop(true);
                    }

                    reconnectionInProgress = !noNeedToRetry;

                    if (reconnectionInProgress) {
                        reconnectionHandler.postDelayed(this, RECONNECTION_CONTROL_PERIOD_MLS);
                    }
                }
            }
        };
    }

    public SDPObserver getSdpObserver(String streamId) {
        return new SDPObserver(streamId);
    }

    public void joinToConferenceRoom(String roomId, String streamId) {
        handler.post(() -> {
            if (wsHandler != null) {
                wsHandler.joinToConferenceRoom(roomId, streamId);
            }
        });
    }

    public void leaveFromConference(String roomId) {
        handler.post(() -> {
            if (wsHandler != null) {
                wsHandler.leaveFromTheConferenceRoom(roomId);
            }
        });
    }

    public void getRoomInfo(String roomId, String streamId) {
        handler.post(() -> {
            if (wsHandler != null) {
                wsHandler.getRoomInfo(roomId, streamId);
            }
        });
    }

    class TrackCheckTask extends TimerTask {
        private MediaStream mediaStream;
        private ConcurrentLinkedQueue<VideoTrack> videoTracks = new ConcurrentLinkedQueue<>();

        @Override
        public void run() {
            if(mediaStream == null) {
                return;
            }

            List<VideoTrack> currentTracks = mediaStream.videoTracks;

            if (currentTracks.size() > 0) {
                for (VideoTrack track : videoTracks) {
                    boolean trackLive = false;
                    for (VideoTrack currentTrack : currentTracks) {
                        if (currentTrack.id().equals(track.id())) {
                            trackLive = true;
                            break;
                        }
                    }
                    if(!trackLive) {
                        videoTracks.remove(track);
                        handler.post(() -> {
                            webRTCListener.onVideoTrackEnded(track);
                        });
                    }
                }
            }
        }

        public void setStream(MediaStream stream) {
            this.mediaStream = stream;
        }

        public ConcurrentLinkedQueue<VideoTrack> getVideoTracks() {
            return videoTracks;
        }

    };

    /**
     * This task is used to check if video track is still alive
     * Different than others SDKs, WebRTC doesn't have any callback in Android for track ended
     */
    TrackCheckTask trackCheckerTask;

    //Timer to check if video track check task
    private Timer trackCheckerTimer;


    // Implementation detail: observe ICE & stream changes and react accordingly.
    class PCObserver implements PeerConnection.Observer {

        private String streamId;

        PCObserver(String streamId) {
            this.streamId = streamId;
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            executor.execute(() -> handler.post(() -> {
                if (wsHandler != null) {
                    wsHandler.sendLocalIceCandidate(streamId, candidate);
                }
            }));
        }

        @Override
        public void onIceCandidateError(final IceCandidateErrorEvent event) {
            Log.d(TAG,
                    "IceCandidateError address: " + event.address + ", port: " + event.port + ", url: "
                            + event.url + ", errorCode: " + event.errorCode + ", errorText: " + event.errorText);
        }

        @Override
        public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
            executor.execute(() -> {
                //not implemented because there is no counterpart on AMS
            });
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState newState) {
            Log.d(TAG, "SignalingState: " + newState);
        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
            executor.execute(() -> {
                Log.d(TAG, "IceConnectionState: " + newState);
                if (newState == PeerConnection.IceConnectionState.CONNECTED) {
                    onIceConnected(streamId);
                } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED) {
                    onIceDisconnected(streamId);
                } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                    reportError(streamId, "ICE connection failed.");
                }
            });
        }

        @Override
        public void onConnectionChange(final PeerConnection.PeerConnectionState newState) {
            executor.execute(() -> {
                Log.d(TAG, "PeerConnectionState: " + newState);
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    onConnected();
                } else if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    onDisconnected();
                } else if (newState == PeerConnection.PeerConnectionState.FAILED) {
                    reportError(streamId, "DTLS connection failed.");
                }
            });
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
            Log.d(TAG, "IceGatheringState: " + newState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean receiving) {
            Log.d(TAG, "IceConnectionReceiving changed to " + receiving);
        }

        @Override
        public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
            Log.d(TAG, "Selected candidate pair changed because: " + event);
        }

        @Override
        public void onAddStream(final MediaStream stream) {
            if(trackCheckerTask != null) {
                trackCheckerTask.setStream(stream);
            }
            if (!isVideoCallEnabled() && !isAudioEnabled())
            {
                if(renderersProvidedAtStart || !remoteSinks.isEmpty()) {
                    updateVideoTracks(streamId);
                }
            }
        }

        private void updateVideoTracks(String streamId) {
            List<VideoTrack> remoteVideoTrackList = getRemoteVideoTrackList(streamId);
            for (int i = 0; i < remoteVideoTrackList.size(); i++)
            {
                VideoTrack videoTrack = remoteVideoTrackList.get(i);

                if (i < remoteSinks.size()) {
                    videoTrack.addSink(remoteSinks.get(i));


                } else {
                    Log.e(TAG, "There is no enough remote sinks to show video tracks");
                }
            }
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {}

        @Override
        public void onDataChannel(final DataChannel dc) {
            Log.d(TAG, "New Data channel " + dc.label());

            if (!dataChannelEnabled)
                return;

            if (peers.get(streamId).dataChannel == null) {
                peers.get(streamId).dataChannel = dc;
            }
            dc.registerObserver(new DataChannelInternalObserver(dc));
        }

        @Override
        public void onRenegotiationNeeded() {
            // No need to do anything; AppRTC follows a pre-agreed-upon
            // signaling/negotiation protocol.
        }

        @Override
        public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
            if(receiver.track() instanceof VideoTrack) {
                VideoTrack videoTrack = (VideoTrack) receiver.track();
                webRTCListener.onNewVideoTrack(videoTrack);
                if(streamMode.equals(MODE_MULTI_TRACK_PLAY) || streamMode.equals(MODE_TRACK_BASED_CONFERENCE)) {
                    trackCheckerTask.getVideoTracks().add(videoTrack);
                }

                if(renderersProvidedAtStart || !remoteSinks.isEmpty()) {
                    updateVideoTracks(streamId);
                }
            }
        }

        @Override
        public void onRemoveTrack(RtpReceiver receiver) {
            Log.d("antmedia","on remove track");
        }
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    class SDPObserver implements SdpObserver {
        private final String streamId;
        SDPObserver(String streamId) {
            this.streamId = streamId;
        }
        @Override
        public void onCreateSuccess(final SessionDescription desc) {
            //if (localDescription != null) {
            //  reportError("Multiple SDP create.");
            //  return;
            //}
            String sdp = desc.description;
            if (preferIsac) {
                sdp = preferCodec(sdp, AUDIO_CODEC_ISAC, true);
            }
            if (isVideoCallEnabled()) {
                sdp = preferCodec(sdp, getSdpVideoCodecName(videoCodec), false);
            }

            if(removeVideoRotationExtention) {
                sdp = sdp.replace(VIDEO_ROTATION_EXT_LINE, "");
            }

            final SessionDescription newDesc = new SessionDescription(desc.type, sdp);
            localDescription = newDesc;
            executor.execute(() -> {
                PeerConnection pc = peers.get(streamId).peerConnection;
                if (pc != null && !isError) {
                    Log.d(TAG, "Set local SDP from " + desc.type);
                    pc.setLocalDescription(this, newDesc);
                }
            });
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG, "onSetSuccess: ");

            executor.execute(() -> {
                PeerConnection pc = peers.get(streamId).peerConnection;
                if (pc == null || isError) {
                    return;
                }

                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (pc.getRemoteDescription() == null) {
                        // We've just set our local SDP so time to send it.
                        Log.d(TAG, "Local SDP set succesfully");
                        onLocalDescription(streamId, localDescription);
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        Log.d(TAG, "Remote SDP set succesfully");
                        drainCandidates(streamId);
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (pc.getLocalDescription() != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        Log.d(TAG, "Local SDP set succesfully");
                        onLocalDescription(streamId, localDescription);
                        drainCandidates(streamId);
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        Log.d(TAG, "Remote SDP set succesfully");
                    }
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {
            reportError(streamId, "createSDP error: " + error);
        }

        @Override
        public void onSetFailure(final String error) {
            reportError(streamId, "setSDP error: " + error);
        }
    }


    public WebRTCClient(IWebRTCListener webRTCListener, Context context) {
        this.webRTCListener = webRTCListener;
        this.context = context;
        mainHandler = new Handler(context.getMainLooper());
        iceServers.add(new PeerConnection.IceServer(stunServerUri));
    }

    public void setRemoteRendererList(List<SurfaceViewRenderer> rendererList) {
        this.remoteRendererList = rendererList;
    }

    public void setSelfStreamId(String streamId) {
        this.initialStreamId = streamId;
    }

    @Override
    public void init(String url, String streamId, String mode, String token, Intent intent) {

        if (url == null) {
            Log.d(TAG, this.context.getString(R.string.missing_url));
            return;
        }
        this.url = url;

        if (streamId == null || streamId.length() == 0) {
            Log.d(TAG, this.context.getString(R.string.missing_stream_id));
            return;
        }
        this.initialStreamId = streamId;

        if (mode == null || mode.length() == 0) {
            Log.d(TAG, this.context.getString(R.string.missing_stream_id));
            return;
        }
        this.streamMode = mode;
        this.token = token;
        if (intent != null) {
            this.intent = intent;
        }

        if(reconnectionEnabled && reconnectionRunnable == null) {
            createReconnectionRunnable();
        }

        /*
         *  TODO (burak): we need this track checker to be able to understand if the track is stopped.
         *  Normally we should have a callback from the peer connection but it does not have or
         *  we could not find it. So we are checking the track status periodically
         */
        initializeTrackChecker();

        initializeRenderers();

        initializeParameters();

        initializePeerConnectionFactory();

        initializeVideoCapturer();

        initializeAudioManager();

        connectWebSocket();
    }

    private void initializeTrackChecker() {
        if((streamMode.equals(MODE_MULTI_TRACK_PLAY) || streamMode.equals(MODE_TRACK_BASED_CONFERENCE))
                && trackCheckerTask == null) {
            trackCheckerTask = new TrackCheckTask();
            trackCheckerTimer = new Timer();
            trackCheckerTimer.schedule(trackCheckerTask, TRACK_CHECK_PERIDOD_MS, TRACK_CHECK_PERIDOD_MS);
        }
    }


    public void addTrackToRenderer(VideoTrack track, SurfaceViewRenderer renderer) {
        mainHandler.post(() -> {
            ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
            remoteVideoSink.setTarget(renderer);
            renderer.init(eglBase.getEglBaseContext(), null);
            renderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
            renderer.setEnableHardwareScaler(true);
            track.addSink(remoteVideoSink);

            dynamicRenderers.add(renderer);
            dynamicRemoteSinks.add(remoteVideoSink);
        });
    }


    public void initializeParameters() {
        loopback = intent.getBooleanExtra(CallActivity.EXTRA_LOOPBACK, false);
        tracing = intent.getBooleanExtra(CallActivity.EXTRA_TRACING, false);

        videoWidth = intent.getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
        videoHeight = intent.getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);

        screencaptureEnabled = intent.getBooleanExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            videoWidth = displayMetrics.widthPixels;
            videoHeight = displayMetrics.heightPixels;
        }

        dataChannelEnabled = intent.getBooleanExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, false);
        if (dataChannelEnabled) {
            dataChannelOrdered = intent.getBooleanExtra(CallActivity.EXTRA_ORDERED, true);
            dataChannelMaxRetransmitTimeMs = intent.getIntExtra(CallActivity.EXTRA_MAX_RETRANSMITS_MS, -1);
            dataChannelMaxRetransmits = intent.getIntExtra(CallActivity.EXTRA_MAX_RETRANSMITS, -1);
            dataChannelProtocol = intent.getStringExtra(CallActivity.EXTRA_PROTOCOL);
            dataChannelNegotiated = intent.getBooleanExtra(CallActivity.EXTRA_NEGOTIATED, false);
            dataChannelId = intent.getIntExtra(CallActivity.EXTRA_ID, -1);
            dataChannelCreator = streamMode.equals(IWebRTCClient.MODE_PUBLISH) || streamMode.equals(IWebRTCClient.MODE_JOIN);
        }

        videoFps = intent.getIntExtra(CallActivity.EXTRA_VIDEO_FPS, 0);

        videoCodec = intent.getStringExtra(CallActivity.EXTRA_VIDEOCODEC);
        if (videoCodec == null) {
            videoCodec = this.context.getString(R.string.pref_videocodec_default);
        }
        videoStartBitrate = this.intent.getIntExtra(CallActivity.EXTRA_VIDEO_BITRATE, 0);

        if (videoStartBitrate == 0) {
            videoStartBitrate = Integer.parseInt(this.context.getString(R.string.pref_maxvideobitratevalue_default));
        }

        audioStartBitrate = this.intent.getIntExtra(CallActivity.EXTRA_AUDIO_BITRATE, 0);
        if (audioStartBitrate == 0) {
            audioStartBitrate = Integer.parseInt(this.context.getString(R.string.pref_startaudiobitratevalue_default));
        }

        videoCallEnabled = intent.getBooleanExtra(CallActivity.EXTRA_VIDEO_CALL, true);

        if (isDataChannelOnly() || streamMode.equals(MODE_PLAY) || streamMode.equals(MODE_MULTI_TRACK_PLAY)) {
            videoCallEnabled = false;
            audioCallEnabled = false;
        }

        if(streamMode.equals(MODE_TRACK_BASED_CONFERENCE)){
            videoCallEnabled = true;
            audioCallEnabled = true;
        }



        hwCodecAcceleration = intent.getBooleanExtra(CallActivity.EXTRA_HWCODEC_ENABLED, true);
        videoFlexfecEnabled = intent.getBooleanExtra(CallActivity.EXTRA_FLEXFEC_ENABLED, false);
        audioCodec = intent.getStringExtra(CallActivity.EXTRA_AUDIOCODEC);
        noAudioProcessing = intent.getBooleanExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, false);
        aecDump = intent.getBooleanExtra(CallActivity.EXTRA_AECDUMP_ENABLED, false);
        useOpenSLES = intent.getBooleanExtra(CallActivity.EXTRA_OPENSLES_ENABLED, false);
        disableBuiltInAEC = intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AEC, false);
        disableBuiltInAGC = intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_AGC, false);
        disableBuiltInNS = intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_BUILT_IN_NS, false);
        disableWebRtcAGCAndHPF = intent.getBooleanExtra(CallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false);
        enableRtcEventLog = intent.getBooleanExtra(CallActivity.EXTRA_ENABLE_RTCEVENTLOG, false);
        captureToTexture = intent.getBooleanExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    public void initializeRenderers() {
        if(eglBase == null) {
            eglBase = EglBase.create();
        }
        if(renderersInitiated){
            return;
        }

        if (remoteRendererList != null) {
            renderersProvidedAtStart = true;
            for (SurfaceViewRenderer renderer : remoteRendererList) {
                //if we are performing reconnection, we shouldn't add remote sinks again
                if (!renderersInitiated) {
                    ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
                    remoteSinks.add(remoteVideoSink);
                }

                renderer.init(eglBase.getEglBaseContext(), null);
                renderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
                renderer.setEnableHardwareScaler(true);

            }
        }
        else {
            remoteSinks.add(remoteProxyRenderer);
        }
      //  else if(remoteProxyRenderer != null){
       //     remoteSinks.add(remoteProxyRenderer);
       // }
        // Create video renderers.
        if (pipRenderer != null) {
            pipRenderer.init(eglBase.getEglBaseContext(), null);
            pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);

        }

        // When saveRemoteVideoToFile is set we save the video from the remote to a file.
        if (saveRemoteVideoToFile != null) {
            try {
                videoFileRenderer = new VideoFileRenderer(
                        saveRemoteVideoToFile, videoWidth, videoHeight, eglBase.getEglBaseContext());
                remoteSinks.add(videoFileRenderer);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Failed to open video file for output: " + saveRemoteVideoToFile, e);
            }
        }
        if(fullscreenRenderer != null) {
            fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
            fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);
            fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
            Log.i(getClass().getSimpleName(), "Initializing the full screen renderer");
        }

        if (pipRenderer != null) {
            pipRenderer.setZOrderMediaOverlay(true);
            pipRenderer.setEnableHardwareScaler(true /* enabled */);
        }

        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);
        renderersInitiated = true;
    }

    public void initializePeerConnectionFactory() {
        if(factory != null) {
            return;
        }

        // Create peer connection client.
        Log.d(TAG, "Preferred video codec: " + getSdpVideoCodecName(videoCodec));
        final String fieldTrials = getFieldTrials(videoFlexfecEnabled, disableWebRtcAGCAndHPF);
        executor.execute(() -> {
            Log.d(TAG, "Initialize WebRTC. Field trials: " + fieldTrials);
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                            .setFieldTrials(fieldTrials)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions());
        });


        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (loopback) {
            options.networkIgnoreMask = 0;
        }
        //options.disableEncryption = true;
        createPeerConnectionFactory(options);
    }

    public void initializeAudioManager() {
        if (audioCallEnabled && audioManager == null) {
            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(this.context.getApplicationContext());
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Starting the audio manager...");
            audioManager.start((audioDevice, availableAudioDevices) ->
            {
                // This method will be called each time the number of available audio devices has changed.
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            });


        }
    }


    public void initializeVideoCapturer() {
        if(videoCapturer != null){
            return;
        }

        // if video capture is null or disposed, we should recreate it.
        // we should also check if video capturer is an instance of ScreenCapturerAndroid
        // because other implementations of VideoCapturer doesn't have a dispose() method.
        if (videoCallEnabled
                && (videoCapturer == null
                || (videoCapturer instanceof ScreenCapturerAndroid))
        ) {

            String source = SOURCE_REAR;
            String videoFileAsCamera = this.intent.getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);

            if (videoFileAsCamera != null) {
                source = SOURCE_FILE;
            }
            else if(screencaptureEnabled) {
                source = SOURCE_SCREEN;
            }
            else if(customCapturerEnabled) {
                source = SOURCE_CUSTOM;
            }
            else if(useCamera2()) {
                source = SOURCE_FRONT;
            }

            videoCapturer = createVideoCapturer(source);
            currentSource = source;
        }

        executor.execute(() -> {
            createMediaConstraintsInternal();
            createVideoTrack(videoCapturer);
            createAudioTrack();
        });
    }

    public void setMediaProjection(MediaProjection mediaProjection){
        this.mediaProjection = mediaProjection;
        adm.setMediaProjection(mediaProjection);
    }

    public void setBitrate(int bitrate) {
        setVideoMaxBitrate(bitrate);
    }

    public void startStream(String streamId) {
        Log.i(getClass().getSimpleName(), "Starting stream");
        startCall(streamId);
    }

    public void connectWebSocket() {
        if (wsHandler == null) {
            Log.i(TAG, "WebsocketHandler is null and creating a new instance");
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(url);
        } else if (!wsHandler.isConnected()) {
            Log.i(TAG, "WebSocketHandler already exists but not connected. Disconnecting and creating new one");
            wsHandler.disconnect(true);
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(url);
        }
    }

    /**
     * Starts the actual WebRTC session with the initial stream Id.
     * @deprecated
     * use {@link #startStream(String)} instead
     */
    @Deprecated
    public void startStream() {
        startStream(initialStreamId);
    }


    @TargetApi(17)
    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
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
    public void startScreenCapture() {
        mediaProjectionManager =
                (MediaProjectionManager) this.context.getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);

        if (this.context instanceof Activity) {
            ((Activity) this.context).startActivityForResult(
                    mediaProjectionManager.createScreenCaptureIntent(), CallActivity.CAPTURE_PERMISSION_REQUEST_CODE);
        }
    }

    @TargetApi(21)
    public @Nullable VideoCapturer createScreenCapturer() {
        if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            reportError(initialStreamId, "User didn't give permission to capture the screen.");
            return null;
        }
        return new ScreenCapturerAndroid(mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                //this is self-explanatory error code
                reportError(initialStreamId, ERROR_USER_REVOKED_CAPTURE_SCREEN_PERMISSION);
            }
        });
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != CallActivity.CAPTURE_PERMISSION_REQUEST_CODE)
            return;
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;

        screenPermissionNeeded = false;
        changeVideoSource(SOURCE_SCREEN);
    }

    public boolean useCamera2() {
        return Camera2Enumerator.isSupported(this.context) && this.intent.getBooleanExtra(CallActivity.EXTRA_CAMERA2, true);
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

    @Override
    public void stopVideoSource() {
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (!screencaptureEnabled) {
            executor.execute(this::stopVideoSourceInternal);
        }

        localProxyVideoSink.setTarget(null);

    }

    @Override
    public void startVideoSource() {
        // Video is not paused for screencapture. See onPause.
        if (!screencaptureEnabled) {
            executor.execute(this::startVideoSourceInternal);
        }
    }

    /*
     * Starts the actual WebRTC session with the given stream Id.
     * @deprecated
     * use {@link #stopStream(String)} instead
     */
    @Deprecated
    @Override
    public void stopStream() {
        stopStream(initialStreamId, true);
    }

    public void stopStream(String streamId) {
        stopStream(streamId, true);
    }


    public void stopStream(String streamId, boolean byUser) {
        Log.i(getClass().getSimpleName(), "Stopping stream");
        streamStoppedByUser = byUser;
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.stop(streamId);
        }
    }

    @Override
    public void switchCamera() {
        openFrontCamera = !openFrontCamera;
        executor.execute(this ::switchCameraInternal);
    }

    @Override
    public void onCameraSwitch() {
        switchCamera();
    }

    @Override
    public void onVideoScalingSwitch(ScalingType scalingType) {
        if (fullscreenRenderer != null) {
            fullscreenRenderer.setScalingType(scalingType);
        }
    }


    public void switchVideoScaling(RendererCommon.ScalingType scalingType) {
        onVideoScalingSwitch(scalingType);
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        executor.execute(() -> changeCaptureFormatInternal(width, height, framerate));
    }

    @Override
    public boolean onToggleMic() {
        micEnabled = !micEnabled;
        setAudioEnabled(micEnabled);
        return micEnabled;
    }

    @Override
    public boolean toggleMic() {
        return onToggleMic();
    }

    private void startCall(String streamId) {
        Log.d(TAG, this.context.getString(R.string.connecting_to, url));
        if (streamMode.equals(IWebRTCClient.MODE_PUBLISH)) {
            publish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, mainTrackId);
        }
        else if (streamMode.equals(IWebRTCClient.MODE_PLAY)) {
            play(streamId, token, null, subscriberId, subscriberCode, viewerInfo);
        }
        else if (streamMode.equals(IWebRTCClient.MODE_JOIN)) {
            init(this.url, streamId, this.streamMode, token, this.intent);
            wsHandler.joinToPeer(streamId, token);
        }
        else if (streamMode.equals(IWebRTCClient.MODE_MULTI_TRACK_PLAY)) {
            init(this.url, streamId, this.streamMode, token, this.intent);
            wsHandler.getTrackList(streamId, token);
        }
    }

    public void publish(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled, String subscriberId, String subscriberCode, String streamName, String mainTrackId) {
        System.out.println("Publishing to: "+streamId+" t:"+this);
        Log.e(TAG, "Publish: "+streamId);

        PeerInfo peerInfo = new PeerInfo(streamId, MODE_PUBLISH);
        peerInfo.token = token;
        peerInfo.videoCallEnabled = videoCallEnabled;
        peerInfo.audioCallEnabled = audioCallEnabled;
        peerInfo.subscriberId = subscriberId;
        peerInfo.subscriberCode = subscriberCode;
        peerInfo.streamName = streamName;
        peerInfo.mainTrackId = mainTrackId;
        peers.put(streamId, peerInfo);

        init(this.url, streamId, this.streamMode, this.token, this.intent);
        wsHandler.startPublish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, mainTrackId);
    }

    public void play(String streamId, String token, String[] tracks) {
        play(streamId, token, tracks, "", "", "");
    }

    public void play(String streamId, String token, String[] tracks,  String subscriberId, String subscriberCode, String viewerInfo) {
        Log.e(TAG, "Play: "+streamId);

        PeerInfo peerInfo = new PeerInfo(streamId, MODE_PLAY);
        peerInfo.token = token;
        peerInfo.subscriberId = subscriberId;
        peerInfo.subscriberCode = subscriberCode;
        peerInfo.metaData = viewerInfo;
        peers.put(streamId, peerInfo);

        init(this.url, streamId, this.streamMode, token, this.intent);
        wsHandler.startPlay(streamId, token, tracks, subscriberId, subscriberCode, viewerInfo);
    }

    public void enableTrack(String streamId, String trackId, boolean enabled) {
        wsHandler.enableTrack(streamId,trackId, enabled);
    }

    // Should be called from UI thread
    private void callConnected(String streamId) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Enable statistics callback.
        enableStatsEvents(streamId, true, CallActivity.STAT_CALLBACK_PERIOD);
        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
        if(audioManager != null) {
            audioManager.selectAudioDevice(device);
        }
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    public void release(boolean closeWebsocket) {
        Log.i(getClass().getSimpleName(), "Releasing resources");

        remoteProxyRenderer.setTarget(null);
        localVideoTrack = null;
        localAudioTrack = null;
        if (closeWebsocket && wsHandler != null && wsHandler.getSignallingListener().equals(this)) {
            wsHandler.disconnect(true);
            wsHandler = null;
        }
        if (pipRenderer != null) {
            pipRenderer.release();
            // pipRenderer = null; Do not make renderer null, we can re-use
        }

        if (remoteRendererList != null) {
            for (SurfaceViewRenderer renderer : remoteRendererList) {
                renderer.release();
            }
        }

        if (fullscreenRenderer != null) {
            Log.i(getClass().getSimpleName(), "Releasing full screen renderer");
            fullscreenRenderer.release();
            // fullscreenRenderer = null; Do not make renderer null, we can re-use
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            // videoFileRenderer = null; Do not make renderer null, we can re-use
        }

        for (SurfaceViewRenderer renderer : dynamicRenderers) {
            renderer.release();
        }
        dynamicRenderers.clear();
        dynamicRemoteSinks.clear();

        executor.execute(this ::closeInternal);

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
    }


    public void disconnectWithErrorMessage(final String errorMessage) {
        Log.e(TAG, "Critical error: " + errorMessage);
        release(true);
    }

    public void reportError(String streamId, final String description) {
        this.handler.post(() -> {

            if (!isError) {
                isError = true;
                errorString = description;

                disconnectWithErrorMessage(description);
                if (webRTCListener != null) {
                    webRTCListener.onError(description, streamId);
                }
            }

        });
    }

    public void changeVideoSource(String newSource) {
        if(currentSource == null || !currentSource.equals(newSource)) {
            if(newSource.equals(SOURCE_SCREEN) && screenPermissionNeeded) {
                startScreenCapture();
                return;
            } else if(newSource.equals(SOURCE_REAR)) {
                openFrontCamera = false;
            } else if(newSource.equals(SOURCE_FRONT)) {
                openFrontCamera = true;
            }
            VideoCapturer newVideoCapturer = createVideoCapturer(newSource);

            int videoWidth = intent.getIntExtra(CallActivity.EXTRA_VIDEO_WIDTH, 0);
            int videoHeight = intent.getIntExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 0);

            // If capturing format is not specified for screencapture, use screen resolution.
            if (videoWidth == 0 || videoHeight == 0) {
                DisplayMetrics displayMetrics = getDisplayMetrics();
                videoWidth = displayMetrics.widthPixels;
                videoHeight = displayMetrics.heightPixels;
            }

            /* When user try to change video source after stopped the publishing
             * peerConnectionClient will null, until start another broadcast
             */
            changeVideoCapturer(newVideoCapturer);
            currentSource = newSource;
        }
    }

    public @Nullable VideoCapturer createVideoCapturer(String source) {
        final VideoCapturer videoCapturer;

        if (SOURCE_FRONT.equals(source)) {
            openFrontCamera = true;
        } else if (SOURCE_REAR.equals(source)) {
            openFrontCamera = false;
        }
        if (source.equals(SOURCE_FILE)) {
            String videoFileAsCamera = this.intent.getStringExtra(CallActivity.EXTRA_VIDEO_FILE_AS_CAMERA);
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                reportError(initialStreamId, "Failed to open video file for emulated camera");
                return null;
            }
        } else if (SOURCE_SCREEN.equals(source)) {
            return createScreenCapturer();
        } else if (SOURCE_CUSTOM.equals(source)) {
            return new CustomVideoCapturer();
        } else {
            if (!captureToTexture) {
                reportError(initialStreamId, this.context.getString(R.string.camera2_texture_only_error));
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this.context));
        }
        if (videoCapturer == null) {
            reportError(initialStreamId, "Failed to open camera");
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
        else if (this.streamMode.equals(MODE_MULTI_TRACK_PLAY) || streamMode.equals(MODE_TRACK_BASED_CONFERENCE))
        {
            if(renderersProvidedAtStart) {
                for (int i = 0; i < remoteSinks.size(); i++) {
                    ((ProxyVideoSink) remoteSinks.get(i)).setTarget(remoteRendererList.get(i));
                }
            }

            if(localProxyVideoSink != null) {
                localProxyVideoSink.setTarget(fullscreenRenderer);
            }
        }
        else if(fullscreenRenderer != null && pipRenderer != null) {
            // True if local view is in the fullscreen renderer.
            localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
            remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
            fullscreenRenderer.setMirror(isSwappedFeeds);
            pipRenderer.setMirror(!isSwappedFeeds);
        }
    }

    public void setWsHandler(WebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    private void setUpReconnection() {
        if(!streamStoppedByUser) {
            Log.i(getClass().getSimpleName(),"Disconnected. Trying to reconnect");
            reconnectionInProgress = true;
            //Toast.makeText(context, "Disconnected.Trying to reconnect "+streamStoppedByUser, Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!reconnectionHandler.hasCallbacks(reconnectionRunnable)) {
                    reconnectionHandler.postDelayed(reconnectionRunnable, RECONNECTION_PERIOD_MLS);
                }
            } else {
                reconnectionHandler.postDelayed(reconnectionRunnable, RECONNECTION_PERIOD_MLS);
            }
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

    public void disableVideo() {
        setVideoEnabled(false);
        videoOn = false;
    }

    public void enableVideo() {
        setVideoEnabled(true);
        videoOn = true;
    }

    public void disableAudio() {
        setAudioEnabled(false);
        audioOn = false;
    }

    public void enableAudio() {
        setAudioEnabled(true);
        audioOn = true;
    }

    public boolean isVideoOn() {
        return videoOn;
    }

    public boolean isAudioOn() {
        return audioOn;
    }

    public int getMediaProjectionPermissionResultCode() {
        return mediaProjectionPermissionResultCode;
    }

    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    public void onLocalDescription(String streamId, final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        this.handler.post(() -> {
            if (wsHandler != null) {
                Log.d(TAG,"Sending " + sdp.type + ", delay=" + delta + "ms");
                if (isInitiator) {
                    wsHandler.sendConfiguration(streamId, sdp, "offer");
                } else {
                    wsHandler.sendConfiguration(streamId, sdp, "answer");
                }
            }
            //check peerConnectionClient null because in very slow devices(emulator), it may cause crash
            if (videoStartBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + videoStartBitrate);
                setVideoMaxBitrate(videoStartBitrate);
            }
        });
    }

    public void onIceConnected(String streamId) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.handler.post(() -> {
            Log.d(TAG, "ICE connected, delay=" + delta + "ms");
            callConnected(streamId);

            if (webRTCListener != null) {
                webRTCListener.onIceConnected(streamId);
            }
        });
    }

    public void onIceDisconnected(String streamId) {
        this.handler.post(() -> {
            Log.d(TAG, "ICE disconnected");
            //release(false);
            if (webRTCListener != null) {
                webRTCListener.onIceDisconnected(streamId);
            }
        });
    }

    public void onConnected() {
        Log.w(getClass().getSimpleName(), "onConnected");
    }

    public void onPeerConnectionClosed() {}

    public void onPeerConnectionStatsReady(RTCStatsReport report) {
        this.handler.post(() -> {
            if (!isError) {
                //hudFragment.updateEncoderStatistics(reports);
                //Log.i(TAG, "onPeerConnectionStatsReady");
                //Log.i(TAG, report.toString());
                statsCollector.onStatsReport(report, streamMode);
            }
        });
    }


    public boolean isStreaming() {
        return isStreaming(initialStreamId);
    }

    public boolean isStreaming(String streamId) {
        PeerConnection pc = null;
        PeerInfo peerInfo = peers.get(initialStreamId);
        if(peerInfo != null) {
            pc = peerInfo.peerConnection;
        }
        return pc != null && pc.iceConnectionState().equals(PeerConnection.IceConnectionState.CONNECTED);
    }

    @Override
    public void setMediaProjectionParams(int resultCode, Intent data) {
        mediaProjectionPermissionResultCode = resultCode;
        mediaProjectionPermissionResultData = data;
    }

    @Override
    public void onTakeConfiguration(String streamId, SessionDescription sdp) {
        this.handler.post(() -> {
            if (sdp.type == SessionDescription.Type.OFFER) {
                if(peers.get(streamId).peerConnection == null) {
                    createPeerConnection(streamId);
                }



                setRemoteDescription(streamId, sdp);
                createAnswer(streamId);

                if (iceCandidates != null) {
                    // Add remote ICE candidates from room.
                    for (IceCandidate iceCandidate : iceCandidates) {
                        addRemoteIceCandidate(streamId, iceCandidate);
                    }
                }
            }
            else {
                setRemoteDescription(streamId, sdp);
            }
        });
    }

    @Override
    public void onPublishFinished(String streamId) {
        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPublishFinished(streamId);
            }
            release(false);
        });
    }


    @Override
    public void onPlayFinished(String streamId) {
        waitingForPlay = false;
        this.handler.post(() -> {
            release(false);
            if (webRTCListener != null) {
                webRTCListener.onPlayFinished(streamId);
            }
        });
    }

    @Override
    public void onPublishStarted(String streamId) {
        streamStoppedByUser = false;
        reconnectionInProgress = false;
        streamStarted = true;

        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPublishStarted(streamId);
            }
        });

    }

    @Override
    public void onPlayStarted(String streamId) {
        streamStoppedByUser = false;
        reconnectionInProgress = false;
        streamStarted = true;
        waitingForPlay = false;

        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.onPlayStarted(streamId);
            }
        });
    }

    @Override
    public void onStartStreaming(String streamId) {
        this.handler.post(() -> {
            createPeerConnection(streamId);
            Log.d(TAG, "Creating OFFER...");
            createOffer(streamId);
        });
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        webRTCListener.onJoinedTheRoom(streamId, streams);
    }

    @Override
    public void onRoomInformation(String[] streams) {
        webRTCListener.onRoomInformation(streams);
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.noStreamExistsToPlay(streamId);
            }
        });
    }

    @Override
    public void onLeftTheRoom (String roomId) {
        webRTCListener.onLeftTheRoom(roomId);
    }

    @Override
    public void streamIdInUse(String streamId){
        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.streamIdInUse(streamId);
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(String streamId, IceCandidate candidate) {
        this.handler.post(() -> addRemoteIceCandidate(streamId, candidate));
    }

    @Override
    public void onDisconnected() {
        this.handler.post(() -> {
            if (webRTCListener != null) {
                webRTCListener.onDisconnected(initialStreamId);
            }

            if(reconnectionEnabled && !reconnectionInProgress) {
                setUpReconnection();
            }
        });
    }

    @Override
    public void onTrackList(String[] tracks) {
        this.handler.post(()-> {
            if (webRTCListener != null) {
                webRTCListener.onTrackList(tracks);
            }
        });

        sendPlayOtherTracks(tracks);
    }

    public void sendPlayOtherTracks(String[] tracks) {
        if(autoPlayTracks && !isStreaming() && !waitingForPlay) {
            waitingForPlay = true;
            init(this.url, this.initialStreamId, this.streamMode, this.token, this.intent);

            //don't send play for its own stream id
            for (int i = 0; i < tracks.length; i++) {
                if(tracks[i].equals(selfStreamId)) {
                    tracks[i] = "!"+ tracks[i];
                    break;
                }
            }
            play(mainTrackId, token, tracks);
        }
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {
        this.handler.post(()-> {
            if (webRTCListener != null) {
                webRTCListener.onBitrateMeasurement(streamId, targetBitrate, videoBitrate, audioBitrate);
            }
        });
    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {
        this.handler.post(()-> {
            if (webRTCListener != null) {
                webRTCListener.onStreamInfoList(streamId, streamInfoList);
            }
        });
    }

    @Override
    public void onError(String streamId, String definition) {
        this.handler.post(()-> {
            if (webRTCListener != null) {
                webRTCListener.onError(definition, streamId);
            }
        });

        if(definition.equals("no_stream_exist")) {
            waitingForPlay = false;
        }
    }

    @Override
    public boolean isDataChannelEnabled() {
        return dataChannelEnabled;
    }

    /**
     * @deprecated
     * use {@link #getStreamInfoList(String)} instead
     */
    @Deprecated
    @Override
    public void getStreamInfoList() {
        getStreamInfoList(initialStreamId);
    }

    public void getStreamInfoList(String streamId) {
        wsHandler.getStreamInfoList(streamId);
    }

    /**
     * @deprecated
     * use {@link #forceStreamQuality(String, int)} instead
     */
    @Deprecated
    @Override
    public void forceStreamQuality(int height) {
        forceStreamQuality(initialStreamId, height);
    }

    public void forceStreamQuality(String streamId, int height) {
        wsHandler.forceStreamQuality(streamId, height);
    }

    @Override
    public void setSubscriberParams(String subscriberId, String subscriberCode) {
        this.subscriberId = subscriberId;
        this.subscriberCode = subscriberCode;
    }

    @Override
    public void setViewerInfo(String viewerInfo) {
        this.viewerInfo = viewerInfo;
    }

    @Override
    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public static void insertFrameId(long captureTimeMs) {
        captureTimeMsMap.put(captureTimeMs, System.currentTimeMillis());
    }

    public static Map<Long, Long> getCaptureTimeMsMapList() {
        return captureTimeMsMap;
    }

    public boolean isDataChannelOnly() {
        return dataChannelOnly;
    }

    public void setDataChannelOnly(boolean dataChannelOnly) {
        this.dataChannelOnly = dataChannelOnly;
    }

    public String getStreamId() {
        return initialStreamId;
    }

    public void setMainTrackId(String mainTrackId) {
        this.mainTrackId = mainTrackId;
    }


    class DataChannelInternalObserver implements DataChannel.Observer {

        private final DataChannel dataChannel;

        DataChannelInternalObserver(DataChannel dataChannel) {
            this.dataChannel = dataChannel;
        }
        @Override
        public void onBufferedAmountChange(long previousAmount) {
            if(dataChannelObserver == null) return;
            Log.d(TAG, "Data channel buffered amount changed: " + dataChannel.label() + ": " + dataChannel.state());
            handler.post(() -> dataChannelObserver.onBufferedAmountChange(previousAmount, dataChannel.label()));
        }

        @Override
        public void onStateChange() {
            handler.post(() -> {
                if(dataChannelObserver == null || dataChannel == null) return;
                //Log.d(TAG, "Data channel state changed: " + dataChannel.label() + ": " + dataChannel.state());
                //TODO: dataChannelObserver.onStateChange(dataChannel.state(), dataChannel.label());
            });
        }

        @Override
        public void onMessage(final DataChannel.Buffer buffer) {
            ByteBuffer copyByteBuffer = ByteBuffer.allocate(buffer.data.capacity());
            copyByteBuffer.put(buffer.data);
            copyByteBuffer.rewind();

            boolean binary = buffer.binary;
            DataChannel.Buffer bufferCopy = new DataChannel.Buffer(copyByteBuffer, binary);
            handler.post(() -> {
                if(dataChannelObserver == null || dataChannel == null) return;
                Log.d(TAG, "Received Message: " + dataChannel.label() + ": " + dataChannel.state());
                dataChannelObserver.onMessage(bufferCopy, dataChannel.label());
            });
        }
    };

    /*
     * sends message via data channel
     * @deprecated
     * use {@link #sendMessageViaDataChannel(String, DataChannel.Buffer)} instead
     */
    @Deprecated
    @Override
    public void sendMessageViaDataChannel(DataChannel.Buffer buffer) {
        sendMessageViaDataChannel(initialStreamId, buffer);
    }
    public void sendMessageViaDataChannel(String streamId, DataChannel.Buffer buffer) {
        if (isDataChannelEnabled()) {
            executor.execute(() -> {
                try {

                    boolean success = peers.get(streamId).dataChannel.send(buffer);
                    buffer.data.rewind();
                    if (dataChannelObserver != null) {
                        if (success) {
                            handler.post(() -> dataChannelObserver.onMessageSent(buffer, true));
                        } else {
                            handler.post(() -> dataChannelObserver.onMessageSent(buffer, false));
                            reportError(streamId, "Failed to send the message via Data Channel ");
                        }
                    }
                } catch (Exception e) {
                    reportError(streamId, "An error occurred when sending the message via Data Channel " + e.getMessage());
                    if (dataChannelObserver != null) {
                        buffer.data.rewind();
                        handler.post(() -> dataChannelObserver.onMessageSent(buffer, false));
                    }
                }
            });
        } else {
            reportError(streamId, "Data Channel is not ready for usage.");
        }
    }

    public void setLocalVideoTrack(@javax.annotation.Nullable VideoTrack localVideoTrack) {
        this.localVideoTrack = localVideoTrack;
    }

    public void changeVideoCapturer(VideoCapturer newVideoCapturer) {
        try {
            this.videoCapturer.stopCapture();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        videoCapturerStopped = true;
        this.videoCapturer = newVideoCapturer;
        this.localVideoTrack = null;

        MediaStreamTrack newTrack = (MediaStreamTrack) createVideoTrack(this.videoCapturer);
        if(this.localVideoSender != null) {
            this.localVideoSender.setTrack(newTrack, true);
        }
    }

    /**
     * This function should only be called once.
     */
    public void createPeerConnectionFactory(PeerConnectionFactory.Options options) {
        if (factory != null) {
            throw new IllegalStateException("PeerConnectionFactory has already been constructed");
        }
        executor.execute(() -> createPeerConnectionFactoryInternal(options));
    }

    public void createPeerConnection(String streamId) {
        executor.execute(() -> {
            try {
                createMediaConstraintsInternal();
                createPeerConnectionInternal(streamId);
                maybeCreateAndStartRtcEventLog(streamId);
            } catch (Exception e) {
                reportError(streamId, "Failed to create peer connection: " + e.getMessage());
                throw e;
            }
        });
    }

    private boolean isVideoCallEnabled() {
        return videoCallEnabled && videoCapturer != null;
    }

    private boolean isAudioEnabled() {
        return audioCallEnabled;
    }

    private void createPeerConnectionFactoryInternal(PeerConnectionFactory.Options options) {
        isError = false;

        if (tracing) {
            PeerConnectionFactory.startInternalTracingCapture(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                            + "webrtc-trace.txt");
        }

        // Check if ISAC is used by default.
        preferIsac = audioCodec != null && audioCodec.equals(AUDIO_CODEC_ISAC);

        adm = (JavaAudioDeviceModule) createJavaAudioDevice();

        // Create peer connection factory.
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
        }
        final boolean enableH264HighProfile =
                VIDEO_CODEC_H264_HIGH.equals(videoCodec);
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        if (hwCodecAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
            decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }

        // Disable encryption for loopback calls.
        if (loopback) {
            options.disableEncryption = true;
        }
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        Log.d(TAG, "Peer connection factory created.");
        adm.release();
    }

    AudioDeviceModule createJavaAudioDevice() {
        // Enable/disable OpenSL ES playback.
        if (!useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.");
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                reportError(initialStreamId, errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                reportError(initialStreamId, errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                reportError(initialStreamId, errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
                reportError(initialStreamId, errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
                reportError(initialStreamId,errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
                reportError(initialStreamId, errorMessage);
            }
        };

        // Set audio record state callbacks.
        JavaAudioDeviceModule.AudioRecordStateCallback audioRecordStateCallback = new JavaAudioDeviceModule.AudioRecordStateCallback() {
            @Override
            public void onWebRtcAudioRecordStart() {
                Log.i(TAG, "Audio recording starts");
            }

            @Override
            public void onWebRtcAudioRecordStop() {
                Log.i(TAG, "Audio recording stops");
            }
        };

        // Set audio track state callbacks.
        JavaAudioDeviceModule.AudioTrackStateCallback audioTrackStateCallback = new JavaAudioDeviceModule.AudioTrackStateCallback() {
            @Override
            public void onWebRtcAudioTrackStart() {
                Log.i(TAG, "Audio playout starts");
            }

            @Override
            public void onWebRtcAudioTrackStop() {
                Log.i(TAG, "Audio playout stops");
            }
        };

        return JavaAudioDeviceModule.builder(context)
                .setCustomAudioFeed(customAudioFeed)
                .setUseHardwareAcousticEchoCanceler(!disableBuiltInAEC)
                .setUseHardwareNoiseSuppressor(!disableBuiltInNS)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .setAudioRecordStateCallback(audioRecordStateCallback)
                .setAudioTrackStateCallback(audioTrackStateCallback)
                .createAudioDeviceModule();
    }

    private void createMediaConstraintsInternal() {
        // Create video constraints if video call is enabled.
        if (isVideoCallEnabled()) {
            // If video resolution is not specified, default to HD.
            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth = HD_VIDEO_WIDTH;
                videoHeight = HD_VIDEO_HEIGHT;
            }

            // If fps is not specified, default to 30.
            if (videoFps == 0) {
                videoFps = 30;
            }
            Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps);
        }

        // Create audio constraints.
        audioConstraints = new MediaConstraints();
        // added for audio performance measurements
        if (noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(
                    new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }
        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", Boolean.toString(isVideoCallEnabled())));
    }

    private void createPeerConnectionInternal(String streamId) {
        if (factory == null || isError) {
            Log.e(TAG, "Peerconnection factory is not created");
            return;
        }
        Log.d(TAG, "Create peer connection.");

        queuedRemoteCandidates = new ArrayList<>();

        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(iceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, getPCObserver(streamId));
        peers.get(streamId).peerConnection = peerConnection;

        isInitiator = false;

        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while `factory` is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR);

        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
        if (isVideoCallEnabled()) {
            peerConnection.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels);
            // We can add the renderers right away because we don't need to wait for an
            // answer to get the remote track.
            remoteVideoTrack = getRemoteVideoTrack(streamId);
            remoteVideoTrack.setEnabled(renderVideo);
            for (VideoSink remoteSink : remoteSinks) {
                remoteVideoTrack.addSink(remoteSink);
            }
        }
        if (isAudioEnabled()) {
            peerConnection.addTrack(createAudioTrack(), mediaStreamLabels);
        }

        if (isVideoCallEnabled()) {
            findVideoSender(streamId);
        }

        if (aecDump) {
            try {
                ParcelFileDescriptor aecDumpFileDescriptor =
                        ParcelFileDescriptor.open(new File(Environment.getExternalStorageDirectory().getPath()
                                        + File.separator + "Download/audio.aecdump"),
                                ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
                                        | ParcelFileDescriptor.MODE_TRUNCATE);
                factory.startAecDump(aecDumpFileDescriptor.detachFd(), -1);
            } catch (IOException e) {
                Log.e(TAG, "Can not open aecdump file", e);
            }
        }

        Log.d(TAG, "Peer connection created.");
    }

    @NonNull
    public PCObserver getPCObserver(String streamId) {
        return new PCObserver(streamId);
    }

    private void initDataChannel(String streamId) {
        if (dataChannelEnabled && dataChannelCreator) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = dataChannelOrdered;
            init.negotiated = dataChannelNegotiated;
            init.maxRetransmits = dataChannelMaxRetransmits;
            init.maxRetransmitTimeMs = dataChannelMaxRetransmitTimeMs;
            init.id = dataChannelId;
            init.protocol = dataChannelProtocol == null ? "" : dataChannelProtocol;
            DataChannel dataChannel = peers.get(streamId).peerConnection.createDataChannel(streamId, init);
            dataChannel.registerObserver(new DataChannelInternalObserver(dataChannel));
            peers.get(streamId).dataChannel = dataChannel;
        }
    }

    private File createRtcEventLogOutputFile() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault());
        Date date = new Date();
        final String outputFileName = "event_log_" + dateFormat.format(date) + ".log";
        return new File(context.getDir(RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE), outputFileName);
    }

    private void maybeCreateAndStartRtcEventLog(String streamId) {
        if (context == null || peers.get(streamId).peerConnection == null) {
            return;
        }
        if (!enableRtcEventLog) {
            Log.d(TAG, "RtcEventLog is disabled.");
            return;
        }
        rtcEventLog = new RtcEventLog(peers.get(streamId).peerConnection);
        rtcEventLog.start(createRtcEventLogOutputFile());
    }

    public void closeInternal() {
        Log.d(TAG, "Closing resources.");
        if (factory != null && aecDump) {
            factory.stopAecDump();
        }

        if(statsTimer != null) {
            statsTimer.cancel();
        }
        if(trackCheckerTimer != null) {
            trackCheckerTimer.cancel();
            trackCheckerTask.cancel();
        }

        if (rtcEventLog != null) {
            // RtcEventLog should stop before the peer connection is disposed.
            rtcEventLog.stop();
            rtcEventLog = null;
        }
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            Log.d(TAG, "Closing peer connections for " + entry.getValue().id);
            PeerConnection peerConnection = entry.getValue().peerConnection;
            if (peerConnection != null) {
                peerConnection.dispose();
                entry.getValue().peerConnection = null;
            }

            Log.d(TAG, "Closing data channels for " + entry.getValue().id);
            DataChannel dataChannel = entry.getValue().dataChannel;
            if (dataChannel != null) {
                dataChannel.dispose();
                entry.getValue().dataChannel = null;
            }
        }
        if(streamStoppedByUser) {
            peers.clear();
        }


        Log.d(TAG, "Closing audio source.");
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        Log.d(TAG, "Stopping capture.");
        if (videoCapturer != null && !videoCapturerStopped) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            videoCapturerStopped = true;
            //videoCapturer.dispose();
            //videoCapturer = null;
        }
        Log.d(TAG, "Closing video source.");
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }

        Log.d(TAG, "Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
        if(eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
        Log.d(TAG, "Closing peer connection done.");
        onPeerConnectionClosed();
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();
        streamStarted = false;
    }

    public boolean isHDVideo() {
        return isVideoCallEnabled() && videoWidth * videoHeight >= 1280 * 720;
    }

    private void getStats(String streamId) {
        PeerConnection pc = peers.get(streamId).peerConnection;
        if (pc == null || isError) {
            return;
        }
        pc.getStats(this::onPeerConnectionStatsReady);
    }

    public void enableStatsEvents(String streamId, boolean enable, int periodMs) {
        if (enable) {
            try {
                statsTimer = new Timer();
                statsTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executor.execute(() -> getStats(streamId));
                    }
                }, 0, periodMs);
            } catch (Exception e) {
                Log.e(TAG, "Can not schedule statistics timer", e);
            }
        } else {
            statsTimer.cancel();
        }
    }

    public void setAudioEnabled(final boolean enable) {
        this.audioCallEnabled = enable;

        executor.execute(() -> {
            enableAudio = enable;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(enableAudio);
            }
        });
    }

    public void setVideoEnabled(final boolean enable) {
        this.videoCallEnabled = enable;
        executor.execute(() -> {
            renderVideo = enable;
            if (localVideoTrack != null) {
                if (enable) {
                    startVideoSourceInternal();
                } else {
                    stopVideoSourceInternal();
                }
                localVideoTrack.setEnabled(renderVideo);
            }
            if (remoteVideoTrack != null) {
                remoteVideoTrack.setEnabled(renderVideo);
            }
        });
    }

    public void createOffer(String streamId) {
        executor.execute(() -> {
            PeerConnection pc = peers.get(streamId).peerConnection;
            if (pc != null && !isError) {
                Log.d(TAG, "PC Create OFFER");
                isInitiator = true;
                initDataChannel(streamId);
                pc.createOffer(getSdpObserver(streamId), sdpMediaConstraints);
            }
        });
    }

    public void createAnswer(String streamId) {
        executor.execute(() -> {
            PeerConnection pc = peers.get(streamId).peerConnection;
            if (pc != null && !isError) {
                Log.d(TAG, "PC create ANSWER");
                isInitiator = false;
                pc.createAnswer(getSdpObserver(streamId), sdpMediaConstraints);
            }
        });
    }

    public void addRemoteIceCandidate(String streamId, final IceCandidate candidate) {
        executor.execute(() -> {
            PeerConnection pc = peers.get(streamId).peerConnection;
            if (pc != null && !isError) {
                if (queuedRemoteCandidates != null) {
                    queuedRemoteCandidates.add(candidate);
                } else {
                    pc.addIceCandidate(candidate, new AddIceObserver() {
                        @Override
                        public void onAddSuccess() {
                            Log.d(TAG, "Candidate " + candidate + " successfully added.");
                        }
                        @Override
                        public void onAddFailure(String error) {
                            Log.d(TAG, "Candidate " + candidate + " addition failed: " + error);
                        }
                    });
                }
            }
        });
    }

    public void removeRemoteIceCandidates(String streamId, final IceCandidate[] candidates) {
        executor.execute(() -> {
            PeerConnection pc = peers.get(streamId).peerConnection;
            if (pc == null || isError) {
                return;
            }
            // Drain the queued remote candidates if there is any so that
            // they are processed in the proper order.
            drainCandidates(streamId);
            pc.removeIceCandidates(candidates);
        });
    }

    public void setRemoteDescription(String streamId, final SessionDescription desc) {
        executor.execute(() -> {
            PeerConnection pc = peers.get(streamId).peerConnection;
            if (pc == null || isError) {
                return;
            }
            String sdp = desc.description;
            if (preferIsac) {
                sdp = preferCodec(sdp, AUDIO_CODEC_ISAC, true);
            }
            if (isVideoCallEnabled()) {
                sdp = preferCodec(sdp, getSdpVideoCodecName(videoCodec), false);
            }
            if (audioStartBitrate > 0) {
                sdp = setStartBitrate(AUDIO_CODEC_OPUS, false, sdp, audioStartBitrate);
            }
            Log.d(TAG, "Set remote SDP.");
            SessionDescription sdpRemote = new SessionDescription(desc.type, sdp);
            pc.setRemoteDescription(getSdpObserver(streamId), sdpRemote);
        });
    }

    private void stopVideoSourceInternal() {
        if (videoCapturer != null && !videoCapturerStopped) {
            Log.d(TAG, "Stop video source.");
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            videoCapturerStopped = true;
        }
    }

    private void startVideoSourceInternal() {
        if (videoCapturer != null && videoCapturerStopped) {
            Log.d(TAG, "Restart video source.");
            videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
            videoCapturerStopped = false;
        }
    }

    public void setVideoMaxBitrate(@androidx.annotation.Nullable final Integer maxBitrateKbps) {
        executor.execute(() -> {
            if (localVideoSender == null || isError) {
                return;
            }
            Log.d(TAG, "Requested max video bitrate: " + maxBitrateKbps);
            if (localVideoSender == null) {
                Log.w(TAG, "Sender is not ready.");
                return;
            }

            RtpParameters parameters = localVideoSender.getParameters();
            if (parameters.encodings.size() == 0) {
                Log.w(TAG, "RtpParameters are not ready.");
                return;
            }

            for (RtpParameters.Encoding encoding : parameters.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps = maxBitrateKbps == null ? null : maxBitrateKbps * BPS_IN_KBPS;
            }
            if (!localVideoSender.setParameters(parameters)) {
                Log.e(TAG, "RtpSender.setParameters failed.");
            }
            Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
        });
    }

    @androidx.annotation.Nullable
    private AudioTrack createAudioTrack() {
        if (localAudioTrack == null) {
            audioSource = factory.createAudioSource(audioConstraints);
            localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localAudioTrack.setEnabled(enableAudio);
        }
        return localAudioTrack;
    }

    @androidx.annotation.Nullable
    private VideoTrack createVideoTrack(VideoCapturer capturer) {
        if (localVideoTrack == null && capturer != null) {
            surfaceTextureHelper =
                    SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(capturer.isScreencast());
            capturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            capturer.startCapture(videoWidth, videoHeight, videoFps);

            localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            localVideoTrack.setEnabled(renderVideo);
            localVideoTrack.addSink(localProxyVideoSink);
            videoCapturerStopped = false;
        }
        return localVideoTrack;
    }

    private void findVideoSender(String streamId) {
        for (RtpSender sender : peers.get(streamId).peerConnection.getSenders()) {
            if (sender.track() != null) {
                String trackType = sender.track().kind();
                if (trackType.equals(VIDEO_TRACK_TYPE)) {
                    Log.d(TAG, "Found video sender.");
                    localVideoSender = sender;
                }
            }
        }
    }

    private List<VideoTrack> getRemoteVideoTrackList(String streamId) {
        List<VideoTrack> videoTrackList = new ArrayList<>();
        for (RtpTransceiver transceiver : peers.get(streamId).peerConnection.getTransceivers())
        {
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (track instanceof VideoTrack) {
                videoTrackList.add((VideoTrack)track);
            }
        }
        return videoTrackList;
    }

    // Returns the remote VideoTrack, assuming there is only one.
    private @androidx.annotation.Nullable VideoTrack getRemoteVideoTrack(String streamId) {
        for (RtpTransceiver transceiver : peers.get(streamId).peerConnection.getTransceivers()) {
            MediaStreamTrack track = transceiver.getReceiver().track();
            if (track instanceof VideoTrack) {
                return (VideoTrack) track;
            }
        }
        return null;
    }

    private static String getSdpVideoCodecName(String codec) {
        switch (codec) {
            case VIDEO_CODEC_VP8:
                return VIDEO_CODEC_VP8;
            case VIDEO_CODEC_VP9:
                return VIDEO_CODEC_VP9;
            case VIDEO_CODEC_AV1:
                return VIDEO_CODEC_AV1;
            case VIDEO_CODEC_H264_HIGH:
            case VIDEO_CODEC_H264_BASELINE:
                return VIDEO_CODEC_H264;
            default:
                return VIDEO_CODEC_VP8;
        }
    }

    private static String getFieldTrials(Boolean videoFlexfecEnabled, boolean disableWebRtcAGCAndHPF) {
        String fieldTrials = "";
        if (videoFlexfecEnabled) {
            fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL;
            Log.d(TAG, "Enable FlexFEC field trial.");
        }
        fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
        if (disableWebRtcAGCAndHPF) {
            fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL;
            Log.d(TAG, "Disable WebRTC AGC field trial.");
        }
        return fieldTrials;
    }

    @SuppressWarnings("StringSplitter")
    private static String setStartBitrate(
            String codec, boolean isVideoCodec, String sdp, int bitrateKbps) {
        String[] lines = sdp.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                rtpmapLineIndex = i;
                break;
            }
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec + " codec");
            return sdp;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex]);

        // Check if a=fmtp string already exist in remote SDP for this codec and
        // update it with new bitrate parameter.
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                Log.d(TAG, "Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Update remote SDP line: " + lines[i]);
                sdpFormatUpdated = true;
                break;
            }
        }

        StringBuilder newSdpDescription = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                String bitrateSet;
                if (isVideoCodec) {
                    bitrateSet =
                            "a=fmtp:" + codecRtpMap + " " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
                } else {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
                            + (bitrateKbps * 1000);
                }
                Log.d(TAG, "Add remote SDP line: " + bitrateSet);
                newSdpDescription.append(bitrateSet).append("\r\n");
            }
        }
        return newSdpDescription.toString();
    }

    /** Returns the line number containing "m=audio|video", or -1 if no such line exists. */
    private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    private static String joinString(
            Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }
        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }
        return buffer.toString();
    }

    private static @androidx.annotation.Nullable String movePayloadTypesToFront(
            List<String> preferredPayloadTypes, String mLine) {
        // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));
        if (origLineParts.size() <= 3) {
            Log.e(TAG, "Wrong SDP media description format: " + mLine);
            return null;
        }
        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
        // Reconstruct the line with `preferredPayloadTypes` moved to the beginning of the payload
        // types.
        final List<String> newLineParts = new ArrayList<>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);
        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    private static String preferCodec(String sdp, String codec, boolean isAudio) {
        final String[] lines = sdp.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        if (mLineIndex == -1) {
            Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
            return sdp;
        }
        // A list with all the payload types with name `codec`. The payload types are integers in the
        // range 96-127, but they are stored as strings here.
        final List<String> codecPayloadTypes = new ArrayList<>();
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
        for (String line : lines) {
            Matcher codecMatcher = codecPattern.matcher(line);
            if (codecMatcher.matches()) {
                codecPayloadTypes.add(codecMatcher.group(1));
            }
        }
        if (codecPayloadTypes.isEmpty()) {
            Log.w(TAG, "No payload types with name " + codec);
            return sdp;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
        if (newMLine == null) {
            return sdp;
        }
        Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
        lines[mLineIndex] = newMLine;
        return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
    }

    private void drainCandidates(String streamId) {
        if (queuedRemoteCandidates != null) {
            Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
            for (IceCandidate candidate : queuedRemoteCandidates) {
                peers.get(streamId).peerConnection.addIceCandidate(candidate, new AddIceObserver() {
                    @Override
                    public void onAddSuccess() {
                        Log.d(TAG, "Candidate " + candidate + " successfully added.");
                    }
                    @Override
                    public void onAddFailure(String error) {
                        Log.d(TAG, "Candidate " + candidate + " addition failed: " + error);
                    }
                });
            }
            queuedRemoteCandidates = null;
        }
    }

    private void switchCameraInternal() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            if (!isVideoCallEnabled() || isError) {
                Log.e(TAG,
                        "Failed to switch camera. Video: " + isVideoCallEnabled() + ". Error : " + isError);
                return; // No video is sent or only one camera is available or error happened.
            }
            Log.d(TAG, "Switch camera");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
        }
    }

    private void changeCaptureFormatInternal(int width, int height, int framerate) {
        if (!isVideoCallEnabled() || isError) {
            Log.e(TAG,
                    "Failed to change capture format. Video: " + isVideoCallEnabled()
                            + ". Error : " + isError);
            return;
        }
        Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
        videoSource.adaptOutputFormat(width, height, framerate);
    }

    public void setDataChannelObserver(@androidx.annotation.Nullable IDataChannelObserver dataChannelObserver) {
        this.dataChannelObserver = dataChannelObserver;
    }

    public void setStreamMode(String streamMode) {
        this.streamMode = streamMode;
    }

    public String getStreamMode() {
        return streamMode;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setVideoCallEnabled(boolean videoCallEnabled) {
        this.videoCallEnabled = videoCallEnabled;
    }

    public boolean getVideoCallEnabled() {
        return videoCallEnabled;
    }

    public boolean getAudioCallEnabled() {
        return audioCallEnabled;
    }

    public String getCurrentSource() {
        return currentSource;
    }

    public void addPeerConnection(String streamId, @androidx.annotation.Nullable PeerConnection peerConnection) {
        PeerInfo peerInfo = new PeerInfo(streamId, streamMode);
        peerInfo.peerConnection = peerConnection;
        this.peers.put(streamId, peerInfo);
    }

    public List<VideoSink> getRemoteSinks() {
        return remoteSinks;
    }

    public void setInitiator(boolean initiator) {
        isInitiator = initiator;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setCustomCapturerEnabled(boolean customCapturerEnabled) {
        this.customCapturerEnabled = customCapturerEnabled;
    }

    public boolean isReconnectionEnabled() {
        return reconnectionEnabled;
    }

    public void setReconnectionEnabled(boolean reconnectionEnabled) {
        this.reconnectionEnabled = reconnectionEnabled;
    }

    public void setAutoPlayTracks(boolean autoPlayTracks) {
        this.autoPlayTracks = autoPlayTracks;
    }

    public boolean isReconnectionInProgress() {
        return reconnectionInProgress;
    }

    public void setCheckStreamIdValidity(boolean checkStreamIdValidity) {
        this.checkStreamIdValidity = checkStreamIdValidity;
    }
    public boolean isStreamStarted() {
        return streamStarted;
    }

    public void setRenderersProvidedAtStart(boolean renderersProvidedAtStart) {
        this.renderersProvidedAtStart = renderersProvidedAtStart;
    }

    public void setInputSampleRate(int sampleRate) {
        this.inputSampleRate = sampleRate;
    }


    public void setStereoInput(boolean stereoInput) {
        this.stereoInput = stereoInput;
    }

    public void setAudioInputFormat(int audioFormat) {
        this.audioInputFormat = audioFormat;
    }

    public void setCustomAudioFeed(boolean customAudioFeed) {
        this.customAudioFeed = customAudioFeed;
    }

    public CustomWebRtcAudioRecord getAudioInput() {
        return adm.getAudioInput();
    }

    public VideoCapturer getVideoCapturer() {
        return videoCapturer;
    }

    public void setRemoveVideoRotationExtention(boolean removeVideoRotationExtention) {
        this.removeVideoRotationExtention = removeVideoRotationExtention;
    }

    public SessionDescription getLocalDescription() {
        return localDescription;
    }

    public void setReconnectionHandler(Handler reconnectionHandler) {
        this.reconnectionHandler = reconnectionHandler;
    }
}
