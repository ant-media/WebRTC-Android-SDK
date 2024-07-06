package io.antmedia.webrtc_android_sample_app.basic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.StatsCollector;

public class StatsActivity extends TestableActivity {
    private TextView statusIndicatorTextView;
    private String streamId;

    private  final static long UPDATE_STATS_INTERVAL_MS = 500L;

    private IWebRTCClient webRTCClient;

    private ScheduledFuture statCollectorFuture;
    private ScheduledExecutorService statCollectorExecutor;

    private AlertDialog statsPopup;

    private boolean publishStarted = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        String generatedStreamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(generatedStreamId);

        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamId = streamIdEditText.getText().toString();
                startStopStream(v);
            }
        });
        Button showStatsButton = findViewById(R.id.show_stats_button);
        showStatsButton.setOnClickListener(v -> {
            if(publishStarted){
                showStatsPopup();

            }else{
                runOnUiThread(() -> {
                    Toast.makeText(StatsActivity.this,"Start publishing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });


    }

    public void startStopStream(View v) {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");
            webRTCClient.publish(streamId);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish stop");

            webRTCClient.stop(streamId);
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

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                publishStarted = true;
                decrementIdle();
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
                decrementIdle();
            }
        };
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
        webRTCClient.sendMessageViaDataChannel(streamId, buf);
    }

    public void showSendDataChannelMessageDialog(View view) {
        if (webRTCClient != null && webRTCClient.isDataChannelEnabled()) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send Message via Data Channel");
            // set the custom layout
            final View customLayout = getLayoutInflater().inflate(R.layout.send_message_data_channel, null);
            builder.setView(customLayout);
            // add a button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText editText = customLayout.findViewById(R.id.message_text_input);
                    sendTextMessage(editText.getText().toString());
                }
            });
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, R.string.data_channel_not_available, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statCollectorFuture != null && !statCollectorFuture.isCancelled()) {
            statCollectorFuture.cancel(true);
        }
        if (statCollectorExecutor != null && !statCollectorExecutor.isShutdown()) {
            statCollectorExecutor.shutdown();
        }
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
                        for (Map.Entry<Long, StatsCollector.TrackStats> entry : webRTCClient.getStatsCollector().getAudioTrackStatsMap().entrySet()) {
                            StatsCollector.TrackStats value = entry.getValue();
                            packetsLostAudio.setText(String.valueOf(value.getPacketsLost()));
                            jitterAudio.setText(String.valueOf(value.getJitter()));
                            rttAudio.setText(String.valueOf(value.getRoundTripTime()));
                            packetLostRatioAudio.setText(String.valueOf(value.getPacketLostRatio()));
                            firCountAudio.setText(String.valueOf(value.getFirCount()));
                            pliCountAudio.setText(String.valueOf(value.getPliCount()));
                            nackCountAudio.setText(String.valueOf(value.getNackCount()));
                            packetsSentAudio.setText(String.valueOf(value.getPacketsSent()));
                            framesEncodedAudio.setText(String.valueOf(value.getFramesEncoded()));
                            bytesSentAudio.setText(String.valueOf(value.getBytesSent()));
                            packetsSentPerSecondAudio.setText(String.valueOf(value.getPacketsSentPerSecond()));
                            packetsSentAudio.setText(String.valueOf(value.getPacketsSent()));

                        }

                        for (Map.Entry<Long, StatsCollector.TrackStats> entry : webRTCClient.getStatsCollector().getVideoTrackStatsMap().entrySet()) {
                            StatsCollector.TrackStats value = entry.getValue();
                            packetsLostVideo.setText(String.valueOf(value.getPacketsLost()));
                            jitterVideo.setText(String.valueOf(value.getJitter()));
                            rttVideo.setText(String.valueOf(value.getRoundTripTime()));
                            packetLostRatioVideo.setText(String.valueOf(value.getPacketLostRatio()));
                            firCountVideo.setText(String.valueOf(value.getFirCount()));
                            pliCountVideo.setText(String.valueOf(value.getPliCount()));
                            nackCountVideo.setText(String.valueOf(value.getNackCount()));
                            packetsSentVideo.setText(String.valueOf(value.getPacketsSent()));
                            framesEncodedVideo.setText(String.valueOf(value.getFramesEncoded()));
                            bytesSentVideo.setText(String.valueOf(value.getBytesSent()));
                            packetsSentPerSecondVideo.setText(String.valueOf(value.getPacketsSentPerSecond()));
                            packetsSentVideo.setText(String.valueOf(value.getPacketsSent()));

                        }

                        localAudioBitrate.setText(String.valueOf(webRTCClient.getStatsCollector().getLocalAudioBitrate()));
                        localAudioLevel.setText(String.valueOf(webRTCClient.getStatsCollector().getLocalAudioLevel()));

                        localVideoBitrate.setText(String.valueOf(webRTCClient.getStatsCollector().getLocalVideoBitrate()));

                    }
                    catch (Exception e) {
                        Log.e("StatsActivity", "Exception in task execution: " + e.getMessage());
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

    public IWebRTCClient getWebRTCClient() {
        return webRTCClient;
    }

}
