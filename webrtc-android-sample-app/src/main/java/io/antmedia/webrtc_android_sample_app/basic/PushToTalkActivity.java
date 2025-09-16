package io.antmedia.webrtc_android_sample_app.basic;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class PushToTalkActivity extends TestableActivity {

    private String streamId;
    private String serverUrl;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private SurfaceViewRenderer remoteRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ptt);
        remoteRenderer = findViewById(R.id.play_view_renderer1);
        View talkButton = findViewById(R.id.talkButton);

        talkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    controlAV(false);
                    view.setBackgroundColor(Color.RED);
                    return true;
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    controlAV(true);
                    view.setBackgroundColor(Color.GREEN);
                    return true;
                }
                return false;
            }
        });


        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        roomId = "room1";
        serverUrl = "wss://test.antmedia.io:5443/burak/websocket";

        createWebRTCClient();
    }

    public void controlAV(boolean enable) {
        webRTCClient.toggleSendVideo(enable);
        webRTCClient.toggleSendAudio(enable);
    }

    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(remoteRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(true)
                .setWebRTCListener(createWebRTCListener(roomId, streamId))
                .build();

        webRTCClient.joinToConferenceRoom(roomId, streamId, true, true, "", "", "", "");
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                decrementIdle();
                controlAV(false);
            }
        };
    }
}
