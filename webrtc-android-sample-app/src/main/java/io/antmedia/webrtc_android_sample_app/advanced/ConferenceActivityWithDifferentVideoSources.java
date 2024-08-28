package io.antmedia.webrtc_android_sample_app.advanced;


import static io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService.EXTRA_MEDIA_PROJECTION_DATA;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtc_android_sample_app.basic.stats.TrackStatsAdapter;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.api.WebRTCClientConfig;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;
import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

/*
 * This sample demonstrates how to switch different stream sources on call or off call.
 * Users can switch in between rear camera, front camera, screen share while in
 * conference or before joining the conference.
 * Critical point of such implementation is always asking for screen share permission if source is
 * selected as SCREEN before publishing. If webrtc client is released(shutdown) and user tries to change video source,
 * be sure to re-create webrtc client.
 */

public class ConferenceActivityWithDifferentVideoSources extends TestableActivity {
    public static final int SCREEN_CAPTURE_PERMISSION_CODE = 1234;
    private final static long UPDATE_STATS_INTERVAL_MS = 500L;

    private TextView statusIndicatorTextView;
    private Button joinButton;
    private String streamId;
    private String serverUrl;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button rearCameraButton;
    private Button frontCameraButton;
    private Button screenShareButton;
    boolean bluetoothEnabled = false;
    boolean initBeforeStream = false;
    boolean joinWithScreenShareRequested = false;
    boolean publishStarted = false;


    private SurfaceViewRenderer localParticipantRenderer;
    private SurfaceViewRenderer remoteParticipant1Renderer;
    private SurfaceViewRenderer remoteParticipant2Renderer;
    private SurfaceViewRenderer remoteParticipant3Renderer;
    private SurfaceViewRenderer remoteParticipant4Renderer;

    private MediaProjectionManager mediaProjectionManager;

    private AlertDialog statsPopup;
    private ScheduledFuture statCollectorFuture;
    private ScheduledExecutorService statCollectorExecutor;

    private ArrayList<TrackStats> audioTrackStatItems = new ArrayList<>();
    private ArrayList<TrackStats> videoTrackStatItems = new ArrayList<>();

    private TrackStatsAdapter audioTrackStatsAdapter;
    private TrackStatsAdapter videoTrackStatsAdapter;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_with_different_video_sources);

        localParticipantRenderer = findViewById(R.id.local_participant_renderer);
        remoteParticipant1Renderer = findViewById(R.id.remote_participant_1_renderer);
        remoteParticipant2Renderer = findViewById(R.id.remote_participant_2_renderer);
        remoteParticipant3Renderer = findViewById(R.id.remote_participant_3_renderer);
        remoteParticipant4Renderer = findViewById(R.id.remote_participant_4_renderer);

        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        rearCameraButton = findViewById(R.id.rear_camera_button);
        frontCameraButton = findViewById(R.id.front_camera_button);
        screenShareButton = findViewById(R.id.screen_share_button);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        if(initBeforeStream){
            if(PermissionHandler.checkCameraPermissions(this)){
                createWebRTCClient();
            }else{
                PermissionHandler.requestCameraPermissions(this);
            }
        }else{
            createWebRTCClient();
        }

        Button showStatsButton = findViewById(R.id.show_stats_button);
        showStatsButton.setOnClickListener(v -> {

            if(publishStarted){
                showStatsPopup();
            }else{
                runOnUiThread(() -> {
                    Toast.makeText(ConferenceActivityWithDifferentVideoSources.this,"Start publishing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    private void showStatsPopup() {
        LayoutInflater li = LayoutInflater.from(this);

        View promptsView = li.inflate(R.layout.multitrack_stats_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(true);
        statsPopup = alertDialogBuilder.create();


        TextView packetsLostAudio = promptsView.findViewById(R.id.multitrack_stats_popup_packets_lost_audio_textview);
        TextView jitterAudio = promptsView.findViewById(R.id.multitrack_stats_popup_jitter_audio_textview);
        TextView rttAudio = promptsView.findViewById(R.id.multitrack_stats_popup_rtt_audio_textview);
        TextView packetLostRatioAudio = promptsView.findViewById(R.id.multitrack_stats_popup_packet_lost_ratio_audio_textview);
        TextView firCountAudio = promptsView.findViewById(R.id.multitrack_stats_popup_fir_count_audio_textview);
        TextView pliCountAudio = promptsView.findViewById(R.id.multitrack_stats_popup_pli_count_audio_textview);
        TextView nackCountAudio = promptsView.findViewById(R.id.multitrack_stats_popup_nack_count_audio_textview);
        TextView packetsSentAudio = promptsView.findViewById(R.id.multitrack_stats_popup_packets_sent_audio_textview);
        TextView framesEncodedAudio = promptsView.findViewById(R.id.multitrack_stats_popup_frames_encoded_audio_textview);
        TextView bytesSentAudio = promptsView.findViewById(R.id.multitrack_stats_popup_bytes_sent_audio_textview);
        TextView packetsSentPerSecondAudio = promptsView.findViewById(R.id.multitrack_stats_popup_packets_sent_per_second_audio_textview);
        TextView localAudioBitrate = promptsView.findViewById(R.id.multitrack_stats_popup_local_audio_bitrate_textview);
        TextView localAudioLevel = promptsView.findViewById(R.id.multitrack_stats_popup_local_audio_level_textview);

        TextView packetsLostVideo = promptsView.findViewById(R.id.multitrack_stats_popup_packets_lost_video_textview);
        TextView jitterVideo = promptsView.findViewById(R.id.multitrack_stats_popup_jitter_video_textview);
        TextView rttVideo = promptsView.findViewById(R.id.multitrack_stats_popup_rtt_video_textview);
        TextView packetLostRatioVideo = promptsView.findViewById(R.id.multitrack_stats_popup_packet_lost_ratio_video_textview);
        TextView firCountVideo = promptsView.findViewById(R.id.multitrack_stats_popup_fir_count_video_textview);
        TextView pliCountVideo = promptsView.findViewById(R.id.multitrack_stats_popup_pli_count_video_textview);
        TextView nackCountVideo = promptsView.findViewById(R.id.multitrack_stats_popup_nack_count_video_textview);
        TextView packetsSentVideo = promptsView.findViewById(R.id.multitrack_stats_popup_packets_sent_video_textview);
        TextView framesEncodedVideo = promptsView.findViewById(R.id.multitrack_stats_popup_frames_encoded_video_textview);
        TextView bytesSentVideo = promptsView.findViewById(R.id.multitrack_stats_popup_bytes_sent_video_textview);
        TextView packetsSentPerSecondVideo = promptsView.findViewById(R.id.multitrack_stats_popup_packets_sent_per_second_video_textview);
        TextView localVideoBitrate = promptsView.findViewById(R.id.multitrack_stats_popup_local_video_bitrate_textview);

        audioTrackStatsAdapter = new TrackStatsAdapter(audioTrackStatItems, this);
        videoTrackStatsAdapter = new TrackStatsAdapter(videoTrackStatItems, this);

        RecyclerView playStatsAudioTrackRecyclerview = promptsView.findViewById(R.id.multitrack_stats_popup_play_stats_audio_track_recyclerview);
        RecyclerView playStatsVideoTrackRecyclerview = promptsView.findViewById(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview);

        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);

        playStatsAudioTrackRecyclerview.setLayoutManager(linearLayoutManager1);
        playStatsVideoTrackRecyclerview.setLayoutManager(linearLayoutManager2);


        playStatsAudioTrackRecyclerview.setAdapter(audioTrackStatsAdapter);
        playStatsVideoTrackRecyclerview.setAdapter(videoTrackStatsAdapter);

        Button closeButton = promptsView.findViewById(R.id.multitrack_stats_popup_close_button);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statsPopup.dismiss();
            }
        });

        statCollectorExecutor = Executors.newScheduledThreadPool(1);
        statCollectorFuture = statCollectorExecutor.scheduleWithFixedDelay(() -> {
            runOnUiThread(() -> {
                try {
                    TrackStats audioTrackStats = webRTCClient.getStatsCollector().getPublishStats().getAudioTrackStats();
                    packetsLostAudio.setText(String.valueOf(audioTrackStats.getPacketsLost()));
                    jitterAudio.setText(String.valueOf(audioTrackStats.getJitter()));
                    rttAudio.setText(String.valueOf(audioTrackStats.getRoundTripTime()));
                    packetLostRatioAudio.setText(String.valueOf(audioTrackStats.getPacketLostRatio()));
                    firCountAudio.setText(String.valueOf(audioTrackStats.getFirCount()));
                    pliCountAudio.setText(String.valueOf(audioTrackStats.getPliCount()));
                    nackCountAudio.setText(String.valueOf(audioTrackStats.getNackCount()));
                    packetsSentAudio.setText(String.valueOf(audioTrackStats.getPacketsSent()));
                    framesEncodedAudio.setText(String.valueOf(audioTrackStats.getFramesEncoded()));
                    bytesSentAudio.setText(String.valueOf(audioTrackStats.getBytesSent()));
                    packetsSentPerSecondAudio.setText(String.valueOf(audioTrackStats.getPacketsSentPerSecond()));
                    packetsSentAudio.setText(String.valueOf(audioTrackStats.getPacketsSent()));

                    TrackStats videoTrackStats = webRTCClient.getStatsCollector().getPublishStats().getVideoTrackStats();
                    packetsLostVideo.setText(String.valueOf(videoTrackStats.getPacketsLost()));
                    jitterVideo.setText(String.valueOf(videoTrackStats.getJitter()));
                    rttVideo.setText(String.valueOf(videoTrackStats.getRoundTripTime()));
                    packetLostRatioVideo.setText(String.valueOf(videoTrackStats.getPacketLostRatio()));
                    firCountVideo.setText(String.valueOf(videoTrackStats.getFirCount()));
                    pliCountVideo.setText(String.valueOf(videoTrackStats.getPliCount()));
                    nackCountVideo.setText(String.valueOf(videoTrackStats.getNackCount()));
                    packetsSentVideo.setText(String.valueOf(videoTrackStats.getPacketsSent()));
                    framesEncodedVideo.setText(String.valueOf(videoTrackStats.getFramesEncoded()));
                    bytesSentVideo.setText(String.valueOf(videoTrackStats.getBytesSent()));
                    packetsSentPerSecondVideo.setText(String.valueOf(videoTrackStats.getPacketsSentPerSecond()));
                    packetsSentVideo.setText(String.valueOf(videoTrackStats.getPacketsSent()));

                    localAudioBitrate.setText(String.valueOf(webRTCClient.getStatsCollector().getPublishStats().getAudioBitrate()));
                    localAudioLevel.setText(String.valueOf(webRTCClient.getStatsCollector().getPublishStats().getLocalAudioLevel()));
                    localVideoBitrate.setText(String.valueOf(webRTCClient.getStatsCollector().getPublishStats().getVideoBitrate()));


                    PlayStats playStats = webRTCClient.getStatsCollector().getPlayStats();
                    audioTrackStatItems.clear();
                    audioTrackStatItems.addAll(playStats.getAudioTrackStatsMap().values());

                    videoTrackStatItems.clear();
                    videoTrackStatItems.addAll(playStats.getVideoTrackStatsMap().values());

                    audioTrackStatsAdapter.notifyDataSetChanged();
                    videoTrackStatsAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    Log.e("ConferenceActivity", "Exception in task execution: " + e.getMessage());
                }
            });

        }, 0, UPDATE_STATS_INTERVAL_MS, TimeUnit.MILLISECONDS);


        statsPopup.setOnDismissListener(dialog -> {
            if (statCollectorFuture != null && !statCollectorFuture.isCancelled()) {
                statCollectorFuture.cancel(true);
            }
            if (statCollectorExecutor != null && !statCollectorExecutor.isShutdown()) {
                statCollectorExecutor.shutdown();
            }
        });

        statsPopup.show();
    }


    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(remoteParticipant1Renderer, remoteParticipant2Renderer, remoteParticipant3Renderer, remoteParticipant4Renderer)
                .setLocalVideoRenderer(localParticipantRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(initBeforeStream)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener(roomId, streamId))
                .setDataChannelObserver(createDatachannelObserver())
                .build();


        joinButton = findViewById(R.id.join_conference_button);
        joinButton.setOnClickListener(v -> {
                joinLeaveRoom();
        });

        rearCameraButton.setOnClickListener(v -> {
            changeVideoSource(IWebRTCClient.StreamSource.REAR_CAMERA);

        });

        frontCameraButton.setOnClickListener(v -> {
            changeVideoSource(IWebRTCClient.StreamSource.FRONT_CAMERA);
        });

        screenShareButton.setOnClickListener(v -> {
            if(webRTCClient.isStreaming(streamId)){
                requestScreenCapture();
            }else{
                webRTCClient.getConfig().videoSource = IWebRTCClient.StreamSource.SCREEN;
            }
        });

    }

    public void changeVideoSource(IWebRTCClient.StreamSource streamSource){
        if(webRTCClient.getConfig().videoSource != streamSource){

            if(!webRTCClient.isShutdown()){
                webRTCClient.changeVideoSource(streamSource);
            }else{
                createWebRTCClient();
                webRTCClient.changeVideoSource(streamSource);
            }
        }
    }

    public void requestScreenCapture() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_PERMISSION_CODE);
    }

    public void joinLeaveRoom() {
        incrementIdle();

        if(!initBeforeStream) {
            if (!PermissionHandler.checkCameraPermissions(this)) {
                PermissionHandler.requestCameraPermissions(this);
                return;
            }else if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
                PermissionHandler.requestPublishPermissions(this, bluetoothEnabled);
                return;
            }
        }

        if (!webRTCClient.isStreaming(streamId)) {
            joinButton.setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling join");

            if(webRTCClient.getConfig().videoSource == IWebRTCClient.StreamSource.SCREEN){
                joinWithScreenShareRequested = true;
                requestScreenCapture();
            }else{
                webRTCClient.joinToConferenceRoom(roomId, streamId);
            }
        }
        else {
            joinButton.setText("Join");
            Log.i(getClass().getSimpleName(), "Calling leave");

            webRTCClient.leaveFromConference(roomId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
            }
        };
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                publishStarted = true;

                decrementIdle();
            }

            @Override
            public void onReconnectionSuccess() {
                super.onReconnectionSuccess();
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
            }

            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                if(webRTCClient.isReconnectionInProgress()){
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
                    statusIndicatorTextView.setText(getResources().getString(R.string.reconnecting));
                }else{
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
                    statusIndicatorTextView.setText(getResources().getString(R.string.disconnected));
                }
            }

            @Override
            public void onPublishAttempt(String streamId) {
                super.onPublishAttempt(streamId);
                if(webRTCClient.isReconnectionInProgress()){
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
                    statusIndicatorTextView.setText(getResources().getString(R.string.reconnecting));
                }else{
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
                    statusIndicatorTextView.setText(getResources().getString(R.string.connecting));
                }
            }

            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                decrementIdle();
            }

            @Override
            public void onShutdown() {
                super.onShutdown();
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                decrementIdle();
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SCREEN_CAPTURE_PERMISSION_CODE)
            return;

        WebRTCClientConfig config = webRTCClient.getConfig();
        config.mediaProjectionIntent = data;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            MediaProjectionService.setListener(() -> {
                startScreenCapturer();
                if(joinWithScreenShareRequested){
                    webRTCClient.joinToConferenceRoom(roomId, streamId);
                    joinWithScreenShareRequested = false;
                }
            });

            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            serviceIntent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data);
            startForegroundService(serviceIntent);

        } else {
            startScreenCapturer();
            if(joinWithScreenShareRequested){
                webRTCClient.joinToConferenceRoom(roomId, streamId);
                joinWithScreenShareRequested = false;
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PermissionHandler.CAMERA_PERMISSION_REQUEST_CODE){
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (initBeforeStream && allPermissionsGranted) {
                createWebRTCClient();
            }else if(!initBeforeStream && allPermissionsGranted){
                joinLeaveRoom();
            }
            else {
                Toast.makeText(this,"Camera permissions are not granted. Cannot initialize.", Toast.LENGTH_LONG).show();
            }


        }else if(requestCode == PermissionHandler.PUBLISH_PERMISSION_REQUEST_CODE){

            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                joinLeaveRoom();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startScreenCapturer() {
        changeVideoSource(IWebRTCClient.StreamSource.SCREEN);
        decrementIdle();
    }
}

