package io.antmedia.webrtc_android_sample_app.basic;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.PermissionHandler;
import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class ConferenceActivity extends TestableActivity {
    private TextView broadcastingView;
    private Button joinButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button audioButton;
    private Button videoButton;
    private boolean playOnly;

    String serverUrl = "";

    SurfaceViewRenderer publisherRenderer;
    SurfaceViewRenderer player1Renderer;
    SurfaceViewRenderer player2Renderer;
    SurfaceViewRenderer player3Renderer;
    SurfaceViewRenderer player4Renderer;

    boolean bluetoothEnabled = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        publisherRenderer = findViewById(R.id.publish_view_renderer);
        player1Renderer = findViewById(R.id.play_view_renderer1);
        player2Renderer = findViewById(R.id.play_view_renderer2);
        player3Renderer = findViewById(R.id.play_view_renderer3);
        player4Renderer = findViewById(R.id.play_view_renderer4);

        broadcastingView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        audioButton = findViewById(R.id.control_audio_button);
        videoButton = findViewById(R.id.control_video_button);

        serverUrl = "wss://fed3805de679.ngrok.app/LiveApp/websocket";

        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        if(PermissionHandler.checkCameraPermissions(this)){

            createWebRTCClient();

        }

        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            playOnly = b;
            publisherRenderer.setVisibility(b ? View.GONE : View.VISIBLE);
        });


        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinLeaveRoom();
            }
        });
    }

    public void createWebRTCClient(){
        DefaultConferenceWebRTCListener defaultConferenceListener = createWebRTCListener(roomId, streamId);

        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(player1Renderer, player2Renderer, player3Renderer, player4Renderer)
                .setLocalVideoRenderer(publisherRenderer)
                .setServerUrl(serverUrl)
                .setBluetoothEnabled(bluetoothEnabled)
                .setActivity(this)
                .setWebRTCListener(defaultConferenceListener)
                .setDataChannelObserver(createDatachannelObserver())
                .build();
    }

    public void joinLeaveRoom() {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            Log.i(getClass().getSimpleName(), "Calling join");

            if(PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){

                if(playOnly) {
                    webRTCClient.joinToConferenceRoom(roomId);
                }
                else {
                    webRTCClient.joinToConferenceRoom(roomId, streamId);
                }
            }
        }
        else {
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
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
            }

            @Override
            public void onDisconnected() {
                super.onDisconnected();
                if(webRTCClient.getConfig().reconnectionEnabled){
                    broadcastingView.setText("Reconnecting...");

                }else{
                    broadcastingView.setText("Disconnected");
                }
            }

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                joinButton.setText("Leave");
                broadcastingView.setText("Connected");

                broadcastingView.setVisibility(View.VISIBLE);
                decrementIdle();
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                joinButton.setText("Join");

                broadcastingView.setVisibility(View.GONE);
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
            if (allPermissionsGranted) {
                createWebRTCClient();
            } else {
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
                joinLeaveRoom();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webRTCClient != null){
            webRTCClient.destroy();
        }
    }

}
