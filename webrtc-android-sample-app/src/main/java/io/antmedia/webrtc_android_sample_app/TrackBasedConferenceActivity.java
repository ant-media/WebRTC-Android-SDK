package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Random;

import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class TrackBasedConferenceActivity extends AbstractSampleSDKActivity {

    private WebRTCClient webRTCClient;
    private Button audioButton;
    private Button videoButton;
    private boolean stoppedStream = false;
    private TextView broadcastingView;
    private ArrayList<SurfaceViewRenderer> playViewRenderers;
    private int rendererIndex = 0;
    private boolean playOnlyMode = false;
    private boolean joined = false;
    private String roomId;
    private String streamId;

    private Handler handler = new Handler();

    private int ROOM_INFO_POLLING_MILLIS = 5000;
    private Runnable getRoomInfoRunnable = new Runnable() {
        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };
    private String token;
    private String subscriberId;
    private String subscriberCode;
    private boolean videoCallEnabled = true;
    private boolean audioCallEnabled = true;
    private String streamName;
    private boolean playMessageSent;
    private String viewerInfo;

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

        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            //conferenceManager.setPlayOnlyMode(b);
            playOnlyMode = b;
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

        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = streamId == null ? "stream"+new Random().nextInt(99999) : streamId;


        webRTCClient = new WebRTCClient(this, this);
        webRTCClient.setMainTrackId(roomId);
        webRTCClient.setSelfStreamId(streamId);
        webRTCClient.setVideoRenderers(null, publishViewRenderer);
        webRTCClient.setRemoteRendererList(playViewRenderers);


        webRTCClient.init(serverUrl, streamId, WebRTCClient.MODE_TRACK_BASED_CONFERENCE, "", this.getIntent());
        webRTCClient.connectWebSocket();
        webRTCClient.setDataChannelObserver(this);
        webRTCClient.setOpenFrontCamera(true);
        webRTCClient.setReconnectionEnabled(true);
    }

    private void scheduleGetRoomInfo() {
        handler.postDelayed(getRoomInfoRunnable, ROOM_INFO_POLLING_MILLIS);
    }

    private void clearGetRoomInfoSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(getRoomInfoRunnable)) {
                handler.removeCallbacks(getRoomInfoRunnable);
            }
        } else {
            handler.removeCallbacks(getRoomInfoRunnable);
        }
    }

    private void getRoomInfo() {
        webRTCClient.getRoomInfo(roomId, streamId);
    }

    public void joinConference(View v) {

        if (!joined) {
            Log.w(getClass().getSimpleName(), "Joining Conference");
            ((Button) v).setText("Leave");
            webRTCClient.joinToConferenceRoom(roomId, streamId);
        } else {
            ((Button) v).setText("Join");
            webRTCClient.leaveFromConference(roomId);
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
        if(false && !track.id().contains(webRTCClient.getStreamId())) {
            SurfaceViewRenderer renderer = playViewRenderers.get(rendererIndex++%4);
            webRTCClient.addTrackToRenderer(track, renderer);
        }
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");

        if(!webRTCClient.isReconnectionInProgress() && !playOnlyMode) {
            publishStream(streamId);
        }

        if(playOnlyMode) {
            startPlaying(streams);
        }

        joined = true;
        // start periodic polling of room info
        scheduleGetRoomInfo();
        if(streams.length > 0) {
            //on track list triggers start playing
            onTrackList(streams);
        }
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        clearGetRoomInfoSchedule();
        joined = false;
        playMessageSent = false;
    }

    public void publishStream(String streamId) {
        if (!this.playOnlyMode) {
            webRTCClient.publish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, roomId);
        }
        else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    private void startPlaying(String[] streams) {
        if(!playMessageSent) {
            webRTCClient.play(roomId, token, streams, subscriberId, subscriberCode, viewerInfo);
            playMessageSent = true;
        }
    }

    @Override
    public void onRoomInformation(String[] streams) {
        if (webRTCClient != null) {
            startPlaying(streams);
        }
    }

    public void controlAudio(View view) {
        if (webRTCClient.isAudioOn()) {
            webRTCClient.disableAudio();
            audioButton.setText("Enable Audio");
        } else {
            webRTCClient.isAudioOn();
            audioButton.setText("Disable Audio");
        }
    }

    public void controlVideo(View view) {
        if (webRTCClient.isVideoOn()) {
            webRTCClient.disableVideo();
            videoButton.setText("Enable Video");

        } else {
            webRTCClient.enableVideo();
            videoButton.setText("Disable Video");
        }
    }

    public void changeWifiState(boolean state) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }
}

