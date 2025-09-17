package io.antmedia.webrtc_android_sample_app.basic;

import android.app.AlertDialog;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

public class PublishActivity extends TestableActivity {

    private TextView statusIndicatorTextView;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private Button startStreamingButton;
    private String serverUrl;
    private TextView streamIdEditText;
    private SurfaceViewRenderer fullScreenRenderer;

    private boolean bluetoothEnabled = false;
    private boolean initBeforeStream = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        statusIndicatorTextView = findViewById(R.id.broadcasting_text_view);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String generatedStreamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(generatedStreamId);

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
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(initBeforeStream)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setAudioCallEnabled(false)
                .build();

        startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(v -> {
            streamId = streamIdEditText.getText().toString();
            startStopStream();
        });

    }

    public void startStopStream() {
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
            startStreamingButton.setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");
            webRTCClient.publish(streamId);
        }
        else {
            startStreamingButton.setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish stop");

            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(PublishActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
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
                decrementIdle();
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
            builder.setPositiveButton("OK", (dialog, which) -> {
                // send data from the AlertDialog to the Activity
                EditText editText = customLayout.findViewById(R.id.message_text_input);
                sendTextMessage(editText.getText().toString());
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
                startStopStream();
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
                startStopStream();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public IWebRTCClient getWebRTCClient() {
        return webRTCClient;
    }
}
