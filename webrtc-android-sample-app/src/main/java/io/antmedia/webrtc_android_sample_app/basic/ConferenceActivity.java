package io.antmedia.webrtc_android_sample_app.basic;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.stats.TrackStatsAdapter;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.core.DataChannelConstants;
import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

public class ConferenceActivity extends TestableActivity {

    private final static long UPDATE_STATS_INTERVAL_MS = 500L;

    private TextView statusIndicatorTextView;
    private View joinButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button audioButton;
    private Button videoButton;
    private boolean playOnly;

    private AlertDialog statsPopup;
    private ScheduledFuture statCollectorFuture;
    private ScheduledExecutorService statCollectorExecutor;

    private ArrayList<TrackStats> audioTrackStatItems = new ArrayList<>();
    private ArrayList<TrackStats> videoTrackStatItems = new ArrayList<>();

    private TrackStatsAdapter audioTrackStatsAdapter;
    private TrackStatsAdapter videoTrackStatsAdapter;

    private boolean publishStarted = false;

    /*
     * We will receive videoTrack objects from the server through the onNewVideoTrack callback of the webrtc client listener.
     * These videoTracks are not yet assigned to a streamId. We store them inside videoTrackList.
     */
    private ArrayList<VideoTrack> videoTrackList = new ArrayList<>();

    /*
     * Store track assignments received through the data channel from the server.
     */
    private JSONArray trackAssignments;

    /*
     * A data channel message will arrive containing the eventType VIDEO_TRACK_ASSIGNMENT_LIST.
     * This message includes a videoLabel (trackId) and trackId (actual streamId).
     * Upon receiving this message, we will match our videoTrack objects with the streamIds and store them in the map below.
     * This allows us to determine which video track belongs to which stream id.
     */
    private HashMap<String,VideoTrack> streamIdVideoTrackMap = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        SurfaceViewRenderer publisherRenderer = findViewById(R.id.publish_view_renderer);
        SurfaceViewRenderer player1Renderer = findViewById(R.id.play_view_renderer1);
        SurfaceViewRenderer player2Renderer = findViewById(R.id.play_view_renderer2);
        SurfaceViewRenderer player3Renderer = findViewById(R.id.play_view_renderer3);
        SurfaceViewRenderer player4Renderer = findViewById(R.id.play_view_renderer4);

        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        audioButton = findViewById(R.id.control_audio_button);
        videoButton = findViewById(R.id.control_video_button);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        DefaultConferenceWebRTCListener defaultConferenceListener = createWebRTCListener(roomId, streamId);

        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            playOnly = b;
            publisherRenderer.setVisibility(b ? View.GONE : View.VISIBLE);
        });

        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(player1Renderer, player2Renderer, player3Renderer, player4Renderer)
                .setLocalVideoRenderer(publisherRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(defaultConferenceListener)
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinLeaveRoom(v);
            }
        });

        Button showStatsButton = findViewById(R.id.show_stats_button);
        showStatsButton.setOnClickListener(v -> {
            if(publishStarted){
                showStatsPopup();
            }else{
                runOnUiThread(() -> {
                    Toast.makeText(ConferenceActivity.this,"Start publishing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });

    }

    public void joinLeaveRoom(View v) {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling join");

            if(playOnly) {
                webRTCClient.joinToConferenceRoom(roomId);
            }
            else {
                webRTCClient.joinToConferenceRoom(roomId, streamId);
            }
        }
        else {
            ((Button) v).setText("Join");
            Log.i(getClass().getSimpleName(), "Calling leave");

            webRTCClient.leaveFromConference(roomId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                try{
                    JSONObject msgJsonObj = new JSONObject(messageText);
                    if(msgJsonObj.has(DataChannelConstants.EVENT_TYPE) && msgJsonObj.getString(DataChannelConstants.EVENT_TYPE).equals(DataChannelConstants.VIDEO_TRACK_ASSIGNMENT_LIST)){
                        trackAssignments = msgJsonObj.getJSONArray(DataChannelConstants.PAYLOAD);
                        matchStreamIdAndVideoTrack();
                    }
                }catch (Exception e){
                    Log.e(getClass().getSimpleName(),"Cant parse data channel message to JSON object. "+e.getMessage());
                }

            }
        };
    }

    private void matchStreamIdAndVideoTrack(){
        try{
            for(int i=0;i<trackAssignments.length();i++){
                // inside this object videoLabel is actually trackId(it is like videoTrack0) and trackId is our actual streamId
                JSONObject videoLabelTrackIdObj = trackAssignments.getJSONObject(i);
                for(int k=0;k<videoTrackList.size();k++){
                    VideoTrack videoTrack = videoTrackList.get(k);
                    String videoTrackId = videoTrack.id().substring(DataChannelConstants.TRACK_ID_PREFIX.length());
                    if(videoLabelTrackIdObj.getString(DataChannelConstants.VIDEO_LABEL).equals(videoTrackId)){
                        streamIdVideoTrackMap.put(videoLabelTrackIdObj.getString(DataChannelConstants.TRACK_ID), videoTrack);
                    }
                }
            }

        }catch (JSONException e){
            Log.e(getClass().getSimpleName(), "Cant parse JSON on matchStreamIdAndVideoTrack method. "+ e.getMessage());
        }
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                decrementIdle();
                publishStarted = true;
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
            public void onNewVideoTrack(VideoTrack track, String trackId) {
                super.onNewVideoTrack(track, trackId);
                videoTrackList.add(track);
                if(trackAssignments != null){
                    matchStreamIdAndVideoTrack();
                }
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                decrementIdle();
            }
        };
    }

    public void controlAudio(View view) {
        if (webRTCClient.getConfig().audioCallEnabled) {
            webRTCClient.setAudioEnabled(false);
            audioButton.setText("Enable Audio");
        } else {
            webRTCClient.setAudioEnabled(true);
            audioButton.setText("Disable Audio");
        }
    }

    public void controlVideo(View view) {
        if (webRTCClient.getConfig().videoCallEnabled) {
            webRTCClient.setVideoEnabled(false);
            videoButton.setText("Enable Video");
        } else {
            webRTCClient.setVideoEnabled(true);
            videoButton.setText("Disable Video");
        }
    }

    private void showStatsPopup(){
        LayoutInflater li = LayoutInflater.from(this);

        View promptsView = li.inflate(R.layout.multitrack_stats_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(true);
        statsPopup  = alertDialogBuilder.create();


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


                    PlayStats playStats = webRTCClient.getStatsCollector().getPlayStats();
                    audioTrackStatItems.clear();
                    audioTrackStatItems.addAll(playStats.getAudioTrackStatsMap().values());

                    videoTrackStatItems.clear();
                    videoTrackStatItems.addAll(playStats.getVideoTrackStatsMap().values());

                    audioTrackStatsAdapter.notifyDataSetChanged();
                    videoTrackStatsAdapter.notifyDataSetChanged();

                }
                catch (Exception e) {
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
}
