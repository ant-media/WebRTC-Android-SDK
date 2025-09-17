/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package io.antmedia.webrtcandroidframework.core;

import android.app.Activity;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
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
import org.webrtc.IceCandidate;
import org.webrtc.IceCandidateErrorEvent;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
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
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.CustomWebRtcAudioRecord;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.PlayParams;
import io.antmedia.webrtcandroidframework.api.WebRTCClientConfig;
import io.antmedia.webrtcandroidframework.apprtc.AppRTCAudioManager;
import io.antmedia.webrtcandroidframework.websocket.AntMediaSignallingEvents;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;
import io.antmedia.webrtcandroidframework.websocket.Subscriber;
import io.antmedia.webrtcandroidframework.websocket.WebSocketHandler;

public class WebRTCClient implements IWebRTCClient, AntMediaSignallingEvents {
    private static final String TAG = "WebRTCClient";


    public enum Mode {
        PUBLISH, PLAY, P2P, MULTI_TRACK_PLAY
    }

    public static final String VIDEO_ROTATION_EXT_LINE = "a=extmap:3 urn:3gpp:video-orientation\r\n";
    public static final String USER_REVOKED_CAPTURE_SCREEN_PERMISSION = "User revoked permission to capture the screen.";
    public static int STAT_CALLBACK_PERIOD = 1000;

    protected final ProxyVideoSink localVideoSink = new ProxyVideoSink();
    protected final List<ProxyVideoSink> remoteVideoSinks = new ArrayList<>();

    protected Handler mainHandler;
    @Nullable
    public AppRTCAudioManager audioManager = null;
    private final long callStartedTimeMs = 0;

    protected EglBase eglBase;
    private String errorString = null;

    private boolean streamStoppedByUser = false;
    private boolean reconnectionInProgress = false;

    private boolean autoPlayTracks = false;
    private boolean waitingForPlay = false;
    private VideoCapturer videoCapturer;
    private VideoTrack localVideoTrack;
    private Handler handler = new Handler();
    private WebSocketHandler wsHandler;

    private final ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private final StatsCollector statsCollector = new StatsCollector();

    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String VIDEO_TRACK_TYPE = "video";
    private static final String VIDEO_CODEC_VP8 = "VP8";
    private static final String VIDEO_CODEC_VP9 = "VP9";
    private static final String VIDEO_CODEC_H264 = "H264";
    private static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    private static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    private static final String VIDEO_CODEC_AV1 = "AV1";
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
    private static final int BPS_IN_KBPS = 1000;

    // Executor thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Timer statsTimer;

    @androidx.annotation.Nullable
    private PeerConnectionFactory factory;

    public static class PeerInfo {

        public PeerInfo(String id, Mode mode) {
            this.id = id;
            this.mode = mode;
        }

        public SessionDescription localDescription;

        // Queued remote ICE candidates are consumed only after both local and
        // remote descriptions are set. Similarly local ICE candidates are sent to
        // remote peer after both local and remote description are set.
        private List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();

        public String id;
        public PeerConnection peerConnection;
        public DataChannel dataChannel;
        public Mode mode;
        public String token;
        public boolean videoCallEnabled;
        public boolean audioCallEnabled;
        public String subscriberId;
        public String subscriberName;
        public String subscriberCode;
        public String streamName;
        public String mainTrackId;
        public String metaData;
        public boolean disableTracksByDefault = false;

        public boolean restartIce = false;

        public SessionDescription getLocalDescription() {
            return localDescription;
        }

        public void setLocalDescription(SessionDescription localDescription) {
            this.localDescription = localDescription;
        }

        public List<IceCandidate> getQueuedRemoteCandidates() {
            return queuedRemoteCandidates;
        }

        public void setQueuedRemoteCandidates(List<IceCandidate> queuedRemoteCandidates) {
            this.queuedRemoteCandidates = queuedRemoteCandidates;
        }

    }

    private final Map<String, PeerInfo> peers = new ConcurrentHashMap<>();
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

    private boolean isInitiator;

    private boolean renderVideo = true;
    @androidx.annotation.Nullable
    public RtpSender localVideoSender;

    @androidx.annotation.Nullable
    private AudioTrack localAudioTrack;

    @androidx.annotation.Nullable
    public JavaAudioDeviceModule adm;

    private WebRTCClientConfig config;

    private boolean removeVideoRotationExtension = true;

    //reconnection parameters
    private Handler peerReconnectionHandler = new Handler();
    private Handler publishReconnectionHandler = new Handler();
    private Handler playReconnectionHandler = new Handler();

    private Runnable peerReconnectorRunnable;
    private Runnable publishReconnectorRunnable;
    private Runnable playReconnectorRunnable;

    public static final long PEER_RECONNECTION_DELAY_MS = 3000;
    public static final long PEER_RECONNECTION_RETRY_DELAY_MS = 10000;

    private boolean released = false;

    private String roomId;

    private BlackFrameSender blackFrameSender;

    private boolean sendVideoEnabled = true;

    private boolean sendAudioEnabled = true;


    public void createReconnectorRunnables() {
        //only used in conference.
        publishReconnectorRunnable = () -> {
            if(released || streamStoppedByUser){
                return;
            }
            publishReconnectionHandler.postDelayed(publishReconnectorRunnable, PEER_RECONNECTION_RETRY_DELAY_MS);

            for (PeerInfo peerInfo : peers.values()) {
                PeerConnection pc = peerInfo.peerConnection;
                if (pc == null ||
                        (pc.iceConnectionState() != PeerConnection.IceConnectionState.CHECKING
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.CONNECTED
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.COMPLETED)) {


                    if (pc != null) {
                        pc.close();
                        /*
                        This is a FIX of a reconnection bug.
                        If dispose is used instead of close, in one of consequent reconnection attempts segmentation fault occurs.
                        pc.dispose();
                        */
                    }

                    config.webRTCListener.onReconnectionAttempt(peerInfo.id);
                    if (peerInfo.mode.equals(Mode.PUBLISH)) {

                        Log.d(TAG, "Reconnect attempt for publish");
                        wsHandler.stop(peerInfo.id);
                        wsHandler.startPublish(peerInfo.id, peerInfo.token, peerInfo.videoCallEnabled, peerInfo.audioCallEnabled, peerInfo.subscriberId, peerInfo.subscriberCode, peerInfo.streamName, peerInfo.mainTrackId);


                    }
                }
            }
        };
        //only used in conference.
        playReconnectorRunnable = () -> {
            if(released || streamStoppedByUser){
                return;
            }
            releaseRemoteRenderers();

            playReconnectionHandler.postDelayed(playReconnectorRunnable, PEER_RECONNECTION_RETRY_DELAY_MS);

            for (PeerInfo peerInfo : peers.values()) {
                PeerConnection pc = peerInfo.peerConnection;
                if (pc == null ||
                        (pc.iceConnectionState() != PeerConnection.IceConnectionState.CHECKING
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.CONNECTED
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.COMPLETED)) {


                    if (pc != null) {
                        pc.close();
                        /*
                        This is a FIX of a reconnection bug.
                        If dispose is used instead of close, in one of consequent reconnection attempts segmentation fault occurs.
                        pc.dispose();
                        */
                    }

                    config.webRTCListener.onReconnectionAttempt(peerInfo.id);
                    if (peerInfo.mode.equals(Mode.PLAY)) {
                        Log.d(TAG, "Reconnect attempt for play");

                        play(peerInfo.id,
                                peerInfo.token,
                                null,
                                peerInfo.subscriberId,
                                peerInfo.subscriberCode,
                                peerInfo.metaData
                        );
                    }
                }
            }
        };

        peerReconnectorRunnable = () -> {
            if(released || streamStoppedByUser){
                return;
            }
            peerReconnectionHandler.postDelayed(peerReconnectorRunnable, PEER_RECONNECTION_RETRY_DELAY_MS);

            for (PeerInfo peerInfo : peers.values()) {
                PeerConnection pc = peerInfo.peerConnection;
                if (pc == null ||
                        (pc.iceConnectionState() != PeerConnection.IceConnectionState.CHECKING
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.CONNECTED
                                && pc.iceConnectionState() != PeerConnection.IceConnectionState.COMPLETED)) {


                    if (pc != null) {
                        pc.close();
                        /*
                        This is a FIX of a reconnection bug.
                        If dispose is used instead of close, in one of consequent reconnection attempts segmentation fault occurs.
                        pc.dispose();
                        */
                    }

                    config.webRTCListener.onReconnectionAttempt(peerInfo.id);
                    if (peerInfo.mode.equals(Mode.PUBLISH)) {

                        Log.d(TAG, "Reconnect attempt for publish");
                        wsHandler.stop(peerInfo.id);
                        wsHandler.startPublish(peerInfo.id, peerInfo.token, peerInfo.videoCallEnabled, peerInfo.audioCallEnabled, peerInfo.subscriberId, peerInfo.subscriberCode, peerInfo.streamName, peerInfo.mainTrackId);


                    } else if (peerInfo.mode.equals(Mode.PLAY)) {
                        if(config.remoteVideoRenderers.size() == 1){ //if its multitrack play dont release.
                            releaseRemoteRenderers();
                        }
                        Log.d(TAG, "Reconnect attempt for play");

                        play(peerInfo.id,
                                peerInfo.token,
                                null,
                                peerInfo.subscriberId,
                                peerInfo.subscriberCode,
                                peerInfo.metaData
                        );

                    } else if (peerInfo.mode.equals(Mode.P2P)) {
                        releaseRemoteRenderers();
                        Log.d(TAG, "Reconnect attempt for P2P");
                        wsHandler.leaveFromP2P(peerInfo.id);
                        config.localVideoRenderer.setZOrderOnTop(true);
                        join(peerInfo.id, peerInfo.token);
                    }
                }
            }
        };
    }

    public WebRTCClient(WebRTCClientConfig config) {
        this.config = config;
        config.webRTCListener.setWebRTCClient(this);
        mainHandler = new Handler(config.activity.getMainLooper());
        iceServers.add(PeerConnection.IceServer.builder(config.stunServerUri)
                .createIceServer());
        sendAudioEnabled = config.audioCallEnabled;
        sendVideoEnabled = config.videoCallEnabled;

        if(config.turnServerUri != null) {
            iceServers.add(PeerConnection.IceServer.builder(config.turnServerUri)
                            .setUsername(config.turnServerUserName)
                            .setPassword(config.turnServerPassword)
                    .createIceServer());
        }

        if (config.initiateBeforeStream) {
            init();
        }
    }

    @Override
    public WebRTCClientConfig getConfig() {
        return config;
    }

    public void setConfig(WebRTCClientConfig config) {
        this.config = config;
    }

    public SDPObserver getSdpObserver(String streamId) {
        return new SDPObserver(streamId);
    }

    public void joinToConferenceRoom(String roomId, String streamId) {
        this.roomId = roomId;

        publish(streamId, "",
                true, true,
                "",
                "",
                "",
                roomId);
    }

    public void joinToConferenceRoom(String roomId, String streamId, boolean videoCallEnabled, boolean audioCallEnabled, String token, String subscriberId, String subscriberCode, String streamName) {

        this.roomId = roomId;

        publish(streamId, token,
                videoCallEnabled, audioCallEnabled,
                subscriberId,
                subscriberCode,
                streamName,
                roomId);
    }

    public void joinToConferenceRoom(String roomId) {
        this.roomId = roomId;

        play(roomId);
    }

    public void leaveFromConference(String roomId) {
        String publishId = getPublishStreamId();
        //publishId can be null if we are player only in conference
        if (publishId != null) {
            stop(publishId);
        }
        stop(roomId);
    }

    public void getRoomInfo(String roomId, String streamId) {
        handler.post(() -> {
            if (wsHandler != null) {
                wsHandler.getRoomInfo(roomId, streamId);
            }
        });
    }


    // Implementation detail: observe ICE & stream changes and react accordingly.
    public class PCObserver implements PeerConnection.Observer {

        private final String streamId;

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
                } else if (newState == PeerConnection.IceConnectionState.DISCONNECTED || newState == PeerConnection.IceConnectionState.CLOSED) {
                    onIceDisconnected(streamId);
                } else if (newState == PeerConnection.IceConnectionState.FAILED) {
                    onIceFailed(streamId);
                }
            });
        }

        @Override
        public void onConnectionChange(final PeerConnection.PeerConnectionState newState) {
            executor.execute(() -> {
                Log.d(TAG, "PeerConnectionState: " + newState);
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    onConnected(streamId);
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
        }

        @Override
        public void onRemoveStream(final MediaStream stream) {
        }

        @Override
        public void onDataChannel(final DataChannel dc) {
            Log.d(TAG, "New Data channel " + dc.label());

            if (!config.dataChannelEnabled)
                return;

            PeerInfo peerInfo = peers.get(streamId);
            if (peerInfo != null && peerInfo.dataChannel == null) {
                peerInfo.dataChannel = dc;
            }
            dc.registerObserver(new DataChannelInternalObserver(dc));
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "Renegotiation needed.");
            PeerInfo peerInfo = getPeerInfoFor(streamId);

            if (peerInfo != null && getPeerInfoFor(streamId).mode == Mode.PUBLISH) {
                createOffer(streamId);
            }
        }

        @Override
        public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
            MediaStreamTrack addedTrack = receiver.track();

            if (addedTrack == null) {
                return;
            }
            String receiverId = receiver.id();

            String videoTrackId = receiverId.substring(DataChannelConstants.TRACK_ID_PREFIX.length());

            Log.d(TAG, "onAddTrack " + addedTrack.kind() + " " + addedTrack.id() + " " + addedTrack.state());

            if (addedTrack instanceof VideoTrack) {
                VideoTrack videoTrack = (VideoTrack) addedTrack;
                config.webRTCListener.onNewVideoTrack(videoTrack, videoTrackId);
            }
        }

        @Override
        public void onRemoveTrack(RtpReceiver receiver) {
            MediaStreamTrack removedTrack = receiver.track();
            if (removedTrack == null) {
                return;
            }
            Log.d("antmedia", "on remove track " + removedTrack.kind() + " " + removedTrack.id() + " " + removedTrack.state());
            if (removedTrack instanceof VideoTrack) {
                config.webRTCListener.onVideoTrackEnded((VideoTrack) removedTrack);
            }

        }
    }

    @Override
    public void setRendererForVideoTrack(SurfaceViewRenderer renderer, VideoTrack videoTrack) {
        mainHandler.post(() -> {
            ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
            if (renderer != null) {
                remoteVideoSink.setTarget(renderer);
                renderer.init(eglBase.getEglBaseContext(), null);
                renderer.setScalingType(config.scalingType);
                renderer.setEnableHardwareScaler(true);
                renderer.setTag(renderer.getId(), remoteVideoSink);
            }
            videoTrack.addSink(remoteVideoSink);
            remoteVideoSinks.add(remoteVideoSink);
        });
    }

    // Implementation detail: handle offer creation/signaling and answer setting,
    // as well as adding remote ICE candidates once the answer SDP is set.
    public class SDPObserver implements SdpObserver {
        private final String streamId;

        SDPObserver(String streamId) {
            this.streamId = streamId;
        }

        @Override
        public void onCreateSuccess(final SessionDescription desc) {
            String sdp = desc.description;
            if (preferIsac) {
                sdp = preferCodec(sdp, AUDIO_CODEC_ISAC, true);
            }
            if (config.videoCallEnabled) {
                sdp = preferCodec(sdp, getSdpVideoCodecName(config.videoCodec), false);
            }

            if (removeVideoRotationExtension) {
                sdp = sdp.replace(VIDEO_ROTATION_EXT_LINE, "");
            }

            final SessionDescription newDesc = new SessionDescription(desc.type, sdp);
            PeerInfo peerInfo = getPeerInfoFor(streamId);
            peerInfo.setLocalDescription(newDesc);
            executor.execute(() -> {
                PeerConnection pc = peerInfo.peerConnection;
                if (pc != null) {
                    Log.d(TAG, "Set local SDP from " + desc.type);
                    pc.setLocalDescription(this, newDesc);
                }
            });
        }

        @Override
        public void onSetSuccess() {
            Log.i(TAG, "onSetSuccess: ");

            executor.execute(() -> {
                PeerInfo peerInfo = getPeerInfoFor(streamId);
                if (peerInfo == null) {
                    return;
                }
                PeerConnection pc = peerInfo.peerConnection;
                if (pc == null) {
                    return;
                }

                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (pc.getRemoteDescription() == null) {
                        // We've just set our local SDP so time to send it.
                        Log.d(TAG, "Local SDP set succesfully");
                        onLocalDescription(streamId, peerInfo.getLocalDescription());
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
                        onLocalDescription(streamId, peerInfo.getLocalDescription());
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

    public void init() {
        if(config.videoCallEnabled && !PermissionHandler.checkCameraPermissions(config.activity)) {
            Toast.makeText(config.activity,"Camera permissions not granted. Cannot init.", Toast.LENGTH_LONG).show();
            Log.e(TAG,"Camera permissions not granted. Cannot init.");
            return;
        }

        if (config.reconnectionEnabled) {
            createReconnectorRunnables();
        }

        initializeRenderers();

        initializeParameters();

        initializePeerConnectionFactory();

        if (config.videoCallEnabled) {
            initializeVideoCapturer();
        }

        connectWebSocket();
        released = false;
    }

    public void initializeParameters() {
        // If capturing format is not specified for screencapture, use screen resolution.
        if (config.videoSource.equals(StreamSource.SCREEN)) {
            DisplayMetrics displayMetrics = getDisplayMetrics();
            config.videoWidth = displayMetrics.widthPixels;
            config.videoHeight = displayMetrics.heightPixels;
        }
    }

    public void initializeRenderers() {
        if (eglBase == null) {
            eglBase = EglBase.create();
        }

        //init local renderer if available
        if (config.localVideoRenderer != null && localVideoSink.getTarget() == null) {
            config.localVideoRenderer.init(eglBase.getEglBaseContext(), null);
            config.localVideoRenderer.setScalingType(config.scalingType);
            config.localVideoRenderer.setZOrderMediaOverlay(true);
            config.localVideoRenderer.setEnableHardwareScaler(true /* enabled */);
            localVideoSink.setTarget(config.localVideoRenderer);
        }

    }

    public void initializePeerConnectionFactory() {
        if (factory != null) {
            return;
        }

        // Create peer connection client.
        Log.d(TAG, "Preferred video codec: " + getSdpVideoCodecName(config.videoCodec));
        final String fieldTrials = getFieldTrials(config.videoFlexfecEnabled, config.disableWebRtcAGCAndHPF);
        executor.execute(() -> {
            Log.d(TAG, "Initialize WebRTC. Field trials: " + fieldTrials);
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(config.activity)
                            .setFieldTrials(fieldTrials)
                            .setEnableInternalTracer(true)
                            .createInitializationOptions());
        });


        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        createPeerConnectionFactory(options);
    }

    public void initializeAudioManager() {
        if (config.audioCallEnabled && audioManager == null) {
            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(this.config.activity.getApplicationContext());
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Starting the audio manager...");
            // This method will be called each time the number of available audio devices has changed.
            audioManager.start(this::onAudioManagerDevicesChanged, config.bluetoothEnabled);
        }
    }


    public void initializeVideoCapturer() {
        if (videoCapturer != null) {
            return;
        }

        if (config.videoCallEnabled) {
            videoCapturer = createVideoCapturer(config.videoSource);
        }

        executor.execute(() -> {
            createMediaConstraintsInternal();
            createVideoTrack(videoCapturer);
            createAudioTrack();
        });
    }

    public void setBitrate(int bitrate) {
        setVideoMaxBitrate(bitrate);
    }

    public void connectWebSocket() {
        if (wsHandler == null) {
            Log.i(TAG, "WebsocketHandler is null and creating a new instance");
            wsHandler = new WebSocketHandler(this, handler);
            wsHandler.connect(config.serverUrl);
            if (config.reconnectionEnabled) {
                wsHandler.setupWsReconnection();
            }
        } else if (!wsHandler.isConnected()) {
            Log.i(TAG, "WebSocketHandler already exists but not connected. Disconnecting and connect again.");
            wsHandler.disconnect(true);
            wsHandler.connect(config.serverUrl);
        }
    }

    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) config.activity.getSystemService(Activity.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
        return displayMetrics;
    }

    public @Nullable VideoCapturer createScreenCapturer() {
        return new ScreenCapturerAndroid(config.mediaProjectionIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                reportError(getPublishStreamId(), USER_REVOKED_CAPTURE_SCREEN_PERMISSION);
            }
        });
    }

    @Nullable
    public VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        if (config.videoSource == StreamSource.FRONT_CAMERA) {
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

    public void setSwappedFeeds(boolean isSwappedFeeds) {
        localVideoSink.setTarget(isSwappedFeeds ? config.remoteVideoRenderers.get(0) : config.localVideoRenderer);
        remoteVideoSinks.get(0).setTarget(isSwappedFeeds ? config.localVideoRenderer : config.remoteVideoRenderers.get(0));
        config.remoteVideoRenderers.get(0).setMirror(isSwappedFeeds);
        config.localVideoRenderer.setMirror(!isSwappedFeeds);
    }


    public void stop(String streamId) {
        stop(streamId, true);
    }


    public void stop(String streamId, boolean byUser) {
        Log.i(getClass().getSimpleName(), "Stopping stream");
        streamStoppedByUser = byUser;

        if (wsHandler != null && wsHandler.isConnected()) {

            PeerInfo peerInfo = getPeerInfoFor(streamId);
            if(peerInfo != null && peerInfo.mode == Mode.P2P){
                wsHandler.leaveFromP2P(streamId);
                release(byUser); // releasing SDK here might not be practical for all P2P users.
                return;
            }


            wsHandler.stop(streamId);
        }
    }

    @Override
    public void switchCamera() {
        if (config.videoSource == StreamSource.FRONT_CAMERA) {
            config.videoSource = StreamSource.REAR_CAMERA;
        } else if (config.videoSource == StreamSource.REAR_CAMERA) {
            config.videoSource = StreamSource.FRONT_CAMERA;
        }
        executor.execute(this::switchCameraInternal);
    }

    @Override
    public void getBroadcastObject(String streamId) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.getBroadcastObject(streamId);
        }
    }

    @Override
    public void onWebSocketConnected() {
        Log.i(TAG, "WebSocket connected.");

        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onWebSocketConnected();
            }

        });

        publishPlayIfRequested();
    }

    private void publishPlayIfRequested() {
        if(wsHandler == null){
            return;
        }
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            PeerInfo peerInfo = entry.getValue();
            Mode peerMode = peerInfo.mode;
            if (peerMode == Mode.PUBLISH && peerInfo.peerConnection == null) {
                Log.i(TAG, "Processing publish request for peer streamId: " + peerInfo.id);
                wsHandler.startPublish(peerInfo.id, peerInfo.token, peerInfo.videoCallEnabled, peerInfo.audioCallEnabled, peerInfo.subscriberId, peerInfo.subscriberCode, peerInfo.streamName, peerInfo.mainTrackId);
            }

            if (peerMode == Mode.PLAY && peerInfo.peerConnection == null) {
                Log.i(TAG, "Processing play request for peer streamId: " + peerInfo.id);
                wsHandler.startPlay(peerInfo.id, peerInfo.token, null, peerInfo.subscriberId, peerInfo.subscriberName, peerInfo.subscriberCode, peerInfo.metaData, peerInfo.disableTracksByDefault);
            }
        }
    }

    public void publish(String streamId) {
        publish(streamId, null, true, true,
                null, null, streamId, null);
    }


    public void publish(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled,
                        String subscriberId, String subscriberCode, String streamName, String mainTrackId) {
        Log.i(TAG, "Publish: " + streamId);

        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPublishAttempt(streamId);
            }
        });

        createPeerInfo(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, "", subscriberCode, streamName, mainTrackId, null, false, Mode.PUBLISH);
        init();

        if(!PermissionHandler.checkPublishPermissions(config.activity, config.bluetoothEnabled, videoCallEnabled || this.config.videoCallEnabled)){
            Toast.makeText(config.activity,"Publish permissions not granted. Cant publish.", Toast.LENGTH_LONG).show();
            Log.e(TAG,"Publish permissions not granted. Cant publish.");
            return;
        }

        initializeAudioManager();

        if (isWebSocketConnected()) {
            Log.i(TAG, "Publish request sent through ws for stream: " + streamId);
            wsHandler.startPublish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, mainTrackId);
        } else {
            Log.w(TAG, "Websocket is not connected. Set publish requested. It will be processed when ws is connected.");
        }
    }

    private void createPeerInfo(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled, String subscriberId, String subscriberName, String subscriberCode, String streamName, String mainTrackId, String metaData, boolean disableTracksByDefault, Mode mode) {
        PeerInfo peerInfo;
        peerInfo = new PeerInfo(streamId, mode);
        peerInfo.token = token;
        peerInfo.videoCallEnabled = videoCallEnabled || config.videoCallEnabled;
        peerInfo.audioCallEnabled = audioCallEnabled || config.audioCallEnabled;
        peerInfo.subscriberId = subscriberId;
        peerInfo.subscriberName = subscriberName;
        peerInfo.subscriberCode = subscriberCode;
        peerInfo.streamName = streamName;
        peerInfo.mainTrackId = mainTrackId;
        peerInfo.metaData = metaData;
        peerInfo.disableTracksByDefault = disableTracksByDefault;
        peers.put(streamId, peerInfo);
    }

    public void play(String streamId) {
        play(streamId, "", null, "", "", "");
    }

    public void play(String streamId, String[] tracks) {
        play(streamId, "", tracks, "", "", "");
    }

    @Override
    public void play(PlayParams params) {
        Log.i(TAG, "Play: " + params.getStreamId());
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPlayAttempt(params.getStreamId());
            }
        });
        createPeerInfo(params.getStreamId(), params.getToken(), false, false, params.getSubscriberId(), params.getSubscriberName(), params.getSubscriberCode(), "", "", params.getViewerInfo(), params.isDisableTracksByDefault(), Mode.PLAY);

        if (!isReconnectionInProgress()) {
            init();
        }

        if(!PermissionHandler.checkPlayPermissions(config.activity, config.bluetoothEnabled)){
            Toast.makeText(config.activity,"Play permissions not granted. Cant play.", Toast.LENGTH_LONG).show();
            Log.e(TAG,"Play permissions not granted. Cant play.");
            return;
        }

        initializeAudioManager();

        Log.i(TAG, "Play: "+params.getStreamId());

        if (isWebSocketConnected()) {
            Log.i(TAG, "Play request sent through ws for stream: " + params.getStreamId());
            wsHandler.startPlay(params.getStreamId(), params.getToken(), params.getTracks(), params.getSubscriberId(), params.getSubscriberName(), params.getSubscriberCode(), params.getViewerInfo(), params.isDisableTracksByDefault());
        } else {
            Log.w(TAG, "Websocket is not connected. Set play requested. It will be processed when ws is connected.");
        }
    }

    public void play(String streamId, String token, String[] tracks, String subscriberId, String subscriberCode, String viewerInfo) {
        PlayParams params = new PlayParams(streamId, token, tracks, subscriberId, "", subscriberCode, viewerInfo, false);
        play(params);
    }

    public void join(String streamId) {
        join(streamId, "");
    }
  
    public void registerPushNotificationToken(String subscriberId, String authToken, String pushNotificationToken, String tokenType) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.registerPushNotificationToken(subscriberId, authToken, pushNotificationToken, tokenType);
        }
    }

    public void sendPushNotification(String subscriberId, String authToken, JSONObject pushNotificationContent, JSONArray receiverSubscriberIdArray) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.sendPushNotification(subscriberId, authToken, pushNotificationContent, receiverSubscriberIdArray);
        }
    }
  
    public void join(String streamId, String token) {
        Log.e(TAG, "Join: " + streamId);

        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onJoinAttempt(streamId);
            }
        });
        PeerInfo peerInfo = new PeerInfo(streamId, Mode.P2P);
        peerInfo.token = token;
        peers.put(streamId, peerInfo);

        init();

        initializeAudioManager();


        wsHandler.joinToPeer(streamId, token);
    }

    public void getTrackList(String streamId, String token) {
        init();
        wsHandler.getTrackList(streamId, token);
    }

    public void enableTrack(String streamId, String trackId, boolean enabled) {
        wsHandler.enableTrack(streamId, trackId, enabled);
    }

    // Should be called from UI thread
    private void callConnected(String streamId) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        // Enable statistics callback.
        enableStatsEvents(streamId, true, STAT_CALLBACK_PERIOD);
    }

    /**
     * Handles changes in audio devices reported by the audio manager, such as
     * switching from a wired headset to a speakerphone.
     *
     * @param device The currently selected audio device.
     * @param availableDevices A set of available audio devices.
     */
    public void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        if(audioManager == null){
            return;
        }
        Log.i(TAG, "Audio devices changed. Available: " + availableDevices + ", Selected: " + device);

        // Prioritize Bluetooth if available
        if (availableDevices.contains(AppRTCAudioManager.AudioDevice.BLUETOOTH)) {
            Log.i(TAG, "Bluetooth device found, switching to Bluetooth.");
            audioManager.selectAudioDevice(AppRTCAudioManager.AudioDevice.BLUETOOTH);
            return;
        }

        // Fallback to the current selected device
        Log.i(TAG, "Switching to the selected device: " + device);
        audioManager.selectAudioDevice(device);

    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    public void release(boolean closeWebsocket) {
        if(released){
            return;
        }
        released = true;
        Log.i(getClass().getSimpleName(), "Releasing resources");

        if (closeWebsocket && wsHandler != null) {
            wsHandler.disconnect(true);
            wsHandler.stopReconnector();
            wsHandler = null;
        }
        if (config.localVideoRenderer != null) {
            releaseRenderer(config.localVideoRenderer, localVideoTrack, localVideoSink);
        }

        for (SurfaceViewRenderer remoteVideoRenderer : config.remoteVideoRenderers) {
            if (remoteVideoRenderer.getTag() != null) {
                releaseRenderer(remoteVideoRenderer);
            }
        }

        localVideoTrack = null;
        localAudioTrack = null;

        remoteVideoSinks.clear();

        mainHandler.post(() -> {
            //if closeInternal works before releasing renderer, app stucks
            executor.execute(this::closeInternal);
        });


        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        config.webRTCListener.onShutdown();

    }

    public void releaseRenderer(SurfaceViewRenderer renderer, VideoTrack track, VideoSink sink) {
        mainHandler.post(() -> {
            VideoTrack videoTrack = (track != null) ? track : (VideoTrack) renderer.getTag();
            VideoSink videoSink = (sink != null) ? sink : (VideoSink) renderer.getTag(renderer.getId());

            if (videoTrack != null && videoSink != null)
                videoTrack.removeSink(videoSink);
            renderer.clearAnimation();
            mainHandler.postAtFrontOfQueue(renderer::clearImage);

            renderer.release();
            renderer.setTag(null);
        });
    }

    private void releaseRemoteRenderers() {
        for (SurfaceViewRenderer remoteVideoRenderer : config.remoteVideoRenderers) {
            if (remoteVideoRenderer.getTag() != null) {
                releaseRenderer(remoteVideoRenderer);
            }
        }
    }

    public boolean isWebSocketConnected() {
        if (wsHandler == null) {
            return false;
        }
        return wsHandler.isConnected();
    }

    public void releaseRenderer(SurfaceViewRenderer renderer) {
        releaseRenderer(renderer, null, null);
    }

    public void reportError(String streamId, final String description) {
        this.handler.post(() -> {
            errorString = description;
            if (config.webRTCListener != null) {
                config.webRTCListener.onError(description, streamId);
            }
        });
    }

    public void changeVideoSource(StreamSource newSource) {
            if (!config.videoSource.equals(newSource)) {
                if (newSource.equals(StreamSource.SCREEN) && adm != null) {
                    adm.setMediaProjection(config.mediaProjection);
                }

                Log.i(TAG, "Change video source started!");
                VideoCapturer newVideoCapturer = createVideoCapturer(newSource);
                /* When user try to change video source after stopped the publishing
                 * peerConnectionClient will null, until start another broadcast
                 */
                changeVideoCapturer(newVideoCapturer);
                config.videoSource = newSource;
                Log.i(TAG, "Change video source done!");
            }

    }

    public @Nullable VideoCapturer createVideoCapturer(StreamSource source) {
        final VideoCapturer videoCapturer;
        config.videoSource = source;

        if (StreamSource.SCREEN.equals(source)) {
            videoCapturer = createScreenCapturer();
        } else if (StreamSource.CUSTOM.equals(source)) {
            videoCapturer = createCustomVideoCapturer();
        } else {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this.config.activity));
        }
        if (videoCapturer == null) {
            reportError(getPublishStreamId(), "Failed to create capturer:" + source);
        }
        return videoCapturer;
    }

    @NonNull
    public VideoCapturer createCustomVideoCapturer() {
        return new CustomVideoCapturer();
    }

    public String getPublishStreamId() {
        for (PeerInfo peerInfo : peers.values()) {
            if (peerInfo.mode.equals(Mode.PUBLISH)) {
                return peerInfo.id;
            }
        }
        return null;
    }

    public String getMultiTrackStreamId() {
        for (PeerInfo peerInfo : peers.values()) {
            if (peerInfo.mode.equals(Mode.MULTI_TRACK_PLAY)) {
                return peerInfo.id;
            }
        }
        return null;
    }

    public void setWsHandler(WebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    @Override
    public String getError() {
        return errorString;
    }

    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    public void onLocalDescription(String streamId, final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        this.handler.post(() -> {
            if (wsHandler != null) {
                Log.d(TAG, "Sending " + sdp.type + ", delay=" + delta + "ms");
                if (isInitiator) {
                    wsHandler.sendConfiguration(streamId, sdp, "offer");
                } else {
                    wsHandler.sendConfiguration(streamId, sdp, "answer");
                }
            }

            if (config.videoStartBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + config.videoStartBitrate);
                setVideoMaxBitrate(config.videoStartBitrate);
            }
        });
    }

    public void onIceConnected(String streamId) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        this.handler.post(() -> {
            Log.d(TAG, "ICE connected, delay=" + delta + "ms");
            PeerInfo peerInfo = getPeerInfoFor(streamId);
            peerInfo.restartIce = false;
            callConnected(streamId);

            if (config.webRTCListener != null) {
                config.webRTCListener.onIceConnected(streamId);
            }
        });
    }

    public void rePublishPlay() {
        if (streamStoppedByUser || reconnectionInProgress) {
            return;
        }
        reconnectionInProgress = true;

        if(isConference()){
            Log.i(TAG, "Conference! Will try to republish in  " + PEER_RECONNECTION_DELAY_MS + " ms.");
            publishReconnectionHandler.postDelayed(publishReconnectorRunnable, PEER_RECONNECTION_DELAY_MS);
        }else{
            Log.i(TAG, "Peer was connected before. Will try to republish/replay in " + PEER_RECONNECTION_DELAY_MS + " ms.");
            peerReconnectionHandler.postDelayed(peerReconnectorRunnable, PEER_RECONNECTION_DELAY_MS);
        }
    }

    public void onIceDisconnected(String streamId) {

        this.handler.post(() -> {
            Log.d(TAG, "ICE disconnected");

           if (config.webRTCListener != null) {
                config.webRTCListener.onIceDisconnected(streamId);
            }

           if (streamStoppedByUser) {
               release(true);
               return;
           }

           if (config.reconnectionEnabled) {
               rePublishPlay();
           }

           if (isConference()) {
               releaseRemoteRenderers();
           }

        });
    }

    private boolean isConference(){
        return roomId != null;
    }

    private boolean isAllPeersConnected() {
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            PeerConnection peerConnection = entry.getValue().peerConnection;
            if(peerConnection == null){
                return false;
            }
            PeerConnection.PeerConnectionState peerConnectionState = peerConnection.connectionState();
            if(peerConnectionState != PeerConnection.PeerConnectionState.CONNECTED){
                return false;
            }
        }
        return true;
    }

    public void onIceFailed(String streamId) {
        PeerInfo peerInfo = getPeerInfoFor(streamId);

        this.handler.post(() -> {
            Log.d(TAG, "ICE failed");
            peerInfo.restartIce = true;
            PeerConnection peerConnection = getPeerConnectionFor(streamId);
            if (peerConnection != null) {
                Log.d(TAG, "Restarting ice.");
                peerConnection.restartIce();
            }
        });
    }

    private boolean isPublishConnected(){
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            PeerConnection peerConnection = entry.getValue().peerConnection;
            if(peerConnection == null){
                return false;
            }
            PeerConnection.PeerConnectionState peerConnectionState = peerConnection.connectionState();
            if(entry.getValue().mode == Mode.PUBLISH && peerConnectionState != PeerConnection.PeerConnectionState.CONNECTED){
                return false;
            }
        }
        return true;
    }

    private boolean isPlayConnected(){
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            PeerConnection peerConnection = entry.getValue().peerConnection;
            if(peerConnection == null){
                return false;
            }
            PeerConnection.PeerConnectionState peerConnectionState = peerConnection.connectionState();
            if(entry.getValue().mode == Mode.PLAY && peerConnectionState != PeerConnection.PeerConnectionState.CONNECTED){
                return false;
            }
        }
        return true;
    }

    public void onConnected(String streamId) {
        Log.i(TAG, "Connected for streamId:" + streamId);

        if(config.reconnectionEnabled && reconnectionInProgress  && isConference() && isPublishConnected() && !isPlayConnected()){
            Log.i(TAG,"Conference reconnection. Publish connected. Play not connected. Try to reconnect play.");
            publishReconnectionHandler.removeCallbacksAndMessages(null);
            playReconnectionHandler.postDelayed(playReconnectorRunnable, PEER_RECONNECTION_DELAY_MS);
            return;
        }

        if (config.reconnectionEnabled && reconnectionInProgress && isAllPeersConnected()) {
            Log.i(TAG, "All peers reconnected. Reconnection completed successfully.");
            reconnectionInProgress = false;
            peerReconnectionHandler.removeCallbacksAndMessages(null);
            publishReconnectionHandler.removeCallbacksAndMessages(null);
            playReconnectionHandler.removeCallbacksAndMessages(null);
            this.handler.post(() -> {
                if (config.webRTCListener != null) {
                    config.webRTCListener.onReconnectionSuccess();
                }
            });
        }
        streamStoppedByUser = false;
    }

    public void onPeerConnectionClosed() {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPeerConnectionClosed();
            }
        });
    }

    public void onPeerConnectionStatsReady(RTCStatsReport report) {
        this.handler.post(() -> {
            statsCollector.onStatsReport(report);
        });
    }

    public boolean isStreaming(String streamId) {
        PeerConnection pc = getPeerConnectionFor(streamId);
        return pc != null && pc.iceConnectionState().equals(PeerConnection.IceConnectionState.CONNECTED);
    }

    @Override
    public void onTakeConfiguration(String streamId, SessionDescription sdp) {
        this.handler.post(() -> {
            if (sdp.type == SessionDescription.Type.OFFER) {
                PeerInfo peerInfo = getPeerInfoFor(streamId);
                if(peerInfo != null){
                    PeerConnection pc = peerInfo.peerConnection;
                    if (pc == null) {
                        boolean createLocalTrack = peerInfo.mode == Mode.P2P;
                        createPeerConnection(streamId, createLocalTrack);
                    }
                }

                setRemoteDescription(streamId, sdp);
                createAnswer(streamId);
            } else {
                setRemoteDescription(streamId, sdp);
            }
        });
    }

    @Override
    public void onPublishFinished(String streamId) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPublishFinished(streamId);
            }
        });
    }


    @Override
    public void onPlayFinished(String streamId) {
        waitingForPlay = false;
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPlayFinished(streamId);
            }
        });
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.d(TAG,"Publish started.");
        streamStoppedByUser = false;

        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPublishStarted(streamId);
            }
        });

    }

    @Override
    public void onPlayStarted(String streamId) {
        Log.d(TAG, "Play started.");

        streamStoppedByUser = false;
        reconnectionInProgress = false;
        waitingForPlay = false;

        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onPlayStarted(streamId);
            }
        });
    }

    @Override
    public void onStartStreaming(String streamId) {
        this.handler.post(() -> {

            createPeerConnection(streamId, true);
            PeerInfo peerInfo = getPeerInfoFor(streamId);
            if(peerInfo != null && peerInfo.mode == Mode.P2P){
                createOffer(streamId);
            }
        });
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        config.webRTCListener.onJoinedTheRoom(streamId, streams);
    }

    @Override
    public void onRoomInformation(String[] streams) {
        config.webRTCListener.onRoomInformation(streams);
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.noStreamExistsToPlay(streamId);
            }
        });
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        config.webRTCListener.onLeftTheRoom(roomId);
    }

    @Override
    public void onSessionRestored(String streamId) {
        streamStoppedByUser = false;
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onSessionRestored(streamId);
            }
        });
    }

    @Override
    public void onBroadcastObject(Broadcast broadcast) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onBroadcastObject(broadcast);
            }
        });
    }

    @Override
    public void onResolutionChange(String streamId, int resolution) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onResolutionChange(streamId, resolution);
            }
        });
    }

    @Override
    public void onJoined(String streamId) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onJoined(streamId);
            }
        });
    }

    @Override
    public void onLeft(String streamId) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onLeft(streamId);
            }
        });
    }

    @Override
    public void streamIdInUse(String streamId) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.streamIdInUse(streamId);
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(String streamId, IceCandidate candidate) {
        this.handler.post(() -> addRemoteIceCandidate(streamId, candidate));
    }

    @Override
    public void onWebSocketDisconnected() {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onWebSocketDisconnected();
            }
        });

        onDisconnected();
    }


    @Deprecated //separate websocket and ice connection
    public void onDisconnected() {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onDisconnected();
            }
        });
    }

    @Override
    public void onTrackList(String[] tracks) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onTrackList(tracks);
            }
        });

        sendPlayOtherTracks(tracks);
    }

    public void sendPlayOtherTracks(String[] tracks) {
        if (autoPlayTracks && !isStreaming(getMultiTrackStreamId()) && !waitingForPlay) {
            waitingForPlay = true;
            init();

            //don't send play for its own stream id
            for (int i = 0; i < tracks.length; i++) {
                if (tracks[i].equals(getPublishStreamId())) {
                    tracks[i] = "!" + tracks[i];
                    break;
                }
            }
            play(getMultiTrackStreamId(), tracks);
        }
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onBitrateMeasurement(streamId, targetBitrate, videoBitrate, audioBitrate);
            }
        });
    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onStreamInfoList(streamId, streamInfoList);
            }
        });
    }

    @Override
    public void onError(String streamId, String definition) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onError(definition, streamId);
            }
        });

        if (definition.equals("no_stream_exist")) {
            waitingForPlay = false;
        }
    }

    @Override
    public boolean isDataChannelEnabled() {
        return config.dataChannelEnabled;
    }

    public void getStreamInfoList(String streamId) {
        wsHandler.getStreamInfoList(streamId);
    }

    public void forceStreamQuality(String mainTrackStreamId, String subTrackStreamId, int height) {
        wsHandler.forceStreamQuality(mainTrackStreamId, subTrackStreamId, height);
    }

    class DataChannelInternalObserver implements DataChannel.Observer {

        private final DataChannel dataChannel;

        DataChannelInternalObserver(DataChannel dataChannel) {
            this.dataChannel = dataChannel;
        }

        @Override
        public void onBufferedAmountChange(long previousAmount) {
            if (config.dataChannelObserver == null) return;
            handler.post(() ->
            {
                if(dataChannel != null){
                    Log.d(TAG, "Data channel buffered amount changed: " + dataChannel.label() + ": " + dataChannel.state());
                    try{
                        config.dataChannelObserver.onBufferedAmountChange(previousAmount, dataChannel.label());

                    }catch (IllegalStateException e){
                        Log.e(TAG, "Data channel related error:" + e.getMessage());
                    }
                }

            });


        }

        @Override
        public void onStateChange() {
            handler.post(() -> {
                if (config.dataChannelObserver != null && dataChannel != null) {
                    try{
                        config.dataChannelObserver.onStateChange(dataChannel.state(), dataChannel.label());
                    }catch (IllegalStateException e){
                        Log.e(TAG, "Data channel related error:" + e.getMessage());
                    }
                }
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
                if (config.dataChannelObserver == null || dataChannel == null) return;
                try{
                    config.dataChannelObserver.onMessage(bufferCopy, dataChannel.label());
                }catch (IllegalStateException e) {
                    Log.e(TAG, "Data channel related error:" + e.getMessage());
                }
            });
        }
    }

    public void sendMessageViaDataChannel(String streamId, DataChannel.Buffer buffer) {
        if (isDataChannelEnabled()) {
            executor.execute(() -> {
                try {
                    PeerInfo peer = peers.get(streamId);
                    if (peer == null || peer.dataChannel == null) {
                        reportError(streamId, "Peer not found for sending message via Data Channel");
                        return;
                    }
                    boolean success = peer.dataChannel.send(buffer);
                    buffer.data.rewind();
                    if (config.dataChannelObserver != null) {
                        if (success) {
                            handler.post(() -> config.dataChannelObserver.onMessageSent(buffer, true));
                        } else {
                            handler.post(() -> config.dataChannelObserver.onMessageSent(buffer, false));
                            reportError(streamId, "Failed to send the message via Data Channel ");
                        }
                    }
                } catch (Exception e) {
                    reportError(streamId, "An error occurred when sending the message via Data Channel " + e.getMessage());
                    if (config.dataChannelObserver != null) {
                        buffer.data.rewind();
                        handler.post(() -> config.dataChannelObserver.onMessageSent(buffer, false));
                    }
                }
            });
        } else {
            Log.w(TAG, "Data Channel is not ready for usage for ." + streamId);
        }
    }

    public void changeVideoCapturer(VideoCapturer newVideoCapturer) {
        try {
            if (videoCapturer != null) {
                videoCapturer.stopCapture();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        videoCapturerStopped = true;
        videoCapturer = newVideoCapturer;
        localVideoTrack = null;

        MediaStreamTrack newTrack = createVideoTrack(videoCapturer);
        if (localVideoSender != null) {
            localVideoSender.setTrack(newTrack, true);
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

    public void createPeerConnection(String streamId, boolean createLocalTrack) {
        executor.execute(() -> {
            try {
                createMediaConstraintsInternal();
                createPeerConnectionInternal(streamId, createLocalTrack);
            } catch (Exception e) {
                reportError(streamId, "Failed to create peer connection: " + e.getMessage());
                throw e;
            }
        });
    }

    private void createPeerConnectionFactoryInternal(PeerConnectionFactory.Options options) {
        // Check if ISAC is used by default.
        preferIsac = config.audioCodec != null && config.audioCodec.equals(AUDIO_CODEC_ISAC);

        adm = (JavaAudioDeviceModule) createJavaAudioDevice();

        // Create peer connection factory.
        if (options != null) {
            Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
        }
        final boolean enableH264HighProfile =
                VIDEO_CODEC_H264_HIGH.equals(config.videoCodec);
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        if (config.hwCodecAcceleration) {
            encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
            decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());
        } else {
            encoderFactory = new SoftwareVideoEncoderFactory();
            decoderFactory = new SoftwareVideoDecoderFactory();
        }

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(adm)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        Log.d(TAG, "Peer connection factory created.");
        if (adm != null) {
            adm.release();
        }
    }

    public AudioDeviceModule createJavaAudioDevice() {
        // Set audio record error callbacks.
        JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback = new JavaAudioDeviceModule.AudioRecordErrorCallback() {
            @Override
            public void onWebRtcAudioRecordInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordStartError(
                    JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
            }

            @Override
            public void onWebRtcAudioRecordError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
            }
        };

        JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback = new JavaAudioDeviceModule.AudioTrackErrorCallback() {
            @Override
            public void onWebRtcAudioTrackInitError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackStartError(
                    JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
            }

            @Override
            public void onWebRtcAudioTrackError(String errorMessage) {
                Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
                reportError(getPublishStreamId(), errorMessage);
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

        JavaAudioDeviceModule.Builder admBuilder = getADMBuilder();
        return admBuilder
                .setCustomAudioFeed(config.customAudioFeed)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .setAudioRecordErrorCallback(audioRecordErrorCallback)
                .setAudioTrackErrorCallback(audioTrackErrorCallback)
                .setAudioRecordStateCallback(audioRecordStateCallback)
                .setAudioTrackStateCallback(audioTrackStateCallback)
                .createAudioDeviceModule();
    }

    public JavaAudioDeviceModule.Builder getADMBuilder() {
        return JavaAudioDeviceModule.builder(config.activity);
    }

    public void createMediaConstraintsInternal() {
        // Create audio constraints.
        audioConstraints = new MediaConstraints();
        // added for audio performance measurements
        if (config.noAudioProcessing) {
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
                "OfferToReceiveVideo", Boolean.toString(config.videoCallEnabled)));
    }

    public void createPeerConnectionInternal(String streamId, boolean createLocalTrack) {
        if (factory == null) {
            Log.e(TAG, "Peer connection factory is not created");
            return;
        }
        Log.d(TAG, "Create peer connection.");

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

        if (peerConnection != null) {

            PeerInfo peer = peers.get(streamId);
            if (peer != null) {
                peer.peerConnection = peerConnection;
            } else {
                Log.e(TAG, "Peer not found for streamId: " + streamId);
            }

            isInitiator = false;

            setWebRTCLogLevel();

            if(createLocalTrack){

                List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
                try{
                    if (config.videoCallEnabled) {
                        peerConnection.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels);
                    }
                    peerConnection.addTrack(createAudioTrack(), mediaStreamLabels);

                }catch (IllegalStateException e){
                    Log.e(TAG,"Could not add track to PC. Is it in closed state? Peer connection state " + peerConnection.connectionState().name()+" Error message: "+e.getMessage());
                    return;
                }

                if (config.videoCallEnabled) {
                    findVideoSender(streamId);
                }
            }

            config.webRTCListener.onPeerConnectionCreated(streamId);
            Log.d(TAG, "Peer connection created.");
        } else {
            Log.e(TAG, "Peer connection is not created");
        }
    }

    public void setWebRTCLogLevel() {
        // Set INFO libjingle logging.
        // NOTE: this _must_ happen while `factory` is alive!
        Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR);
    }

    @NonNull
    public PCObserver getPCObserver(String streamId) {
        return new PCObserver(streamId);
    }

    public void initDataChannel(String streamId) {
        if (config.dataChannelEnabled) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            init.negotiated = false;
            init.maxRetransmits = -1;
            init.maxRetransmitTimeMs = -1;
            init.id = 1;
            init.protocol = "";
            PeerInfo peer = peers.get(streamId);
            if (peer != null && peer.peerConnection != null) {
                DataChannel dataChannel = peer.peerConnection.createDataChannel(streamId, init);
                if (dataChannel != null) {
                    dataChannel.registerObserver(new DataChannelInternalObserver(dataChannel));
                    peer.dataChannel = dataChannel;
                }
            } else {
                Log.e(TAG, "Peer not found for streamId: " + streamId);
            }
        }
    }

    public void setDegradationPreference(RtpParameters.DegradationPreference degradationPreference) {
        if (localVideoSender == null) {
            Log.w(TAG, "Sender is not ready.");
            return;
        }
        executor.execute(() -> {
            RtpParameters newParameters = localVideoSender.getParameters();
            if (newParameters != null) {
                newParameters.degradationPreference = degradationPreference;
                localVideoSender.setParameters(newParameters);
            }
        });
    }

    public void closeInternal() {
        Log.d(TAG, "Closing resources.");
        if (statsTimer != null) {
            statsTimer.cancel();
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
        if (streamStoppedByUser) {
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
                videoCapturer.dispose();
                videoCapturer = null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            videoCapturerStopped = true;
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
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
        if(blackFrameSender != null){
            blackFrameSender.stop();
            blackFrameSender = null;
        }

        localVideoSink.setTarget(null);

        Log.d(TAG, "Closing peer connection done.");
        onPeerConnectionClosed();

        clearStatsCollector();
        reconnectionInProgress = false;
        peerReconnectionHandler.removeCallbacksAndMessages(null);
        publishReconnectionHandler.removeCallbacksAndMessages(null);
        playReconnectionHandler.removeCallbacksAndMessages(null);
    }

    private void clearStatsCollector(){
        statsCollector.getPublishStats().reset();
        statsCollector.getPlayStats().reset();
    }

    public void getStats(String streamId) {
        PeerConnection pc = getPeerConnectionFor(streamId);
        if (pc != null) {
            pc.getStats(this::onPeerConnectionStatsReady);
        }
    }

    public StatsCollector getStatsCollector() {
        return statsCollector;
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
        config.audioCallEnabled = enable;

        executor.execute(() -> {
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(enable);
            }
        });
    }

    public void setVideoEnabled(final boolean enable) {
        config.videoCallEnabled = enable;
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
        });
    }

    public void toggleSendAudio(boolean enableAudio) {
        executor.execute(() -> {
            sendAudioEnabled = enableAudio;
            if (localAudioTrack != null) {
                localAudioTrack.setEnabled(enableAudio);
            }
        });
    }

    public void toggleSendVideo(boolean enableVideo) {
        if(!config.videoCallEnabled){
            Log.i(TAG, "Cannot change send video because video call is disabled.");
            return;
        }
        executor.execute(() -> {
            sendVideoEnabled = enableVideo;
            if (enableVideo) {
                if(blackFrameSender != null && blackFrameSender.isRunning()){
                    blackFrameSender.stop();
                    blackFrameSender = null;
                }
                changeVideoSource(StreamSource.FRONT_CAMERA);
            } else {
                changeVideoSource(StreamSource.CUSTOM);
                blackFrameSender = new BlackFrameSender((CustomVideoCapturer) getVideoCapturer());
                blackFrameSender.start();
            }
        });
    }

    public void createOffer(String streamId) {

        executor.execute(() -> {
            Log.d(TAG, "Creating OFFER...");
            PeerConnection pc = getPeerConnectionFor(streamId);
            PeerInfo peerInfo = getPeerInfoFor(streamId);
            if (pc != null) {
                Log.d(TAG, "PC Create OFFER");
                isInitiator = true;
                initDataChannel(streamId);
                MediaConstraints sdpMediaConstraintsLocal;
                if (peerInfo.restartIce) {
                    Log.d(TAG, "Peer info restart ice is true. Will create offer.");
                    sdpMediaConstraintsLocal = new MediaConstraints();
                    sdpMediaConstraintsLocal.mandatory.add(
                            new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                    sdpMediaConstraintsLocal.mandatory.add(new MediaConstraints.KeyValuePair(
                            "OfferToReceiveVideo", Boolean.toString(config.videoCallEnabled)));

                    sdpMediaConstraintsLocal.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));

                } else {
                    sdpMediaConstraintsLocal = sdpMediaConstraints;
                }

                pc.createOffer(getSdpObserver(streamId), sdpMediaConstraintsLocal);
            }
        });
    }

    public void createAnswer(String streamId) {
        executor.execute(() -> {
            PeerConnection pc = getPeerConnectionFor(streamId);
            if (pc != null) {
                Log.d(TAG, "PC create ANSWER");
                isInitiator = false;
                pc.createAnswer(getSdpObserver(streamId), sdpMediaConstraints);
            }
        });
    }

    public void addRemoteIceCandidate(String streamId, final IceCandidate candidate) {
        executor.execute(() -> {
            PeerInfo peerInfo = getPeerInfoFor(streamId);
            if (peerInfo == null) {
                return;
            }
            PeerConnection pc = peerInfo.peerConnection;
            if (pc != null) {
                List<IceCandidate> queuedRemoteCandidates = peerInfo.getQueuedRemoteCandidates();
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
            PeerConnection pc = getPeerConnectionFor(streamId);
            if (pc != null) {
                // Drain the queued remote candidates if there is any so that
                // they are processed in the proper order.
                drainCandidates(streamId);
                pc.removeIceCandidates(candidates);
            }
        });
    }

    public void setRemoteDescription(String streamId, final SessionDescription desc) {
        executor.execute(() -> {
            PeerConnection pc = getPeerConnectionFor(streamId);
            if (pc == null) {
                return;
            }
            String sdp = desc.description;
            if (preferIsac) {
                sdp = preferCodec(sdp, AUDIO_CODEC_ISAC, true);
            }
            if (config.videoCallEnabled) {
                sdp = preferCodec(sdp, getSdpVideoCodecName(config.videoCodec), false);
            }
            if (config.videoStartBitrate > 0) {
                sdp = setStartBitrate(config.videoCodec, true, sdp, config.videoStartBitrate);
            }
            if (config.audioStartBitrate > 0) {
                sdp = setStartBitrate(config.audioCodec, false, sdp, config.audioStartBitrate);
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
            videoCapturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);
            videoCapturerStopped = false;
        }
    }

    public void setVideoMaxBitrate(@androidx.annotation.Nullable final Integer maxBitrateKbps) {
        if(released){
            return;
        }
        executor.execute(() -> {
            if (localVideoSender == null) {
                return;
            }
            Log.d(TAG, "Requested max video bitrate: " + maxBitrateKbps);
            if (localVideoSender == null) {
                Log.w(TAG, "Sender is not ready.");
                return;
            }

            RtpParameters parameters = localVideoSender.getParameters();
            if (parameters.encodings.isEmpty()) {
                Log.w(TAG, "RtpParameters are not ready.");
                return;
            }

            for (RtpParameters.Encoding encoding : parameters.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps = maxBitrateKbps == null ? null : maxBitrateKbps * BPS_IN_KBPS;
                encoding.minBitrateBps = maxBitrateKbps == null ? null : maxBitrateKbps * BPS_IN_KBPS / 2;
            }
            if (!localVideoSender.setParameters(parameters)) {
                Log.e(TAG, "RtpSender.setParameters failed.");
            }
            Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
        });
    }

    @androidx.annotation.Nullable
    public AudioTrack createAudioTrack() {
        if (localAudioTrack == null && factory != null) {
            audioSource = factory.createAudioSource(audioConstraints);
            localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            localAudioTrack.setEnabled(config.audioCallEnabled);
        }
        return localAudioTrack;
    }

    @androidx.annotation.Nullable
    private VideoTrack createVideoTrack(VideoCapturer capturer) {
        if (localVideoTrack == null && capturer != null && factory != null) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
            videoSource = factory.createVideoSource(capturer.isScreencast());
            capturer.initialize(surfaceTextureHelper, config.activity, videoSource.getCapturerObserver());
            capturer.startCapture(config.videoWidth, config.videoHeight, config.videoFps);

            localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            localVideoTrack.setEnabled(renderVideo);
            localVideoTrack.addSink(localVideoSink);
            videoCapturerStopped = false;
        }
        return localVideoTrack;
    }

    private void findVideoSender(String streamId) {
        PeerConnection pc = getPeerConnectionFor(streamId);

        if (pc != null) {
            for (RtpSender sender : pc.getSenders()) {
                MediaStreamTrack track = sender.track();
                if (track != null) {
                    String trackType = track.kind();
                    if (trackType.equals(VIDEO_TRACK_TYPE)) {
                        Log.d(TAG, "Found video sender.");
                        localVideoSender = sender;
                    }
                }
            }
        }
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
    private static String setStartBitrate(String codec, boolean isVideoCodec, String sdp, int bitrateKbps) {
        String[] lines = sdp.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        // Search for codec rtpmap in format
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+''?$";
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
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*''?$";
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

    /**
     * Returns the line number containing "m=audio|video", or -1 if no such line exists.
     */
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
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+''?$");
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
        PeerInfo peerInfo = getPeerInfoFor(streamId);
        if (peerInfo == null || peerInfo.getQueuedRemoteCandidates() == null) {
            return;
        }
        List<IceCandidate> queuedRemoteCandidates = peerInfo.getQueuedRemoteCandidates();
        Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
        for (IceCandidate candidate : queuedRemoteCandidates) {
            PeerConnection pc = getPeerConnectionFor(streamId);
            if (pc != null) {
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
        peerInfo.setQueuedRemoteCandidates(null);
    }

    private PeerConnection getPeerConnectionFor(String streamId) {
        PeerInfo peer = peers.get(streamId);
        if (peer != null) {
            return peer.peerConnection;
        }
        return null;
    }

    private PeerInfo getPeerInfoFor(String streamId) {
        return peers.get(streamId);
    }


    private void switchCameraInternal() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            if (!config.videoCallEnabled) {
                Log.e(TAG,
                        "Failed to switch camera. Video: " + config.videoCallEnabled);
                return; // No video is sent or only one camera is available or error happened.
            }
            Log.d(TAG, "Switch camera");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
        }
    }

    public void changeCaptureFormat(int width, int height, int framerate) {
        if (!config.videoCallEnabled || videoSource == null) {
            Log.e(TAG, "Failed to change capture format. Video: " + config.videoCallEnabled);
            return;
        }
        Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
        videoSource.adaptOutputFormat(width, height, framerate);
    }

    public void addPeerConnection(String streamId, @androidx.annotation.Nullable PeerConnection peerConnection) {
        PeerInfo peerInfo = new PeerInfo(streamId, Mode.PLAY);
        peerInfo.peerConnection = peerConnection;
        this.peers.put(streamId, peerInfo);
    }

    public List<ProxyVideoSink> getRemoteVideoSinks() {
        return remoteVideoSinks;
    }

    public void setInitiator(boolean initiator) {
        isInitiator = initiator;
    }

    public void toggleAudioOfAllParticipants(boolean enabled) {

        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            PeerConnection peerConnection = entry.getValue().peerConnection;
            if (peerConnection != null) {
                List<RtpReceiver> receivers = peerConnection.getReceivers();

                for (RtpReceiver receiver : receivers) {
                    MediaStreamTrack track = receiver.track();
                    if (track != null && track.kind().equals("audio")) {
                        AudioTrack audioTrack = (AudioTrack) track;
                        audioTrack.setEnabled(enabled);
                    }
                }
            }
        }
    }

    public boolean isLocalVideoTrackEnabled(){
        if(localVideoTrack == null){
            return false;
        }
        return localVideoTrack.enabled();
    }

    public boolean isLocalAudioTrackEnabled(){
        if(localAudioTrack == null){
            return false;
        }
        return localAudioTrack.enabled();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setAutoPlayTracks(boolean autoPlayTracks) {
        this.autoPlayTracks = autoPlayTracks;
    }

    public boolean isReconnectionInProgress() {
        return reconnectionInProgress;
    }

    public CustomWebRtcAudioRecord getAudioInput() {
        if (adm != null) {
            return adm.getAudioInput();
        }
        return null;
    }

    public VideoCapturer getVideoCapturer() {
        return videoCapturer;
    }

    public void setVideoCapturer(VideoCapturer videoCapturer){
        this.videoCapturer = videoCapturer;
    }

    public void setRemoveVideoRotationExtension(boolean removeVideoRotationExtension) {
        this.removeVideoRotationExtension = removeVideoRotationExtension;
    }

    public void setDataChannelEnabled(boolean dataChannelEnabled) {
        this.config.dataChannelEnabled = dataChannelEnabled;
    }

    public void setFactory(@androidx.annotation.Nullable PeerConnectionFactory factory) {
        this.factory = factory;
    }

    public Map<String, PeerInfo> getPeersForTest() {
        return peers;
    }

    public boolean isStreamStoppedByUser() {
        return streamStoppedByUser;
    }

    public void setStreamStoppedByUser(boolean streamStoppedByUser) {
        this.streamStoppedByUser = streamStoppedByUser;
    }

    public void setPeerReconnectionHandler(Handler peerReconnectionHandler) {
        this.peerReconnectionHandler = peerReconnectionHandler;
    }

    public ArrayList<PeerConnection.IceServer> getIceServers() {
        return iceServers;
    }
    @Override
    public boolean isShutdown() {
        return released;
    }

    @Override
    public boolean isSendVideoEnabled() {
        return sendVideoEnabled;
    }

    @Override
    public boolean isSendAudioEnabled() {
        return sendAudioEnabled;
    }

    public BlackFrameSender getBlackFrameSender() {
        return blackFrameSender;
    }

    @androidx.annotation.Nullable
    public AudioTrack getLocalAudioTrack() {
        return localAudioTrack;
    }

    public void setLocalAudioTrack(@androidx.annotation.Nullable AudioTrack localAudioTrack) {
        this.localAudioTrack = localAudioTrack;
    }

    @Override
    public void onSubscriberCount(String streamId, int count) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onSubscriberCount(streamId, count);
            }
        });
    }

    @Override
    public void onSubscriberList(String streamId, Subscriber[] subscribers) {
        this.handler.post(() -> {
            if (config.webRTCListener != null) {
                config.webRTCListener.onSubscriberList(streamId, subscribers);
            }
        });
    }

    @Override
    public void getSubscriberCount(String streamId) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.getSubscriberCount(streamId);
        }
    }

    @Override
    public void getSubscriberList(String streamId, long offset, long size) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.getSubscriberList(streamId, offset, size);
        }
    }

    @Override
    public void getDebugInfo(String streamId) {
        if (wsHandler != null && wsHandler.isConnected()) {
            wsHandler.getDebugInfo(streamId);
        }
    }
}