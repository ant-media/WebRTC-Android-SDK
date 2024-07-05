package io.antmedia.webrtc_android_sample_app.basic;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class ConferenceActivity extends TestableActivity {
    private TextView statusIndicatorTextView;
    private View joinButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button audioButton;
    private Button videoButton;
    private boolean playOnly;


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
                //Toast.makeText(ConferenceActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
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


    /**
     * This method is used to change the state of the wifi for testing purposes
     * @param state
     */
    public void changeWifiState(boolean state) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }
}
