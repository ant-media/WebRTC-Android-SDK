package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import org.webrtc.DataChannel;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtc_android_sample_app.chat.Message;
import io.antmedia.webrtc_android_sample_app.chat.MessageAdapter;
import io.antmedia.webrtc_android_sample_app.chat.SettingsActivity;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

public class DataChannelActivity extends Activity implements IWebRTCListener, IDataChannelObserver, TextView.OnEditorActionListener {


    private WebRTCClient webRTCClient;
    private String webRTCMode;
    private Button startStreamingButton;
    private String operationName = "";
    private EditText messageInput;
    private MessageAdapter messageAdapter;
    private ListView messagesView;
    private Button settingsButton;
    SurfaceViewRenderer cameraViewRenderer;

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

        setContentView(R.layout.activity_data);

        messageInput = findViewById(R.id.message_text_input);
        messageInput.setOnEditorActionListener(this);
        messageInput.setEnabled(false);

        messageAdapter = new MessageAdapter(this);
        messagesView = findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);

        webRTCClient = new WebRTCClient( this,this);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverAddress = sharedPreferences.getString(getString(R.string.serverAddress), "192.168.1.23");
        String serverPort = sharedPreferences.getString(getString(R.string.serverPort), "5080");
        webRTCMode = sharedPreferences.getString(getString(R.string.stream_mode), IWebRTCClient.MODE_JOIN);

        String streamId = sharedPreferences.getString(getString(R.string.streamId), "stream1");
        String tokenId = "tokenId";
        String serverURL = "ws://" + serverAddress + ":" + serverPort + "/WebRTCAppEE/websocket";


        cameraViewRenderer = findViewById(R.id.camera_view_renderer);

        SurfaceViewRenderer pipViewRenderer = findViewById(R.id.pip_view_renderer);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        settingsButton = findViewById(R.id.settings);
        cameraViewRenderer.setZOrderOnTop(true);
        webRTCClient.setVideoRenderers(cameraViewRenderer, pipViewRenderer);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);


        if (webRTCMode.equals(IWebRTCClient.MODE_PUBLISH)) {
            startStreamingButton.setText("Start Publishing");
            operationName = "Publishing";
        }
        else  if (webRTCMode.equals(IWebRTCClient.MODE_PLAY)) {
            startStreamingButton.setText("Start Playing");
            operationName = "Playing";
        }
        else if (webRTCMode.equals(IWebRTCClient.MODE_JOIN)) {
            startStreamingButton.setText("Start P2P");
            operationName = "P2P";
        }
        webRTCClient.setDataChannelObserver(this);

        // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(serverURL, streamId, webRTCMode, tokenId, this.getIntent());
    }


    public void startStreaming(View v) {

        if (!webRTCClient.isStreaming()) {
            ((Button)v).setText("Stop " + operationName);
            webRTCClient.startStream();
            //final ByteBuffer buffer = ByteBuffer.allocate(5);
        }
        else {
            ((Button)v).setText("Start " + operationName);
            webRTCClient.stopStream();
        }
    }

    public void sendDataMessage() {
        String messageToSend = messageInput.getText().toString();
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(Charset.defaultCharset()));
        DataChannel.Buffer buf= new DataChannel.Buffer(buffer,false);
        webRTCClient.sendMessageViaDataChannel(buf);
    }

    @Override
    public void onPlayStarted() {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        webRTCClient.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        messageInput.setEnabled(true);
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
        messageInput.setEnabled(false);
        settingsButton.setEnabled(true);
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onIceDisconnected() {
    }

    @Override
    public void onDisconnected() {
        messageInput.setEnabled(false);
        settingsButton.setEnabled(true);
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();

        //finish();
    }

    @Override
    public void onIceConnected() {
        //it is called when connected to ice
        messageInput.setEnabled(true);
        settingsButton.setEnabled(false);
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

    }

    @Override
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {
        Log.d(DataChannelActivity.class.getName(), "Data channel buffered amount changed: " );
    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {
        Log.d(DataChannelActivity.class.getName(), "Data channel state changed: " );
    }

    @Override
    public void onMessage(final DataChannel.Buffer buffer, String dataChannelLabel) {
        if (buffer.binary) {
            Log.d(DataChannelActivity.class.getName(), "Received binary msg over " );
        } else {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String strData = new String(bytes, StandardCharsets.UTF_8);

            final Message message = new Message(strData, true);
            messageAdapter.add(message);
            // scroll the ListView to the last added element
            messagesView.setSelection(messagesView.getCount() - 1);
        }
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (!buffer.binary) {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String strData = new String(bytes, StandardCharsets.UTF_8);

            final Message message = new Message(strData, false);
            messageAdapter.add(message);
            // scroll the ListView to the last added element
            messagesView.setSelection(messagesView.getCount() - 1);
        } else {
            //TODO
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendDataMessage();
            handled = true;
            messageInput.setText("");
        }
        return handled;
    }

    public void sendMessage(View view) {
        if (messageInput.isEnabled()) {
            sendDataMessage();
            messageInput.setText("");
        }
    }

    public void goToSettings(View view) {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

}


