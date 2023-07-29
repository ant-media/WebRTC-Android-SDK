package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import androidx.annotation.RequiresApi;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

/**
 * This Activity is for demonstrating the data channel usage without video and audio
 * Steps:
 * set dataChannelOnly parameter of WebRTCClient
 * start WebRTC Cilent with play mode
 * if no stream exist is called start it in publish mode
 */
public class DataChannelOnlyActivity extends AbstractSampleSDKActivity {

    private boolean enableDataChannel = true;

    private WebRTCClient webRTCClient;

    private Button startStreamingButton;
    private String operationName = "";
    String tokenId = "tokenId";
    String serverUrl;

    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;

    // variables for handling reconnection attempts after disconnected
    final int RECONNECTION_PERIOD_MLS = 100;
    private boolean stoppedStream = false;
    Handler reconnectionHandler = new Handler();
    private EditText messageInput;
    private TextView messages;
    private EditText streamIdEditText;
    private View broadcastView;

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

        setContentView(R.layout.activity_data_channel_only);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        messageInput = findViewById(R.id.message_text_input);
        messages = findViewById(R.id.messages_view);
        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        broadcastView = findViewById(R.id.broadcasting_text_view);
        streamIdEditText = findViewById(R.id.stream_id_edittext);
        streamIdEditText.setText("streamId" + (int)(Math.random()*9999));

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), io.antmedia.webrtc_android_sample_app.SettingsActivity.DEFAULT_WEBSOCKET_URL);

        operationName = "DataChannel";

        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel);

        webRTCClient = new WebRTCClient( this,this);
        webRTCClient.setDataChannelOnly(true);
        webRTCClient.setDataChannelObserver(this);
        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);
        webRTCClient.init(serverUrl, streamIdEditText.getText().toString(), IWebRTCClient.MODE_PLAY, tokenId, this.getIntent());
    }

    public void startStreaming(View v) {
        idlingResource.increment();
        //update stream id if it is changed
        webRTCClient.setStreamId(streamIdEditText.getText().toString());
        if (!webRTCClient.isStreaming()) {
            ((Button) v).setText("Stop");
            webRTCClient.startStream();
        }
        else {
            ((Button)v).setText("Start");
            webRTCClient.stopStream();
            stoppedStream = true;
        }
    }

    public void sendMessage(View v) {
        String messageToSend = messageInput.getText().toString();
        messageInput.setText("");

        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf= new DataChannel.Buffer(buffer,false);
        webRTCClient.sendMessageViaDataChannel(buf);
    }

    @Override
    public void onPlayStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        broadcastView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_LONG).show();
        broadcastView.setVisibility(View.VISIBLE);
        decrementIdle();
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_LONG).show();
        broadcastView.setVisibility(View.GONE);
        decrementIdle();
    }

    @Override
    public void onPlayFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_LONG).show();
        broadcastView.setVisibility(View.GONE);
        decrementIdle();
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        //Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        //Toast.makeText(this, "No stream exist to play", Toast.LENGTH_LONG).show();
        decrementIdle();
        webRTCClient.stopStream();

        webRTCClient = new WebRTCClient( this,this);
        webRTCClient.setDataChannelOnly(true);
        webRTCClient.setDataChannelObserver(this);
        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);
        webRTCClient.init(serverUrl, streamId, IWebRTCClient.MODE_PUBLISH, tokenId, this.getIntent());

        startStreaming(startStreamingButton);
    }

    @Override
    protected void onStop() {
        super.onStop();
        webRTCClient.stopStream();
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
        startStreamingButton.setText("Stop");
    }

    @Override
    public void onIceDisconnected(String streamId) {
        //it's called when ice is disconnected
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
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        ByteBuffer data = buffer.data;
        String messageText = new String(data.array(), StandardCharsets.UTF_8);
        messages.append("received:"+messageText+"\n");
        Toast.makeText(this, "New Message: " + messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (successful) {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String messageText = new String(bytes, StandardCharsets.UTF_8);
            messages.append("sent:"+messageText+"\n");
            Toast.makeText(this, "Message is sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not send the text message", Toast.LENGTH_LONG).show();
        }
    }

}
