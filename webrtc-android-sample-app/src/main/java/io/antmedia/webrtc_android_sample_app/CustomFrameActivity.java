package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.apache.commons.lang3.RandomStringUtils;
import org.webrtc.DataChannel;
import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
//import io.github.crow_misia.libyuv.AbgrBuffer;
//import io.github.crow_misia.libyuv.I420Buffer;

public class CustomFrameActivity extends AbstractSampleSDKActivity {

    private boolean enableDataChannel = true;


    private WebRTCClient webRTCClient;

    private Button startStreamingButton;
    private String operationName = "";
    private String serverUrl;
    private String restUrl;

    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;
    private Spinner streamInfoListSpinner;
    private TextView broadcastingView;
    private EditText streamIdEditText;
    private Bitmap bitmapImage;
    private Timer frameFeedTimer;

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

        setContentView(R.layout.activity_main);

        cameraViewRenderer = findViewById(R.id.camera_view_renderer);
        pipViewRenderer = findViewById(R.id.pip_view_renderer);

        broadcastingView = findViewById(R.id.broadcasting_text_view);

        streamIdEditText = findViewById(R.id.stream_id_edittext);
        streamIdEditText.setText("streamId" + RandomStringUtils.randomNumeric(5));

        startStreamingButton = findViewById(R.id.start_streaming_button);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        startStreamingButton.setText("Start Publishing");
        operationName = "Publishing";


        this.getIntent().putExtra(EXTRA_VIDEO_FPS, 30);
        this.getIntent().putExtra(EXTRA_VIDEO_BITRATE, 1500);
        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel);

        webRTCClient = new WebRTCClient( this,this);

        //webRTCClient.setOpenFrontCamera(false);

        String tokenId = "tokenId";
        webRTCClient.setVideoRenderers(null, null);
        webRTCClient.setCustomCapturerEnabled(true);

        // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(serverUrl, streamIdEditText.getText().toString(), IWebRTCClient.MODE_PUBLISH, tokenId, this.getIntent());
        webRTCClient.setDataChannelObserver(this);

    }

    public void startStreaming(View v) {
        //update stream id if it is changed
        webRTCClient.setStreamId("stream2");//streamIdEditText.getText().toString());
        incrementIdle();
        if (!webRTCClient.isStreaming()) {
            ((Button) v).setText("Stop " + operationName);
            Log.i(getClass().getSimpleName(), "Calling startStream");

            webRTCClient.startStream();

            bitmapImage = BitmapFactory.decodeResource(getResources(), R.drawable.test);
            bitmapImage = Bitmap.createScaledBitmap(bitmapImage, 360, 640, false);

            frameFeedTimer = new Timer();
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                    VideoFrame videoFrame = getNextFrame();
                    ((CustomVideoCapturer)webRTCClient.getVideoCapturer()).writeFrame(videoFrame);
                }
            };

            frameFeedTimer.schedule(tt, 0, 50);

        }
        else {
            ((Button)startStreamingButton).setText("Start " + operationName);
            Log.i(getClass().getSimpleName(), "Calling stopStream");
            webRTCClient.stopStream();
            frameFeedTimer.cancel();
        }

    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();
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
        if (webRTCClient != null) {
            Log.i(getClass().getSimpleName(), "onStop and calling stopStream");
            webRTCClient.stopStream();
        }
    }

    @Override
    public void onDisconnected(String streamId) {

        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.GONE);
        decrementIdle();
        startStreamingButton.setText("Start " + operationName);
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
        startStreamingButton.setText("Stop " + operationName);
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {
        Log.e(getClass().getSimpleName(), "st:"+streamId+" tb:"+targetBitrate+" vb:"+videoBitrate+" ab:"+audioBitrate);
        if(targetBitrate < (videoBitrate+audioBitrate)) {
            Toast.makeText(this, "low bandwidth", Toast.LENGTH_SHORT).show();
        }
    }

    public VideoFrame getNextFrame() {
        /*

        final long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

        int frameWidth = bitmapImage.getWidth();
        int frameHeight = bitmapImage.getHeight();
        AbgrBuffer originalBuffer = AbgrBuffer.Factory.allocate(frameWidth, frameHeight);
        I420Buffer i420Buffer = I420Buffer.Factory.allocate(frameWidth, frameHeight);

        bitmapImage.copyPixelsToBuffer(originalBuffer.asBuffer());
        originalBuffer.convertTo(i420Buffer);

        int ySize = frameWidth * frameHeight;
        int uvSize = ySize / 4;

        final JavaI420Buffer buffer = JavaI420Buffer.wrap(frameWidth, frameHeight,
                    i420Buffer.getPlaneY().getBuffer(), i420Buffer.getPlaneY().getRowStride(),
                    i420Buffer.getPlaneU().getBuffer(), i420Buffer.getPlaneU().getRowStride(),
                    i420Buffer.getPlaneV().getBuffer(), i420Buffer.getPlaneV().getRowStride(),
                    null);

        originalBuffer.close();
                return new VideoFrame(buffer, 0, captureTimeNs);

        */
        return null;
    }
}
