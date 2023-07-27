package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_CALL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.MultitrackConferenceManager;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class PushToTalkActivity extends Activity implements IWebRTCListener, IDataChannelObserver {

    private MultitrackConferenceManager conferenceManager;

    final int RECONNECTION_PERIOD_MLS = 1000;
    private boolean stoppedStream = false;

    @SuppressLint("WrongViewCast")
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

        this.getIntent().putExtra(EXTRA_VIDEO_CALL, false);

        setContentView(R.layout.activity_ptt);

        ArrayList<SurfaceViewRenderer> playViewRenderers = new ArrayList<>();

       // playViewRenderers.add(findViewById(R.id.play_view_renderer1));

        View talkButton = findViewById(R.id.talkButton);

        talkButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    controlAudio(false);
                    view.setBackgroundColor(Color.RED);
                    return true;
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    controlAudio(true);
                    view.setBackgroundColor(Color.GREEN);
                    return true;
                }
                return false;
            }
        });

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        //  this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_CALL, false);

        String streamId = "stream"+  (int)(Math.random()*9999);
        String roomId = "room1";
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        conferenceManager = new MultitrackConferenceManager(
                this,
                this,
                getIntent(),
                serverUrl,
                roomId,
                null,
                playViewRenderers,
                streamId,
                this
        );

        conferenceManager.setPlayOnlyMode(false);
        conferenceManager.setOpenFrontCamera(true);
    }

    public void joinConference(View v) {

        if (!conferenceManager.isJoined()) {
            Log.w(getClass().getSimpleName(), "Joining Conference");
            ((Button)v).setText("Leave");
            conferenceManager.joinTheConference();
        }
        else {
            ((Button)v).setText("Join");
            conferenceManager.leaveFromConference();
            stoppedStream = true;
        }
    }


    @Override
    public void onPlayStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();

        runOnUiThread(() -> controlAudio(false));
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPlayFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        Toast.makeText(this, "No stream exist to play", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void streamIdInUse(String streamId) {
        Log.w(getClass().getSimpleName(), "streamIdInUse");
        Toast.makeText(this, "Stream id is already in use.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String description, String streamId) {
        Toast.makeText(this, "Error: "  +description , Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stoppedStream = true;
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected(String streamId) {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
    }

    @Override
    public void onIceDisconnected(String streamId) {
        Log.w(getClass().getSimpleName(), "Conference manager publish stream id left" + streamId);
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

    @Override
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {

    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        ByteBuffer data = buffer.data;
        String strDataJson = new String(data.array(), StandardCharsets.UTF_8);

        try {
            JSONObject json = new JSONObject(strDataJson);
            String eventType = json.getString("eventType");
            String streamId = json.getString("streamId");
            Toast.makeText(this, eventType + " : " + streamId, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        ByteBuffer data = buffer.data;
        String strDataJson = new String(data.array(), StandardCharsets.UTF_8);

        Log.e(getClass().getSimpleName(), "SentEvent: " + strDataJson);
    }

    public void controlAudio(boolean enable) {
        if (enable) {
            conferenceManager.enableAudio();
            conferenceManager.updateAudioLevel(10);
        } else {
            conferenceManager.disableAudio();
            conferenceManager.updateAudioLevel(0);
        }
    }
}

