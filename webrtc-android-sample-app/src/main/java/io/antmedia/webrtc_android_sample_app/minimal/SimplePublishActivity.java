package io.antmedia.webrtc_android_sample_app.minimal;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.PermissionHandler;
import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class SimplePublishActivity extends Activity {

    IWebRTCClient webRTCClient;
    SurfaceViewRenderer fullScreenRenderer;
    boolean bluetoothEnabled = false;
    String streamId = "streamId" + (int)(Math.random()*9999);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);
        fullScreenRenderer = findViewById(R.id.full_screen_renderer);

        if(PermissionHandler.checkCameraPermissions(this)){
            createWebRTCClient();
        }
    }

    public void createWebRTCClient(){
        webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setBluetoothEnabled(bluetoothEnabled)
                .setWebRTCListener(createWebRTCListener())
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl("wss://test.antmedia.io:5443/LiveApp/websocket")
                .build();

        if(PermissionHandler.checkPublishPermissions(this, bluetoothEnabled)){
            webRTCClient.publish(streamId);
        }
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
            }
        };
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
                webRTCClient.publish(streamId);
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webRTCClient != null){
            webRTCClient.stopReconnector();
        }
    }
}
