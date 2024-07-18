package io.antmedia.webrtc_android_sample_app.advanced;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtc_android_sample_app.basic.stats.TrackStatsAdapter;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.DataChannelConstants;
import io.antmedia.webrtcandroidframework.core.ProxyVideoSink;
import io.antmedia.webrtcandroidframework.core.model.PlayStats;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

/*
 * This sample demonstrates how to play multiple streams using a single WebRTC client.
 * In Ant Media Server, a broadcast object includes an array field called 'subtracks'.
 * By calling .play() with the streamId of a broadcast(maintrack), where 'subtracks' contains the streamIds of other streams,
 * it will play those subtrack streams.
 * For more information:
 * https://antmedia.io/docs/guides/publish-live-stream/multitrack-publish-and-play-with-ams/#terminologies-related-to-multitrack
 */

public class MultiTrackPlayActivity extends TestableActivity {
    private final static long UPDATE_STATS_INTERVAL_MS = 500L;

    private IWebRTCClient webRTCClient;
    private EditText streamIdEditText;
    private String streamId;
    private Button startStreamingButton;
    private LinearLayout playersLayout;
    private LinearLayout checkboxesLayout;
    private Handler handler = new Handler();

    /*
     * We will receive videoTrack objects from the server through the onNewVideoTrack callback of the webrtc client listener.
     * These videoTracks are not yet assigned to a streamId. We store them inside videoTrackList.
     * A runnable will read those video tracks and assign them to surface view renderers.
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
    private HashMap<String, VideoTrack> streamIdVideoTrackMap = new HashMap<>();

    private HashMap<SurfaceViewRenderer, VideoTrack> surfaceViewRendererVideoTrackMap = new HashMap<>();

    private HashMap<String, SurfaceViewRenderer> streamIdSurfaceViewRendererMap = new HashMap<>();

    private Runnable videoTrackSurfaceViewRendererMatcherRunnable;

    private boolean playStarted = false;

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
        setContentView(R.layout.activity_multitrack);

        playersLayout = findViewById(R.id.players);
        checkboxesLayout = findViewById(R.id.checkboxes);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        Button tracksButton = findViewById(R.id.tracks_button);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        streamIdEditText.setText("mainTrack");

        webRTCClient = IWebRTCClient.builder()
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setVideoCallEnabled(false)
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(v -> startStopStream(v));

        tracksButton.setOnClickListener(v -> {
            /*
             * If you require, you can play certain subtracks with different resolutions.
             * Don't forget to add your desired resolution as ABR on server side application settings.
             * Play subtrack with 240p resolution:
             * webRTCClient.forceStreamQuality("mainTrack", "subTrack1", 240);
             * Play a stream with 720p resolution:
             * webRTCClient.forceStreamQuality("someStreamId", "", 720);
             *
             */
            webRTCClient.getTrackList(streamIdEditText.getText().toString(), "");
        });

        Button showStatsButton = findViewById(R.id.show_stats_button);

        showStatsButton.setOnClickListener(v -> {
            if(playStarted){
                showStatsPopup();
            }else{
                runOnUiThread(() -> {
                    Toast.makeText(MultiTrackPlayActivity.this,"Start playing first.", Toast.LENGTH_SHORT).show();
                });
            }
        });

        //Handle adding new video tracks as new surface view renderers.
        //Handle setting new video tracks to existing surface view renderers. (reconnection case)
        videoTrackSurfaceViewRendererMatcherRunnable = () -> {
            handler.postDelayed(videoTrackSurfaceViewRendererMatcherRunnable, 500);
            runOnUiThread(() -> {
                for(VideoTrack videoTrack: videoTrackList){
                    String streamId = getStreamIdByVideoTrack(videoTrack);
                    if(streamId == null){
                        return;
                    }
                    SurfaceViewRenderer surfaceViewRenderer = getSurfaceViewRendererByStreamId(streamId);
                    if(surfaceViewRenderer == null){
                        SurfaceViewRenderer renderer = new SurfaceViewRenderer(getApplicationContext());
                        renderer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        playersLayout.addView(renderer);
                        webRTCClient.getConfig().remoteVideoRenderers.add(renderer);
                        renderer.setTag(videoTrack);
                        webRTCClient.setRendererForVideoTrack(renderer, videoTrack);
                        surfaceViewRendererVideoTrackMap.put(renderer, videoTrack);
                        matchStreamIdAndSurfaceViewRenderer();
                    }else{
                        //In case of a reconnection we need to set video tracks to our old surface view renderers.
                        ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
                        remoteVideoSink.setTarget(surfaceViewRenderer);
                        videoTrack.addSink(remoteVideoSink);
                    }
                }
            });
        };
    }

    private void showStatsPopup(){
        LayoutInflater li = LayoutInflater.from(this);

        View promptsView = li.inflate(R.layout.multitrack_stats_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(promptsView);

        alertDialogBuilder.setCancelable(true);
        statsPopup  = alertDialogBuilder.create();

        audioTrackStatsAdapter = new TrackStatsAdapter(audioTrackStatItems, this);
        videoTrackStatsAdapter = new TrackStatsAdapter(videoTrackStatItems, this);

        RelativeLayout publishStatsContainer = promptsView.findViewById(R.id.multitrack_stats_popup_publish_stats_main_container);
        //this is only playing so hide publish stats
        publishStatsContainer.setVisibility(View.GONE);

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
                    PlayStats playStats = webRTCClient.getStatsCollector().getPlayStats();
                    audioTrackStatItems.clear();
                    audioTrackStatItems.addAll(playStats.getAudioTrackStatsMap().values());

                    videoTrackStatItems.clear();
                    videoTrackStatItems.addAll(playStats.getVideoTrackStatsMap().values());

                    audioTrackStatsAdapter.notifyDataSetChanged();
                    videoTrackStatsAdapter.notifyDataSetChanged();

                }
                catch (Exception e) {
                    Log.e("MultiTrackPlayActivity", "Exception in task execution: " + e.getMessage());
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

    public void startStopStream(View v) {
        incrementIdle();
        streamId = streamIdEditText.getText().toString();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling play start");
            webRTCClient.play(streamId);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling play start");
            webRTCClient.stop(streamId);
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
                        matchStreamIdAndSurfaceViewRenderer();
                    }
                }catch (Exception e){
                    Log.e(getClass().getSimpleName(),"Cant parse data channel message to JSON object. "+e.getMessage());
                }
            }
        };
    }

    private void matchStreamIdAndVideoTrack() {
        try{
            for(int i=0;i<trackAssignments.length();i++){
                // inside this object videoLabel is actually trackId(ex:videoTrack0) and trackId is our actual streamId
                JSONObject videoLabelTrackIdObj = trackAssignments.getJSONObject(i);
                for(int k=0;k<videoTrackList.size();k++){
                    VideoTrack videoTrack = videoTrackList.get(k);
                    String videoTrackId = videoTrack.id().substring(DataChannelConstants.TRACK_ID_PREFIX.length());
                    if(videoLabelTrackIdObj.getString(DataChannelConstants.VIDEO_LABEL).equals(videoTrackId)){
                        String subtrackStreamId = videoLabelTrackIdObj.getString(DataChannelConstants.TRACK_ID);
                        streamIdVideoTrackMap.put(subtrackStreamId, videoTrack);
                    }
                }
            }
        }catch (JSONException e){
            Log.e(getClass().getSimpleName(), "Cant parse JSON on matchStreamIdAndVideoTrack method. "+ e.getMessage());
        }
    }

    private String getStreamIdByVideoTrack(VideoTrack videoTrack) {
        for (Map.Entry<String, VideoTrack> entry : streamIdVideoTrackMap.entrySet()) {
            if (entry.getValue().equals(videoTrack)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void matchStreamIdAndSurfaceViewRenderer() {
        for (Map.Entry<String, VideoTrack> entry1 : streamIdVideoTrackMap.entrySet()) {
            String streamId = entry1.getKey();
            VideoTrack videoTrack1 = entry1.getValue();
            for (Map.Entry<SurfaceViewRenderer, VideoTrack> entry2 : surfaceViewRendererVideoTrackMap.entrySet()) {
                SurfaceViewRenderer renderer = entry2.getKey();
                VideoTrack videoTrack2 = entry2.getValue();
                if(videoTrack1.equals(videoTrack2)){
                    streamIdSurfaceViewRendererMap.put(streamId, renderer);
                }
            }
        }
    }

    private SurfaceViewRenderer getSurfaceViewRendererByStreamId(String streamId) {
        for (Map.Entry<String, SurfaceViewRenderer> entry : streamIdSurfaceViewRendererMap.entrySet()) {
            if (entry.getKey().equals(streamId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                decrementIdle();
                playStarted = true;
                handler.postDelayed(videoTrackSurfaceViewRendererMatcherRunnable, 0);
            }

            @Override
            public void onPlayFinished(String streamId) {
                super.onPlayFinished(streamId);
                decrementIdle();
            }

            @Override
            public void onResolutionChange(String streamId, int resolution) {
                super.onResolutionChange(streamId, resolution);
                Toast.makeText(MultiTrackPlayActivity.this, "Resolution changed to "+ resolution + " for stream "+ streamId,Toast.LENGTH_LONG).show();
            }

            @Override
            public void onShutdown() {
                super.onShutdown();
                videoTrackList.clear();
                streamIdVideoTrackMap.clear();
                streamIdSurfaceViewRendererMap.clear();
                surfaceViewRendererVideoTrackMap.clear();
                playersLayout.removeAllViews();
                handler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                videoTrackList.clear();
            }

            @Override
            public void onNewVideoTrack(VideoTrack videoTrack, String trackId) {
                videoTrackList.add(videoTrack);
                if(trackAssignments != null){
                    matchStreamIdAndVideoTrack();
                }
            }

            @Override
            public void onTrackList(String[] tracks) {
                for (String track : tracks) {
                    CheckBox checkBox = new CheckBox(MultiTrackPlayActivity.this);
                    checkBox.setText(track);
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                            webRTCClient.enableTrack(streamId, track, isChecked));
                    checkboxesLayout.addView(checkBox);
                }
            }
        };
    }
}