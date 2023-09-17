package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.ConferenceManager;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

/*
 * This activity is a sample activity shows how to implement Stream Based Conference Solution
 * https://antmedia.io/reveal-the-secrets-of-3-types-of-video-conference-solutions/
 *
 * @deprecated use {@link TrackBasedConferenceActivity} instead
 */
@Deprecated
public class ConferenceActivity extends AbstractSampleSDKActivity {

    private ConferenceManager conferenceManager;
    private Button audioButton;
    private Button videoButton;
    private String serverUrl;
    private TextView broadcastingView;

    private boolean stoppedStream = false;

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

        setContentView(R.layout.activity_conference);

        broadcastingView = findViewById(R.id.broadcasting_text_view);


        SurfaceViewRenderer publishViewRenderer = findViewById(R.id.publish_view_renderer);
        ArrayList<SurfaceViewRenderer> playViewRenderers = new ArrayList<>();

        playViewRenderers.add(findViewById(R.id.play_view_renderer1));
        playViewRenderers.add(findViewById(R.id.play_view_renderer2));
        playViewRenderers.add(findViewById(R.id.play_view_renderer3));
        playViewRenderers.add(findViewById(R.id.play_view_renderer4));

        audioButton = findViewById(R.id.control_audio_button);
        videoButton = findViewById(R.id.control_video_button);

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        //  this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_CALL, false);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        String roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        String streamId = null; //"stream1";
        conferenceManager = new ConferenceManager(
                this,
                this,
                getIntent(),
                serverUrl,
                roomId,
                publishViewRenderer,
                playViewRenderers,
                streamId,
                this
        );

        conferenceManager.setPlayOnlyMode(false);
        conferenceManager.setOpenFrontCamera(true);
    }
    public void joinConference(View v) {
        incrementIdle();
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
        decrementIdle();
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();
        broadcastingView.setText("Publishing");
        broadcastingView.setVisibility(View.VISIBLE);
        decrementIdle();

    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.GONE);
        decrementIdle();
    }

    @Override
    protected void onStop() {
        super.onStop();
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
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
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
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

    public void switchCamera(View view) {
        conferenceManager.switchCamera();
    }

    @Override
    public void onNewVideoTrack(VideoTrack track) {
    }

}

