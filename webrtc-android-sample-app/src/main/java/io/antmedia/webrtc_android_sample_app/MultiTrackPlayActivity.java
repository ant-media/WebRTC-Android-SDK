package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.ArrayList;
import java.util.List;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.apprtc.CallFragment;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

public class MultiTrackPlayActivity extends Activity implements IWebRTCListener {


    public static final String SERVER_URL = "ws://172.16.110.228:5080/WebRTCAppEE/websocket";
    private CallFragment callFragment;

    private WebRTCClient webRTCClient;
    private String webRTCMode;
    private Button startStreamingButton;
    private String operationName = "";
    private String streamId;
    private String tokenId;
    private ToggleButton track1Button;
    private ToggleButton track2Button;
    private String[] allTracks;

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

        setContentView(R.layout.activity_multitrack);


        webRTCClient = new WebRTCClient( this,this);

        //webRTCClient.setOpenFrontCamera(false);


        //String streamId = "stream" + (int)(Math.random() * 999);
        streamId = "stream_multi_track";
        tokenId = "tokenId";

        SurfaceViewRenderer cameraViewRenderer = findViewById(R.id.player1);

        SurfaceViewRenderer pipViewRenderer = findViewById(R.id.player2);

        List<SurfaceViewRenderer> rendererList = new ArrayList<>();
        rendererList.add(cameraViewRenderer);
        rendererList.add(pipViewRenderer);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        
        track1Button = findViewById(R.id.track_1_button);
        track2Button = findViewById(R.id.track_2_button);

        track1Button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                webRTCClient.enableTrack(streamId, allTracks[0], isChecked);
            }
        });

        track2Button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                webRTCClient.enableTrack(streamId, allTracks[1], isChecked);
            }
        });

        //webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        webRTCClient.setRemoteRendererList(rendererList);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);

        webRTCMode = IWebRTCClient.MODE_MULTI_TRACK_PLAY;

       // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(SERVER_URL, streamId, webRTCMode, tokenId, this.getIntent());

    }


    public void startStreaming(View v) {

        if (!webRTCClient.isStreaming()) {
            ((Button)v).setText("Stop " + operationName);
            webRTCClient.startStream();
        }
        else {
            ((Button)v).setText("Start " + operationName);
            webRTCClient.stopStream();
        }
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
        finish();
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
        webRTCClient.stopStream();

    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected(String streamId) {

        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();

        finish();
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
    }

    @Override
    public void onIceDisconnected(String streamId) {

    }

    public void onOffVideo(View view) {
        if (webRTCClient.isVideoOn()) {
            webRTCClient.disableVideo();
        }
        else {
            webRTCClient.enableVideo();
        }
    }

    public void onOffAudio(View view) {
        if (webRTCClient.isAudioOn()) {
            webRTCClient.disableAudio();
        }
        else {
            webRTCClient.enableAudio();
        }
    }

    @Override
    public void onTrackList(String[] tracks) {
        allTracks = new String[tracks.length+1];

        allTracks[0] = streamId;
        for (int i = 0; i < tracks.length; i++) {
            allTracks[i+1] = tracks[i];
            Log.i(getClass().getSimpleName(), "track id: " + tracks[i]);
        }

        webRTCClient.play(streamId, tokenId, allTracks);
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {

    }
}
