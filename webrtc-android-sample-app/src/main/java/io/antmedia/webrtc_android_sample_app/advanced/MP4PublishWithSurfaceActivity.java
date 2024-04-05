package io.antmedia.webrtc_android_sample_app.advanced;

import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.SurfaceViewRenderer;

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
//import io.github.crow_misia.libyuv.AbgrBuffer;
//import io.github.crow_misia.libyuv.I420Buffer;
//import io.github.crow_misia.libyuv.PlanePrimitive;

public class MP4PublishWithSurfaceActivity extends TestableActivity {


    private Surface surface;

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
                .setVideoSource(IWebRTCClient.StreamSource.CUSTOM)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .build();


        mediaFileReader = MediaFileReader.fromResources(getResources(), R.raw.test)
                .withFrameType(MediaFileReader.FrameType.video)
                .withVideoFrameListener(yuvImage->onYuvImage(yuvImage));

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                streamId = streamIdEditText.getText().toString();
                startStopStream(v);
            }
        });
    }


    //We can use surface with rgb so we convert yuv to rgb first
    private void onYuvImage(Image image) {
        /*
        ByteBuffer bufferY = image.getPlanes()[0].getBuffer();
        ByteBuffer bufferU = image.getPlanes()[1].getBuffer();
        ByteBuffer bufferV = image.getPlanes()[2].getBuffer();

        int strideY = image.getPlanes()[0].getRowStride();
        int strideU = image.getPlanes()[1].getRowStride();
        int strideV = image.getPlanes()[2].getRowStride();

        PlanePrimitive planeY = new PlanePrimitive(strideY, bufferY);
        PlanePrimitive planeU = new PlanePrimitive(strideU, bufferU);
        PlanePrimitive planeV = new PlanePrimitive(strideV, bufferV);

        I420Buffer yuvBuffer = I420Buffer.Factory.wrap(planeY, planeU, planeV, 640, 360);
        AbgrBuffer rgbBuffer = AbgrBuffer.Factory.allocate(640, 360);

        yuvBuffer.convertTo(rgbBuffer);

        Canvas canvas = surface.lockCanvas(null);
        canvas.drawBitmap(rgbBuffer.asBitmap(), 0, 0, null);
        surface.unlockCanvasAndPost(canvas);
        */
    }

    public void startStopStream(View v) {
        incrementIdle();

        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling publish start");

            webRTCClient.publish(streamId);

            if(surface == null) {
                SurfaceTexture surfaceTexture = ((CustomVideoCapturer) webRTCClient.getVideoCapturer())
                        .getSurfaceTextureHelper().getSurfaceTexture();
                surface = new Surface(surfaceTexture);
            }

            mediaFileReader.start();
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
                Toast.makeText(MP4PublishWithSurfaceActivity.this, "Message received: " + messageText, Toast.LENGTH_SHORT).show();
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
