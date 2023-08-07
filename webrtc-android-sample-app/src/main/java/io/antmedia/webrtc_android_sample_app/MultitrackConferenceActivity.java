package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;

import io.antmedia.webrtcandroidframework.MultitrackConferenceManager;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class MultitrackConferenceActivity extends AbstractSampleSDKActivity {

    private MultitrackConferenceManager conferenceManager;
    private Button audioButton;
    private Button videoButton;

    final int RECONNECTION_PERIOD_MLS = 1000;
    private boolean stoppedStream = false;
    private TextView broadcastingView;
    private ArrayList<SurfaceViewRenderer> playViewRenderers;
    private int rendererIndex = 0;
    private Switch playOnlySwitch;

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

        setContentView(R.layout.activity_multitrack_conference);

        broadcastingView = findViewById(R.id.broadcasting_text_view);

        SurfaceViewRenderer publishViewRenderer = findViewById(R.id.publish_view_renderer);

        playViewRenderers = new ArrayList<>();

        playViewRenderers.add(findViewById(R.id.play_view_renderer1));
        playViewRenderers.add(findViewById(R.id.play_view_renderer2));
        playViewRenderers.add(findViewById(R.id.play_view_renderer3));
        playViewRenderers.add(findViewById(R.id.play_view_renderer4));

        playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            conferenceManager.setPlayOnlyMode(b);
            publishViewRenderer.setVisibility(b ? View.GONE : View.VISIBLE);
        });

        audioButton = findViewById(R.id.control_audio_button);
        audioButton.setOnClickListener((view) -> controlAudio(view));

        videoButton = findViewById(R.id.control_video_button);
        videoButton.setOnClickListener((view) -> controlVideo(view));

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        //  this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_CALL, false);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        String roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        String streamId = null;

        conferenceManager = new MultitrackConferenceManager(
                this,
                this,
                getIntent(),
                serverUrl,
                roomId,
                publishViewRenderer,
                playViewRenderers, //new ArrayList<>(),//
                streamId,
                this
        );

        conferenceManager.init();
        conferenceManager.setPlayOnlyMode(false);
        conferenceManager.setOpenFrontCamera(true);
        conferenceManager.setReconnectionEnabled(true);
    }

    public void joinConference(View v) {

        if (!conferenceManager.isJoined()) {
            Log.w(getClass().getSimpleName(), "Joining Conference");
            ((Button) v).setText("Leave");
            conferenceManager.joinTheConference();
        } else {
            ((Button) v).setText("Join");
            conferenceManager.leaveFromConference();
            stoppedStream = true;
        }
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();

        broadcastingView.setText("Publishing");
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
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
        stoppedStream = true;
    }

    @Override
    public void onDisconnected(String streamId) {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
    }

    @Override
    public void onNewVideoTrack(VideoTrack track) {
        if(false && !track.id().contains(conferenceManager.getStreamId())) {
            SurfaceViewRenderer renderer = playViewRenderers.get(rendererIndex++%4);
            conferenceManager.addTrackToRenderer(track, renderer);
        }
    }

    public void controlAudio(View view) {
        if (conferenceManager.isPublisherAudioOn()) {
            conferenceManager.disableAudio();
            audioButton.setText("Enable Audio");
        } else {
            conferenceManager.enableAudio();
            audioButton.setText("Disable Audio");
        }
    }

    public void controlVideo(View view) {
        if (conferenceManager.isPublisherVideoOn()) {
            conferenceManager.disableVideo();
            videoButton.setText("Enable Video");

        } else {
            conferenceManager.enableVideo();
            videoButton.setText("Disable Video");
        }
    }

    public void changeWifiState(boolean state) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        conferenceManager.onMessage(buffer, dataChannelLabel);
    }
}

