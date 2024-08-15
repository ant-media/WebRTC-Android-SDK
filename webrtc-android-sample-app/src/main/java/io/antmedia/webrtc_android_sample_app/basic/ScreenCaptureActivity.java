package io.antmedia.webrtc_android_sample_app.basic;

import static io.antmedia.webrtc_android_sample_app.basic.MediaProjectionService.EXTRA_MEDIA_PROJECTION_DATA;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.api.WebRTCClientConfig;

import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;

public class ScreenCaptureActivity extends TestableActivity {

    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1234;
    private static final int REQUEST_CODE_PERMISSIONS = 1001;

    private View broadcastingView;
    private View startStreamingButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private RadioGroup bg;

    private MediaProjectionManager mediaProjectionManager;

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
        streamId = "streamId" + (int) (Math.random() * 9999);
        streamIdEditText.setText(streamId);

        bg = findViewById(R.id.rbGroup);
        bg.check(R.id.rbFront);
        bg.setOnCheckedChangeListener((group, checkedId) -> {
            IWebRTCClient.StreamSource newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
            if (checkedId == R.id.rbScreen) {
                requestScreenCapture();
                return;
            } else if (checkedId == R.id.rbFront) {
                newSource = IWebRTCClient.StreamSource.FRONT_CAMERA;
            } else if (checkedId == R.id.rbRear) {
                newSource = IWebRTCClient.StreamSource.REAR_CAMERA;
            }
            webRTCClient.changeVideoSource(newSource);
        });

        webRTCClient = IWebRTCClient.builder()
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setInitiateBeforeStream(true)
                .build();

        startStreamingButton.setOnClickListener(v -> {
            streamId = streamIdEditText.getText().toString();
            startStopStream(v);
        });

        // Check and request necessary permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasPermissions()) {
                requestPermissions(new String[]{
                        android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION

                }, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!hasPermissions()) {
                Toast.makeText(this, "Permissions not granted, cannot start screen capture", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startStopStream(View v) {
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");
            webRTCClient.publish(streamId);
        } else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling publish stop");
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
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
            }
        };
    }

    @TargetApi(21)
    private void requestScreenCapture() {
        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
            return;

        if (resultCode == RESULT_OK && data != null && hasPermissions()) {
            WebRTCClientConfig config = webRTCClient.getConfig();
            config.mediaProjectionIntent = data;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaProjectionService.setListener(() -> startScreenCapturer());

                Intent serviceIntent = new Intent(this, MediaProjectionService.class);
                serviceIntent.putExtra(EXTRA_MEDIA_PROJECTION_DATA, data);
                startForegroundService(serviceIntent);
            } else {
              //  MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                startScreenCapturer();
            }
        } else {
            Toast.makeText(this, "Screen capture permission denied or missing permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScreenCapturer() {
        WebRTCClientConfig config = webRTCClient.getConfig();
      //  config.mediaProjection = mediaProjection;
        webRTCClient.changeVideoSource(IWebRTCClient.StreamSource.SCREEN);
    }
}
