package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.apprtc.CallFragment;
import io.antmedia.webrtcandroidframework.apprtc.UnhandledExceptionHandler;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_HEIGHT;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_WIDTH;

public class MainActivity extends Activity implements IWebRTCListener {


    public static final String SERVER_URL = "ws://10.2.40.89:5080/WebRTCAppEE/websocket";
    private CallFragment callFragment;

    private WebRTCClient webRTCClient;

    @RequiresApi(api = Build.VERSION_CODES.M)
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

        setContentView(R.layout.activity_main);


        webRTCClient = new WebRTCClient( this,this);

        //webRTCClient.setOpenFrontCamera(false);


        //String streamId = "stream" + (int)(Math.random() * 999);
        String streamId = "stream1";
        String tokenId = "tokenId";

        SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.camera_view_renderer);

        SurfaceViewRenderer pipViewRenderer = findViewById(R.id.pip_view_renderer);


        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);

       // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(SERVER_URL, streamId, IWebRTCClient.MODE_PUBLISH, tokenId);

        webRTCClient.startStream();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startRecording(View v) {

        if (!webRTCClient.isRecording()) {
            ((Button)v).setText("Stop Recording");
            webRTCClient.startRecording(null, 800000, 64000);
        }
        else {
            webRTCClient.stopRecording();
            ((Button)v).setText("Start Recording");
        }

    }

    @Override
    public void onPlayStarted() {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        webRTCClient.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    @Override
    public void onPublishStarted() {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onPublishFinished() {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_LONG).show();

        webRTCClient.stopStream();



        SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.camera_view_renderer);

        SurfaceViewRenderer pipViewRenderer = findViewById(R.id.pip_view_renderer);


        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);


        webRTCClient.init(SERVER_URL, "stream1", IWebRTCClient.MODE_PUBLISH, "tokenId");

        webRTCClient.startStream();
    }

    @Override
    public void onPlayFinished() {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void noStreamExistsToPlay() {
        Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        Toast.makeText(this, "No stream exist to play", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onError(String description) {
        Toast.makeText(this, "Error: "  +description , Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        webRTCClient.stopStream();
        if (webRTCClient.isRecording()) {
            webRTCClient.stopRecording();
        }
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected() {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();

        finish();
    }

    @Override
    public void onConnected() {
        //it is called when connected to ice
    }
}
