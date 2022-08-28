package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class ScreenCaptureActivity extends Activity implements IWebRTCListener {

    private WebRTCClient webRTCClient;
    private RadioGroup bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_screenshare);


        webRTCClient = new WebRTCClient(this, this);

        //webRTCClient.setOpenFrontCamera(false);

        SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.camera_view_renderer);

        SurfaceViewRenderer pipViewRenderer = findViewById(R.id.pip_view_renderer);


        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS)
        {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        //this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_BITRATE, 1000);
        this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_WIDTH, 360);
        this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, 640);
        //this.getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, true);
        this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        //webRTCClient.setCameraOrientationFix(90);

        bg = findViewById(R.id.rbGroup);
        bg.check(R.id.rbFront);
        bg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String newSource = "";
                if(checkedId == R.id.rbScreen) {
                    newSource = WebRTCClient.SOURCE_SCREEN;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, true);
                }
                else if(checkedId == R.id.rbFront) {
                    newSource = WebRTCClient.SOURCE_FRONT;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
                }
                else if(checkedId == R.id.rbRear) {
                    newSource = WebRTCClient.SOURCE_REAR;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
                }
                webRTCClient.changeVideoSource(newSource);
            }
        });

        String streamId = "stream36";
        String tokenId = "tokenId";
        String url = "ws://192.168.1.28:5080/WebRTCAppEE/websocket";

        webRTCClient.init(url, streamId, IWebRTCClient.MODE_PUBLISH, tokenId,  this.getIntent());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // If the device version is v29 or higher, screen sharing will work service due to media projection policy.
        // Otherwise media projection will work without service
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MediaProjectionService service = new MediaProjectionService();

            service.setListener(mediaProjection -> {
                webRTCClient.setMediaProjection(mediaProjection);
                webRTCClient.onActivityResult(requestCode, resultCode, data);
            });

            service.start(getApplicationContext(), data);
        }
        else{
            webRTCClient.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void startStreaming(View v) {

        if (!webRTCClient.isStreaming()) {
            ((Button)v).setText("Stop Streaming");
            webRTCClient.startStream();
        }
        else {
            ((Button)v).setText("Start Streaming");
            webRTCClient.stopStream();
        }
    }

    public void switchCamera(View v) {
        webRTCClient.switchCamera();
    }

    @Override
    public void onPlayStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        webRTCClient.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlayFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        Toast.makeText(this, "No stream exist to play", Toast.LENGTH_LONG).show();
    }

    @Override
    public void streamIdInUse(String streamId) {
        Log.w(getClass().getSimpleName(), "streamIdInUse");
        Toast.makeText(this, "Stream id is already in use.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String description, String streamId) {
        Toast.makeText(this, "Error: "  +description , Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webRTCClient.stopStream();

        //webRTCClient.releaseResources();
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected(String streamId) {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
    }

    @Override
    public void onIceDisconnected(String streamId) {

    }

    @Override
    public void onTrackList(String[] tracks) {

    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {

    }


}

