package io.antmedia.webrtc_android_sample_app;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.api.PlayParams;

public class StudentActivity extends AppCompatActivity {
    private IWebRTCClient webRTCClient;
    private LinearLayout playersLayout;
    private String streamId = "melik";
    private String roomId = "room1";
    String serverUrl = "ws://10.0.2.2:5080/live/websocket";
    private SurfaceViewRenderer studentRenderer;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        playersLayout = findViewById(R.id.players);

        studentRenderer = findViewById(R.id.student_renderer);


        webRTCClient = IWebRTCClient.builder()
                .setServerUrl(serverUrl)
                .setLocalVideoRenderer(studentRenderer)
                .setActivity(this)
                .setVideoCallEnabled(false)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        View raiseHandButton = findViewById(R.id.raise_hand_button);
        startStreamingButton.setOnClickListener(v -> startStopStream(v));
        raiseHandButton.setOnClickListener(v -> {
            sendTextMessage("RAISE_HAND");
        });

        studentRenderer.setVisibility(INVISIBLE);
    }


    public void startStopStream(View v) {
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling play start");
            PlayParams params = new PlayParams(
                    roomId,
                    null,
                    null,
                    "subscriberMelik",
                    "Subscriber Melik",
                    null,
                    null,
                    true
            );

            webRTCClient.play(params);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling play start");
            webRTCClient.stop(roomId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(StudentActivity.this, messageText, LENGTH_LONG);

                if (messageText.contains("BECOME_ATTENDEE")) {
                    Log.i("","BECOME_ATTENDEE");
                    studentRenderer.setVisibility(VISIBLE);
                    webRTCClient.publish(streamId, null,
                            false, true,
                            null, null,
                            "melik", roomId);

                    //important enable track
                    webRTCClient.enableTrack(roomId, "burak", true);
                }
                else if (messageText.contains("BECOME_STUDENT")) {
                    Log.i("","BECOME_STUDENT");
                    studentRenderer.setVisibility(INVISIBLE);

                    webRTCClient.stop(streamId);

                    //important disable track
                    webRTCClient.enableTrack(roomId, "burak", false);

                }
            }
        };
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onNewVideoTrack(VideoTrack videoTrack, String trackId) {
                runOnUiThread(() -> {
                    SurfaceViewRenderer renderer = new SurfaceViewRenderer(getApplicationContext());
                    renderer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    playersLayout.addView(renderer);
                    webRTCClient.getConfig().remoteVideoRenderers.add(renderer);
                    renderer.setTag(videoTrack);
                    webRTCClient.setRendererForVideoTrack(renderer, videoTrack);
                });
            }
        };
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);

        //important roomId
        webRTCClient.sendMessageViaDataChannel(roomId, buf);
    }
}