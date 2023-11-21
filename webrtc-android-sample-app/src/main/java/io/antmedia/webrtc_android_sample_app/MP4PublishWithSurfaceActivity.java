package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_HEIGHT;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_WIDTH;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import org.webrtc.SurfaceViewRenderer;

import java.io.IOException;
import java.nio.ByteBuffer;

import io.antmedia.webrtcandroidframework.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.github.crow_misia.libyuv.AbgrBuffer;
import io.github.crow_misia.libyuv.I420Buffer;
import io.github.crow_misia.libyuv.Plane;
import io.github.crow_misia.libyuv.PlanePrimitive;

public class MP4PublishWithSurfaceActivity extends AbstractSampleSDKActivity {

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
    private Surface surface;

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


        this.getIntent().putExtra(EXTRA_VIDEO_FPS, 20);
        this.getIntent().putExtra(EXTRA_VIDEO_BITRATE, 1500);
        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel);
        this.getIntent().putExtra(EXTRA_VIDEO_WIDTH, 360);
        this.getIntent().putExtra(EXTRA_VIDEO_HEIGHT, 640);

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

            if(surface == null) {
                SurfaceTexture surfaceTexture = ((CustomVideoCapturer) webRTCClient.getVideoCapturer()).getSurfaceTextureHelper().getSurfaceTexture();
                surface = new Surface(surfaceTexture);
            }

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

    private void sendFrame(Image image) {
        byte[] data = new byte[image.getHeight() * image.getWidth() * 3 / 2];
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        ByteBuffer bufferU = image.getPlanes()[1].getBuffer();
        ByteBuffer bufferV = image.getPlanes()[2].getBuffer();

        int strideY = image.getPlanes()[0].getRowStride();
        int strideU = image.getPlanes()[1].getRowStride();
        int strideV = image.getPlanes()[2].getRowStride();

        //drawYUVonSurface(data);


        PlanePrimitive planeY = new PlanePrimitive(strideY, bufferY);
        PlanePrimitive planeU = new PlanePrimitive(strideU, bufferU);
        PlanePrimitive planeV = new PlanePrimitive(strideV, bufferV);

        I420Buffer yuvBuffer = I420Buffer.Factory.wrap(planeY, planeU, planeV, 640, 360);
        AbgrBuffer rgbBuffer = AbgrBuffer.Factory.allocate(640, 360);

        yuvBuffer.convertTo(rgbBuffer);

        Canvas canvas = surface.lockCanvas(null);
        canvas.drawBitmap(rgbBuffer.asBitmap(), 0, 0, null);
        surface.unlockCanvasAndPost(canvas);
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
