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

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.MultitrackConferenceManager;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class PushToTalkActivity extends AbstractSampleSDKActivity {

    private MultitrackConferenceManager conferenceManager;

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

        conferenceManager.init();
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
        }
    }
    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();

        runOnUiThread(() -> controlAudio(false));
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

