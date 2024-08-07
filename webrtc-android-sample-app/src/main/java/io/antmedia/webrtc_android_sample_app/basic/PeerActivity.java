package io.antmedia.webrtc_android_sample_app.basic;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.stats.TrackStatsAdapter;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;
import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

/*
 * This sample demonstrates how to make peer to peer call.
 * During P2P, ant media server acts only as a signalling server.
 * Video/Audio data does not flow through ant media server.
 * For more information:
 * https://antmedia.io/docs/guides/publish-live-stream/webrtc/webrtc-peer-to-peer-communication/
 */

public class PeerActivity extends TestableActivity {
    private  final static long UPDATE_STATS_INTERVAL_MS = 500L;


    private TextView statusIndicatorTextView;
    private Button startStreamingButton;
    private String roomName;
    private IWebRTCClient webRTCClient;
    private EditText roomNameEditText;
    private SurfaceViewRenderer fullScreenRenderer;
    private SurfaceViewRenderer pipRenderer;
    private String serverUrl;
    boolean initBeforeStream = true;
    boolean bluetoothEnabled = false;
    boolean joinedP2PRoom = false;

    private ScheduledFuture statCollectorFuture;
    private ScheduledExecutorService statCollectorExecutor;

    private AlertDialog statsPopup;

    private ArrayList<TrackStats> audioTrackStatItems = new ArrayList<>();
    private ArrayList<TrackStats> videoTrackStatItems = new ArrayList<>();

    private TrackStatsAdapter audioTrackStatsAdapter;
    private TrackStatsAdapter videoTrackStatsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer);

        fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        pipRenderer = findViewById(R.id.pip_view_renderer);

        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        startStreamingButton = findViewById(R.id.start_streaming_button);
        roomNameEditText = findViewById(R.id.stream_id_edittext);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        roomNameEditText.setText(PEER_ROOM_ID_FOR_TEST);

        if(initBeforeStream) {
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
            if(joinedP2PRoom){
                showStatsPopup();
            }else{
                runOnUiThread(() -> {
                    Toast.makeText(PeerActivity.this,"Start publishing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(pipRenderer)
                .setInitiateBeforeStream(initBeforeStream)
                .addRemoteVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setBluetoothEnabled(bluetoothEnabled)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(v -> {
            roomName = roomNameEditText.getText().toString();
            startStopStream();
        });
    }

    public void startStopStream() {
        incrementIdle();

        if(!initBeforeStream) {
            if (!PermissionHandler.checkCameraPermissions(this)) {
                PermissionHandler.requestCameraPermissions(this);
                return;
            }else if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
                PermissionHandler.requestPublishPermissions(this, bluetoothEnabled);
                return;
            }
        }else{
            if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
                PermissionHandler.requestPublishPermissions(this, bluetoothEnabled);
                return;
            }
        }

        if (!joinedP2PRoom) {
            startStreamingButton.setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling publish start");
            webRTCClient.join(roomName);
        }
        else {
            startStreamingButton.setText("Join");
            Log.i(getClass().getSimpleName(), "Calling publish stop");
            webRTCClient.stop(roomName);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(PeerActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onJoined(String streamId) {
                super.onJoined(streamId);
                joinedP2PRoom = true;
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                decrementIdle();
            }

            @Override
            public void onJoinAttempt(String streamId) {
                super.onJoinAttempt(streamId);
                if(webRTCClient.isReconnectionInProgress()){
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
                    statusIndicatorTextView.setText(getResources().getString(R.string.reconnecting));
                }else{
                    statusIndicatorTextView.setTextColor(getResources().getColor(R.color.blue));
                    statusIndicatorTextView.setText(getResources().getString(R.string.connecting));
                }
            }

            @Override
            public void onReconnectionSuccess() {
                super.onReconnectionSuccess();
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                decrementIdle();
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
            public void onLeft(String streamId) {
                super.onLeft(streamId);
                joinedP2PRoom = false;
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
                statusIndicatorTextView.setText(getResources().getString(R.string.disconnected));
                decrementIdle();
            }
        };
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
        webRTCClient.sendMessageViaDataChannel(roomName, buf);
    }

    public void showSendDataChannelMessageDialog(View view) {
        if (webRTCClient != null && webRTCClient.isDataChannelEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send Message via Data Channel");
            final View customLayout = getLayoutInflater().inflate(R.layout.send_message_data_channel, null);
            builder.setView(customLayout);
            builder.setPositiveButton("OK", (dialog, which) -> {
                EditText editText = customLayout.findViewById(R.id.message_text_input);
                sendTextMessage(editText.getText().toString());
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, R.string.data_channel_not_available, Toast.LENGTH_LONG).show();
        }
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
                startStopStream();
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
                startStopStream();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
