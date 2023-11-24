package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.apache.commons.lang3.RandomStringUtils;
import org.webrtc.JavaI420Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtcandroidframework.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class MP4PublishActivity extends AbstractSampleSDKActivity {

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

    Handler handler = new Handler();

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
        webRTCClient.setVideoRenderers(null, cameraViewRenderer);
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

            Thread t = new Thread() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void run() {
                    extractFrames();
                }
            };
            t.start();

        }
        else {
            ((Button)startStreamingButton).setText("Start " + operationName);
            Log.i(getClass().getSimpleName(), "Calling stopStream");
            webRTCClient.stopStream();
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

    private void sendFrame(Image yuvImage) {
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void extractFrames() {

        MediaExtractor extractor = new MediaExtractor();
        try {
            Resources res = this.getResources();
            extractor.setDataSource(res.openRawResourceFd(R.raw.test));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        int trackCount = extractor.getTrackCount();
        int videoTrackIndex = -1;
        MediaFormat videoFormat = null;

        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                videoTrackIndex = i;
                videoFormat = format;
                break;
            }
        }

        if (videoTrackIndex == -1 || videoFormat == null) {
            Log.e("MP4Publish", "No video track found.");
            return;
        }

        extractor.selectTrack(videoTrackIndex);

        try {
            MediaCodec decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME));

            decoder.configure(videoFormat, null, null, 0);
            //Surface surface = decoder.createInputSurface();
            decoder.start();

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            boolean isEOS = false;

            while (!Thread.interrupted() && !isEOS) {
                int inputIndex = decoder.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputIndex);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        isEOS = true;
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }

                int outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (outputIndex >= 0) {
                    Image yuvImage = decoder.getOutputImage(outputIndex);

                    sendFrame(yuvImage);

                    yuvImage.close();
                    decoder.releaseOutputBuffer(outputIndex, true);
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Handle format change if needed
                }

                Thread.sleep(50);
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
