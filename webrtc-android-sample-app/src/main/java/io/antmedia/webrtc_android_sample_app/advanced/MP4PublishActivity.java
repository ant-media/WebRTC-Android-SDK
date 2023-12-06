package io.antmedia.webrtc_android_sample_app.advanced;

import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.core.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.MediaFileReader;

public class MP4PublishActivity extends TestableActivity {

    private View broadcastingView;
    private View startStreamingButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private MediaFileReader mediaFileReader;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        broadcastingView = findViewById(R.id.broadcasting_text_view);
        startStreamingButton = findViewById(R.id.start_streaming_button);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        streamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(streamId);

        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setCustomVideoCapturerEnabled(true)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setInitiateBeforeStream(true)
                .build();


        mediaFileReader = MediaFileReader.fromResources(getResources(), R.raw.test)
                .withFrameType(MediaFileReader.FrameType.video)
                .withVideoFrameListener(yuvImage -> onYuvImage(yuvImage));

        mediaFileReader.start();


        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopStream(v);
            }
        });
    }

    private void onYuvImage(Image yuvImage) {
        int frameHeight = yuvImage.getHeight();
        int frameWidth = yuvImage.getWidth();

        ByteBuffer yData = (ByteBuffer) yuvImage.getPlanes()[0].getBuffer().rewind();
        ByteBuffer uData = (ByteBuffer) yuvImage.getPlanes()[1].getBuffer().rewind();
        ByteBuffer vData = (ByteBuffer) yuvImage.getPlanes()[2].getBuffer().rewind();

        final JavaI420Buffer buffer = JavaI420Buffer.wrap(frameWidth, frameHeight,
                yData, yuvImage.getPlanes()[0].getRowStride(),
                uData, yuvImage.getPlanes()[1].getRowStride(),
                vData, yuvImage.getPlanes()[2].getRowStride(),
                null);

        final long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

        VideoFrame videoFrame = new VideoFrame(buffer, 0 /* rotation */, captureTimeNs);
        ((CustomVideoCapturer)webRTCClient.getVideoCapturer()).writeFrame(videoFrame);
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

            mediaFileReader.stop();
            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                Toast.makeText(MP4PublishActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
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
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
                decrementIdle();
            }
        };
    }
}
