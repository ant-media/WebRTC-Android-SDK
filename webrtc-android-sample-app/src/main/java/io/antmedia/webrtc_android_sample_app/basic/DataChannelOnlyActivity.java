package io.antmedia.webrtc_android_sample_app.basic;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.WebRTCClient;

/**
 * This Activity is for demonstrating the data channel usage without video and audio
 * Steps:
 * set dataChannelOnly parameter of WebRTCClient
 * start WebRTC Cilent with play mode
 * if no stream exist is called start it in publish mode
 */
public class DataChannelOnlyActivity extends TestableActivity {
    private WebRTCClient webRTCClient;
    private View broadcastingView;
    private Button startStreamingButton;
    private String operationName = "";
    private EditText messageInput;
    private TextView messages;
    private View broadcastView;
    private String streamId;
    private TextView streamIdEditText;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_channel_only);

        broadcastingView = findViewById(R.id.broadcasting_text_view);
        startStreamingButton = findViewById(R.id.start_streaming_button);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        setContentView(R.layout.activity_data_channel_only);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        messageInput = findViewById(R.id.message_text_input);
        messages = findViewById(R.id.messages_view);

        broadcastView = findViewById(R.id.broadcasting_text_view);
        streamIdEditText = findViewById(R.id.stream_id_edittext);
        streamIdEditText.setText("streamId" + (int)(Math.random()*9999));

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        streamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(streamId);

        webRTCClient = IWebRTCClient.builder()
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setVideoCallEnabled(false)
                .setAudioCallEnabled(false)
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamId = streamIdEditText.getText().toString();
                startStopStream(v);
            }
        });
    }

    public void startStopStream(View v) {
        incrementIdle();
        streamId = streamIdEditText.getText().toString();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.play(streamId);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.stop(streamId);
        }
    }

    public void sendMessage(View v) {
        String messageToSend = messageInput.getText().toString();
        messageInput.setText("");

        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf= new DataChannel.Buffer(buffer,false);
        webRTCClient.sendMessageViaDataChannel(streamId, buf);
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
                String messageText = toTextMessage(buffer);
                Toast.makeText(DataChannelOnlyActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
                messages.append("received:"+messageText+"\n");
            }

            @Override
            public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
                if (successful) {
                    String messageText = toTextMessage(buffer);
                    Log.i(DefaultDataChannelObserver.class.getSimpleName(), "Message is sent");
                    Toast.makeText(DataChannelOnlyActivity.this, "Message is sent", Toast.LENGTH_SHORT).show();
                    messages.append("sent:"+messageText+"\n");
                } else {
                    Toast.makeText(DataChannelOnlyActivity.this, "Could not send the text message", Toast.LENGTH_LONG).show();
                    Log.e(DefaultDataChannelObserver.class.getSimpleName(),"Could not send the text message");
                }
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {

            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
                runOnUiThread(() -> {
                    startStreamingButton.setEnabled(true);
                    Toast.makeText(DataChannelOnlyActivity.this,"Websocket connected",Toast.LENGTH_SHORT).show();

                });
            }

            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                broadcastingView.setVisibility(View.VISIBLE);
                decrementIdle();
            }

            @Override
            public void onPlayFinished(String streamId) {
                super.onPlayFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
                decrementIdle();
            }
        };
    }

}
