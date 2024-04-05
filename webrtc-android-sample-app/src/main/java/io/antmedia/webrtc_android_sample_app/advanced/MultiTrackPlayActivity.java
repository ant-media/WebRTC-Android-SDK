package io.antmedia.webrtc_android_sample_app.advanced;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class MultiTrackPlayActivity extends TestableActivity {
    private IWebRTCClient webRTCClient;
    private String[] allTracks;
    private EditText streamIdEditText;
    private String streamId;
    private Button startStreamingButton;
    private String selecetedTrack;
    private LinearLayout playersLayout;
    private LinearLayout checkboxesLayout;

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
        streamIdEditText.setText("streamId");

        webRTCClient = IWebRTCClient.builder()
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setVideoCallEnabled(false)
                //.setVideoCodec("H264")
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopStream(v);
            }
        });

        tracksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.getTrackList(streamIdEditText.getText().toString(), "");
            }
        });
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
                Toast.makeText(MultiTrackPlayActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                decrementIdle();
            }

            @Override
            public void onPlayFinished(String streamId) {
                super.onPlayFinished(streamId);
                decrementIdle();
            }

            @Override
            public void onNewVideoTrack(VideoTrack track, String streamId) {
                runOnUiThread(() -> {
                    SurfaceViewRenderer renderer = new SurfaceViewRenderer(MultiTrackPlayActivity.this);
                    renderer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    playersLayout.addView(renderer);
                    webRTCClient.setRendererForVideoTrack(renderer, track);
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
