package io.antmedia.webrtc_android_sample_app.basic;

import static io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService.EXTRA_MEDIA_PROJECTION_DATA;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.WebRTCClientConfig;

import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

public class ScreenCaptureActivity extends TestableActivity {
    private View broadcastingView;
    private View startStreamingButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private RadioGroup bg;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screenshare);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        broadcastingView = findViewById(R.id.broadcasting_text_view);
        startStreamingButton = findViewById(R.id.start_streaming_button);
        TextView streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        streamId = "streamId" + (int)(Math.random()*9999);
        streamIdEditText.setText(streamId);

        bg = findViewById(R.id.rbGroup);
        bg.check(R.id.rbFront);
        bg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                IWebRTCClient.StreamSource newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
                if(checkedId == R.id.rbScreen) {
                    requestScreenCapture();
                    return;
                }
                else if(checkedId == R.id.rbFront) {
                    newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
                }
                else if(checkedId == R.id.rbRear) {
                    newSource = IWebRTCClient.StreamSource.REAR_CAMERA;
                }
                idlingResource.increment();
                webRTCClient.changeVideoSource(newSource);
                decrementIdle();
            }
        });

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
                startStopStream(v);
            }
        });
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
                Toast.makeText(ScreenCaptureActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
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



    public static final int CAPTURE_PERMISSION_REQUEST_CODE = 1234;
    public MediaProjectionManager mediaProjectionManager;

    @TargetApi(21)
    public void requestScreenCapture() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;

        // If the device version is v29 or higher, screen sharing will work service due to media projection policy.
        // Otherwise media projection will work without service
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){

            MediaProjectionService.setListener(mediaProjection -> {
                startScreenCapturer(mediaProjection);
            });

            Intent serviceIntent = new Intent(this, MediaProjectionService.class);
            serviceIntent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data);
            startForegroundService(serviceIntent);
        }
        else{
            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            startScreenCapturer(mediaProjection);
        }
    }

    private void startScreenCapturer(MediaProjection mediaProjection) {
        WebRTCClientConfig config = webRTCClient.getConfig();
        config.mediaProjection = mediaProjection;
        webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.SCREEN);
        decrementIdle();
    }
}
