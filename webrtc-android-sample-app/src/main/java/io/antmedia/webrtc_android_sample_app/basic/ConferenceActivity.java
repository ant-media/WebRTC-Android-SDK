package io.antmedia.webrtc_android_sample_app.basic;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class ConferenceActivity extends TestableActivity {
    private TextView broadcastingView;
    private View joinButton;
    private View streamInfoListSpinner;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private String roomId;

    private Button audioButton;
    private Button videoButton;




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

        broadcastingView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        audioButton = findViewById(R.id.control_audio_button);
        videoButton = findViewById(R.id.control_video_button);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        DefaultConferenceWebRTCListener defaultConferenceListener = createWebRTCListener(roomId, streamId);

        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            defaultConferenceListener.setPlayOnly(b);
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
    }

    public void joinLeaveRoom(View v) {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling join");

            webRTCClient.joinToConferenceRoom(roomId, streamId);
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
                Toast.makeText(ConferenceActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId);
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
}
