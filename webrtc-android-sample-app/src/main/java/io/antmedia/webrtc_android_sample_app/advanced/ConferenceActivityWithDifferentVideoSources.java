package io.antmedia.webrtc_android_sample_app.advanced;


import static io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService.EXTRA_MEDIA_PROJECTION_DATA;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.WebRTCClientConfig;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

public class ConferenceActivityWithDifferentVideoSources extends TestableActivity {
    public static final int SCREEN_CAPTURE_PERMISSION_CODE = 1234;

    private TextView statusIndicatorTextView;
    private Button joinButton;
    private String streamId;
    private String serverUrl;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button rearCameraButton;
    private Button frontCameraButton;
    private Button screenShareButton;
    boolean bluetoothEnabled = false;
    boolean initBeforeStream = false;

    private SurfaceViewRenderer localParticipantRenderer;
    private SurfaceViewRenderer remoteParticipant1Renderer;
    private SurfaceViewRenderer remoteParticipant2Renderer;
    private SurfaceViewRenderer remoteParticipant3Renderer;
    private SurfaceViewRenderer remoteParticipant4Renderer;

    private MediaProjectionManager mediaProjectionManager;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference_with_different_video_sources);

        localParticipantRenderer = findViewById(R.id.local_participant_renderer);
        remoteParticipant1Renderer = findViewById(R.id.remote_participant_1_renderer);
        remoteParticipant2Renderer = findViewById(R.id.remote_participant_2_renderer);
        remoteParticipant3Renderer = findViewById(R.id.remote_participant_3_renderer);
        remoteParticipant4Renderer = findViewById(R.id.remote_participant_4_renderer);

        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        rearCameraButton = findViewById(R.id.rear_camera_button);
        frontCameraButton = findViewById(R.id.front_camera_button);
        screenShareButton = findViewById(R.id.screen_share_button);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        serverUrl = "wss://ovh36.antmedia.io:5443/LiveApp/websocket";
        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);



        if(initBeforeStream){
            if(PermissionHandler.checkCameraPermissions(this)){
                createWebRTCClient();
            }else{
                PermissionHandler.requestCameraPermissions(this);
            }
        }else{
            createWebRTCClient();
        }

    }

    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(remoteParticipant1Renderer, remoteParticipant2Renderer, remoteParticipant3Renderer, remoteParticipant4Renderer)
                .setLocalVideoRenderer(localParticipantRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(initBeforeStream)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener(roomId, streamId))
                .setDataChannelObserver(createDatachannelObserver())
                .build();






        joinButton = findViewById(R.id.join_conference_button);
        joinButton.setOnClickListener(v -> {
            joinLeaveRoom();
        });

        rearCameraButton.setOnClickListener(v -> {
            //webRTCClient.switchCamera(); // this works
           webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.REAR_CAMERA);
        });

        frontCameraButton.setOnClickListener(v -> {
            webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.FRONT_CAMERA);
        });

        screenShareButton.setOnClickListener(v -> {
            requestScreenCapture();
        });

    }

    public void requestScreenCapture() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), SCREEN_CAPTURE_PERMISSION_CODE);
    }

    public void joinLeaveRoom() {
        incrementIdle();

        if(!initBeforeStream) {
            if (!PermissionHandler.checkCameraPermissions(this)) {
                PermissionHandler.requestCameraPermissions(this);
                return;
            }else if(!PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
                PermissionHandler.requestPublishPermissions(this, bluetoothEnabled);
                return;
            }
        }

        if (!webRTCClient.isStreaming(streamId)) {
            joinButton.setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling join");

            webRTCClient.joinToConferenceRoom(roomId, streamId);

        }
        else {
            joinButton.setText("Join");
            Log.i(getClass().getSimpleName(), "Calling leave");

            webRTCClient.leaveFromConference(roomId);
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
               /* super.onPlayStarted(streamId);
                statusIndicatorTextView.setTextColor(getResources().getColor(R.color.green));
                statusIndicatorTextView.setText(getResources().getString(R.string.live));
                decrementIdle();*/

            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                decrementIdle();
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SCREEN_CAPTURE_PERMISSION_CODE)
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
            // MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startScreenCapturer();
        }

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
                joinLeaveRoom();
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
                joinLeaveRoom();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startScreenCapturer() {
        webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.SCREEN);
        decrementIdle();
    }
}

