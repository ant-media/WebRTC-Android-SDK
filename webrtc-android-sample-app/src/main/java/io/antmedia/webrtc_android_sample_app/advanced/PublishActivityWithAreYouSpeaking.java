package io.antmedia.webrtc_android_sample_app.advanced;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtc_android_sample_app.utility.SoundMeter;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class PublishActivityWithAreYouSpeaking extends TestableActivity {
    private View broadcastingView;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private SoundMeter soundMeter;
    boolean microphoneMuted = false;
    private final double SPEAKING_DECIBEL_THRESHOLD = 45;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_are_you_speaking);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        broadcastingView = findViewById(R.id.broadcasting_text_view);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        String generatedStreamId = "streamId" + (int) (Math.random() * 9999);
        streamIdEditText.setText(generatedStreamId);

        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamId = streamIdEditText.getText().toString();
                startStopStream(v);
            }
        });

        Button toggleMicrophoneButton = findViewById(R.id.toggle_microphone_button);
        toggleMicrophoneButton.setOnClickListener(v -> {
            if (microphoneMuted) {
                toggleMicrophoneButton.setText("Mute");
                unMuteMicrophone();

            } else {
                toggleMicrophoneButton.setText("Unmute");
                muteMicrophone();
            }
        });


    }

    public void startSoundMeter(){
        try {
            soundMeter = new SoundMeter(250, this, decibelLevel -> {
                // Handle updated audio level here
                // This method will be called whenever there's an audio level update
                Log.d("PublishActivity", "Received audio level update: " + decibelLevel);
                if(microphoneMuted && decibelLevel >= SPEAKING_DECIBEL_THRESHOLD){
                    runOnUiThread(() -> {
                        Toast.makeText(this,"Are you speaking?",Toast.LENGTH_SHORT).show();
                    });
                }
            });
            soundMeter.start();
        }catch (SecurityException e){
            runOnUiThread(() -> {
                Toast.makeText(this,"Permission to record audio not granted", Toast.LENGTH_SHORT).show();
            });

        }
    }

    public void stopSoundMeter(){
        if(soundMeter != null){
            soundMeter.stop();
        }
    }

    public void muteMicrophone(){
        webRTCClient.setAudioEnabled(false);
        microphoneMuted = true;
    }

    public void unMuteMicrophone(){
        webRTCClient.setAudioEnabled(true);
        microphoneMuted = false;
    }

    public void startStopStream(View v) {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.publish(streamId);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(PublishActivityWithAreYouSpeaking.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                broadcastingView.setVisibility(View.VISIBLE);
                decrementIdle();
                startSoundMeter();

            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
                decrementIdle();
                stopSoundMeter();
            }
        };
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
        webRTCClient.sendMessageViaDataChannel(streamId, buf);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(soundMeter != null){
            soundMeter.stop();
        }
    }

    public void showSendDataChannelMessageDialog(View view) {
        if (webRTCClient != null && webRTCClient.isDataChannelEnabled()) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send Message via Data Channel");
            // set the custom layout
            final View customLayout = getLayoutInflater().inflate(R.layout.send_message_data_channel, null);
            builder.setView(customLayout);
            // add a button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText editText = customLayout.findViewById(R.id.message_text_input);
                    sendTextMessage(editText.getText().toString());
                }
            });
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, R.string.data_channel_not_available, Toast.LENGTH_LONG).show();
        }
    }

}
