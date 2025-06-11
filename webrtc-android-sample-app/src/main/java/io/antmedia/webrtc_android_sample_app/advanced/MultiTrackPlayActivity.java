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
import java.util.Map;
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
    private final static long UPDATE_SURFACE_VIEWS_INTERVAL_MS = 500L;


    private IWebRTCClient webRTCClient;
    private EditText streamIdEditText;
    private String streamId;
    private Button startStreamingButton;
    private LinearLayout playersLayout;
    private LinearLayout checkboxesLayout;
    private Handler handler = new Handler();
    private boolean playStarted;

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
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                decrementIdle();
                playStarted = true;
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
            public void onReconnectionSuccess() {
                super.onReconnectionSuccess();
                Toast.makeText(MultiTrackPlayActivity.this,"Reconnected.",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onShutdown() {
                super.onShutdown();
                playersLayout.removeAllViews();
                handler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                Toast.makeText(MultiTrackPlayActivity.this,"Disconnected.",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNewVideoTrack(VideoTrack videoTrack, String trackId) {
                runOnUiThread(() -> {
                    SurfaceViewRenderer renderer = new SurfaceViewRenderer(getApplicationContext());
                    renderer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    playersLayout.addView(renderer);
                    webRTCClient.getConfig().remoteVideoRenderers.add(renderer);
                    renderer.setTag(videoTrack);
                    webRTCClient.setRendererForVideoTrack(renderer, videoTrack);
                });
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