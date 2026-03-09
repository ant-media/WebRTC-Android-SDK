package io.antmedia.webrtc_android_sample_app.basic;

import static io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService.EXTRA_MEDIA_PROJECTION_DATA;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.w3c.dom.Text;
import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.api.WebRTCClientConfig;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;
import io.antmedia.webrtcandroidframework.core.StatsCollector;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScreenCaptureActivity extends TestableActivity {
    private TextView statusIndicatorTextView;
    private SurfaceViewRenderer fullScreenRenderer;
    private EditText streamIdEditText;
    private Button startStreamingButton;
    private String streamId;
    private String serverUrl;
    private boolean initBeforeStream = true;
    private boolean bluetoothEnabled = false;
    private IWebRTCClient webRTCClient;
    private RadioGroup bg;
    public static final int CAPTURE_PERMISSION_REQUEST_CODE = 1234;
    public MediaProjectionManager mediaProjectionManager;
    private  final static long UPDATE_STATS_INTERVAL_MS = 500L;

    private ScheduledFuture statCollectorFuture;
    private ScheduledExecutorService statCollectorExecutor;
    private AlertDialog statsPopup;
    private boolean publishStarted = false;
    private int lastCheckedId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshare);

        fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String generatedStreamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(generatedStreamId);

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
                    Toast.makeText(ScreenCaptureActivity.this,"Start publishing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    private void showStatsPopup(){
        LayoutInflater li = LayoutInflater.from(this);

        View promptsView = li.inflate(R.layout.stats_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(true);
        statsPopup  = alertDialogBuilder.create();

        TextView packetsLostAudio = promptsView.findViewById(R.id.stats_popup_packets_lost_audio_textview);
        TextView jitterAudio = promptsView.findViewById(R.id.stats_popup_jitter_audio_textview);
        TextView rttAudio = promptsView.findViewById(R.id.stats_popup_rtt_audio_textview);
        TextView packetLostRatioAudio = promptsView.findViewById(R.id.stats_popup_packet_lost_ratio_audio_textview);
        TextView firCountAudio = promptsView.findViewById(R.id.stats_popup_fir_count_audio_textview);
        TextView pliCountAudio = promptsView.findViewById(R.id.stats_popup_pli_count_audio_textview);
        TextView nackCountAudio = promptsView.findViewById(R.id.stats_popup_nack_count_audio_textview);
        TextView packetsSentAudio = promptsView.findViewById(R.id.stats_popup_packets_sent_audio_textview);
        TextView framesEncodedAudio = promptsView.findViewById(R.id.stats_popup_frames_encoded_audio_textview);
        TextView bytesSentAudio = promptsView.findViewById(R.id.stats_popup_bytes_sent_audio_textview);
        TextView packetsSentPerSecondAudio = promptsView.findViewById(R.id.stats_popup_packets_sent_per_second_audio_textview);
        TextView localAudioBitrate = promptsView.findViewById(R.id.stats_popup_local_audio_bitrate_textview);
        TextView localAudioLevel = promptsView.findViewById(R.id.stats_popup_local_audio_level_textview);


        TextView packetsLostVideo = promptsView.findViewById(R.id.stats_popup_packets_lost_video_textview);
        TextView jitterVideo = promptsView.findViewById(R.id.stats_popup_jitter_video_textview);
        TextView rttVideo = promptsView.findViewById(R.id.stats_popup_rtt_video_textview);
        TextView packetLostRatioVideo = promptsView.findViewById(R.id.stats_popup_packet_lost_ratio_video_textview);
        TextView firCountVideo = promptsView.findViewById(R.id.stats_popup_fir_count_video_textview);
        TextView pliCountVideo = promptsView.findViewById(R.id.stats_popup_pli_count_video_textview);
        TextView nackCountVideo = promptsView.findViewById(R.id.stats_popup_nack_count_video_textview);
        TextView packetsSentVideo = promptsView.findViewById(R.id.stats_popup_packets_sent_video_textview);
        TextView framesEncodedVideo = promptsView.findViewById(R.id.stats_popup_frames_encoded_video_textview);
        TextView bytesSentVideo = promptsView.findViewById(R.id.stats_popup_bytes_sent_video_textview);
        TextView packetsSentPerSecondVideo = promptsView.findViewById(R.id.stats_popup_packets_sent_per_second_video_textview);
        TextView localVideoBitrate = promptsView.findViewById(R.id.stats_popup_local_video_bitrate_textview);

        Button closeButton = promptsView.findViewById(R.id.stats_popup_close_button);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statsPopup.dismiss();
            }
        });

        statCollectorExecutor = Executors.newScheduledThreadPool(1);
        statCollectorFuture = statCollectorExecutor.scheduleWithFixedDelay(() -> {
            runOnUiThread(() -> {
                try{

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

                }
                catch (Exception e) {
                    Log.e("ScreenCaptureActivity", "Exception in task execution: " + e.getMessage());
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
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(initBeforeStream)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(v -> {
            streamId = streamIdEditText.getText().toString();
            startStopStream();
        });


        bg = findViewById(R.id.rbGroup);
        bg.check(R.id.rbFront);
        bg.setOnCheckedChangeListener((group, checkedId) -> {
            if(webRTCClient.isShutdown()){
                Toast.makeText(this, "Webrtc client is shutdown. Cannot switch video source.", Toast.LENGTH_SHORT).show();
                group.check(lastCheckedId);
                return;
            }
            lastCheckedId = checkedId;

            IWebRTCClient.StreamSource newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
            if(checkedId == R.id.rbScreen) {
                requestScreenCapture();
                return;
            }
            else if(checkedId == R.id.rbFront) {
                newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
            }
            else if(checkedId == R.id.rbRear) {
                newSource = IWebRTCClient.StreamSource.REAR_CAMERA;
            }
            // idlingResource.increment();
            webRTCClient.changeVideoSource(newSource);
            //    decrementIdle();
        });

    }

    public void startStopStream() {
        // incrementIdle();

        if (!PermissionHandler.checkCameraPermissions(this)) {
            PermissionHandler.requestCameraPermissions(this);
            return;
        }else if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
            PermissionHandler.requestPublishPermissions(this, bluetoothEnabled);
            return;
        }

        if (!webRTCClient.isStreaming(streamId)) {
            startStreamingButton.setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.publish(streamId);
        }
        else {
            startStreamingButton.setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(ScreenCaptureActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                publishStarted = true;
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                // decrementIdle();
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
            public void onReconnectionSuccess() {
                super.onReconnectionSuccess();
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                //   decrementIdle();
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
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.red));
                statusIndicatorTextView.setText(getResources().getString(R.string.disconnected));
                //  decrementIdle();
            }
        };
    }

    public void requestScreenCapture() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;

        WebRTCClientConfig config = webRTCClient.getConfig();
        config.mediaProjectionIntent = data;

        // If the device version is v29 or higher, screen sharing will work service due to media projection policy.
        // Otherwise media projection will work without service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            MediaProjectionService.setListener(() -> {
                startScreenCapturer();
            });

            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            serviceIntent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data);
            startForegroundService(serviceIntent);

        } else {
            startScreenCapturer();
        }
    }

    private void startScreenCapturer() {
        webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.SCREEN);
        decrementIdle();
    }
}