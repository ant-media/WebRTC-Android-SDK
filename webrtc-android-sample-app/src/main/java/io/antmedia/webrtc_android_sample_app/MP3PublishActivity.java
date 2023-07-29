package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.MP3Publisher;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class MP3PublishActivity extends AbstractSampleSDKActivity {

    private static final int DESIRED_SAMPLE_RATE = 48000;
    /**
     * Mode can Publish, Play or P2P
     */
    private String webRTCMode = IWebRTCClient.MODE_PUBLISH;

    private boolean enableDataChannel = true;


    private WebRTCClient webRTCClient;

    private Button startStreamingButton;
    private String operationName = "";
    private String serverUrl;

    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;
    private Spinner streamInfoListSpinner;
    public static final String WEBRTC_MODE = "WebRTC_MODE";
    private TextView broadcastingView;
    private EditText streamIdEditText;
    private boolean audioPushingEnabled = false;
    private String TAG = MP3PublishActivity.class.getSimpleName();
    private MP3Publisher mp3Publisher;

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

        cameraViewRenderer = findViewById(R.id.camera_view_renderer);
        pipViewRenderer = findViewById(R.id.pip_view_renderer);

        broadcastingView = findViewById(R.id.broadcasting_text_view);

        streamIdEditText = findViewById(R.id.stream_id_edittext);
        streamIdEditText.setText("streamId" + (int)(Math.random()*9999));

        startStreamingButton = findViewById(R.id.start_streaming_button);

        streamInfoListSpinner = findViewById(R.id.stream_info_list);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String mode = this.getIntent().getStringExtra(WEBRTC_MODE);
        if (mode != null) {
            webRTCMode = mode;
        }

        startStreamingButton.setText("Start Publishing");
        operationName = "Publishing";

        this.getIntent().putExtra(EXTRA_VIDEO_FPS, 30);
        this.getIntent().putExtra(EXTRA_VIDEO_BITRATE, 1500);
        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel);

        webRTCClient = new WebRTCClient( this,this);

        //webRTCClient.setOpenFrontCamera(false);

        String tokenId = "tokenId";
        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/sample_44100_stereo.mp3";
        mp3Publisher = new MP3Publisher(webRTCClient, this, path);

       // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(serverUrl, streamIdEditText.getText().toString(), webRTCMode, tokenId, this.getIntent());
        webRTCClient.setDataChannelObserver(this);


    }

    public void startStreaming(View v) {
        //update stream id if it is changed
        webRTCClient.setStreamId(streamIdEditText.getText().toString());
        idlingResource.increment();
        if (!webRTCClient.isStreaming()) {
            ((Button) v).setText("Stop " + operationName);
            Log.i(getClass().getSimpleName(), "Calling startStream");

            mp3Publisher.startStreaming();
            webRTCClient.startStream();

        }
        else {
            ((Button)v).setText("Start " + operationName);
            Log.i(getClass().getSimpleName(), "Calling stopStream");
            webRTCClient.stopStream();
            mp3Publisher.stopStreaming();
        }

    }


    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.GONE);

    }


    @Override
    protected void onStop() {
        super.onStop();
        if (webRTCClient != null) {
            Log.i(getClass().getSimpleName(), "onStop and calling stopStream");
            webRTCClient.stopStream();
        }
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
        startStreamingButton.setText("Stop " + operationName);
    }
}
