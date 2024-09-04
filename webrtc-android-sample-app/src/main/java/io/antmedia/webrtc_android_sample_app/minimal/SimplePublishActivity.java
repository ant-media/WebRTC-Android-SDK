package io.antmedia.webrtc_android_sample_app.minimal;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

public class SimplePublishActivity extends Activity {

    SurfaceViewRenderer fullScreenRenderer;
    IWebRTCClient webRTCClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);

        fullScreenRenderer = findViewById(R.id.full_screen_renderer);

        if(!PermissionHandler.checkCameraPermissions(this)){
            PermissionHandler.requestCameraPermissions(this);
        }else{
            createWebRTCClient();
        }
    }

    private void startPublishing(){
        webRTCClient.publish("stream1");
    }

    private void createWebRTCClient(){
         webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setInitiateBeforeStream(true)
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl("wss://test.antmedia.io:5443/LiveApp/websocket")
                .build();
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
                if(!PermissionHandler.checkPublishPermissions(SimplePublishActivity.this, false)){
                    PermissionHandler.requestPublishPermissions(SimplePublishActivity.this, false);
                }else{
                    startPublishing();
                }
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
                startPublishing();
            } else {
                Toast.makeText(this,"Publish permissions are not granted.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
