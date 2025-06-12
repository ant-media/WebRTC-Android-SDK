package io.antmedia.webrtc_android_sample_app;

import static android.widget.Toast.LENGTH_LONG;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

public class TeacherActivity extends AppCompatActivity {
    private IWebRTCClient webRTCClient;
    private LinearLayout playersLayout;
    String streamId = "burak";
    String roomId = "room1";
    String serverUrl = "ws://10.0.2.2:5080/live/websocket";


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        playersLayout = findViewById(R.id.players);
        SurfaceViewRenderer teacherRenderer = findViewById(R.id.teacher_renderer);

        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(teacherRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setInitiateBeforeStream(true)
                .setBluetoothEnabled(true)
                .setWebRTCListener(createWebRTCListener(roomId, streamId))
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        View subscribersButton = findViewById(R.id.raise_hand_button);
        View makeStudentButton = findViewById(R.id.make_student_button);
        startStreamingButton.setOnClickListener(v -> startStopStream(v));
        subscribersButton.setOnClickListener(v -> {
            webRTCClient.getSubscriberCount(roomId);
            webRTCClient.getSubscriberList(roomId, 0, 10);
        });

        makeStudentButton.setOnClickListener(v -> {
            sendTextMessage("BECOME_STUDENT");
        });


    }


    public void startStopStream(View v) {
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            webRTCClient.joinToConferenceRoom(roomId, streamId);
        }
        else {
            ((Button) v).setText("Start");
            webRTCClient.leaveFromConference(roomId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(TeacherActivity.this, messageText, LENGTH_LONG);

                if (messageText.contains("RAISE_HAND")) {
                    Log.i("","Raise Hand detected!");
                    sendTextMessage("BECOME_ATTENDEE");
                }
            }
        };
    }

    private IWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {
            @Override
            public void onIceDisconnected(String streamId) {
                super.onIceDisconnected(streamId);
                Toast.makeText(TeacherActivity.this,"Disconnected.",Toast.LENGTH_SHORT).show();
            }

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
        webRTCClient.sendMessageViaDataChannel(streamId, buf);
    }
}