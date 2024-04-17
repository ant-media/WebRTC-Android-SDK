package io.antmedia.webrtc_android_sample_app.minimal;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class SimplePublishActivity extends Activity {

    IWebRTCClient webRTCClient;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);
        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);

        webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl("wss://test.antmedia.io:5443/LiveApp/websocket")
                .build();

    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
                Toast.makeText(SimplePublishActivity.this,"Websocket connected", Toast.LENGTH_SHORT).show();
                String streamId = "streamId" + (int)(Math.random()*9999);
                webRTCClient.publish(streamId);
            }
        };
    }
}
