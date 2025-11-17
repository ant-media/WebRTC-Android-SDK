package io.antmedia.webrtc_android_sample_app.advanced;

import android.Manifest;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import io.antmedia.webrtc_android_sample_app.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import javax.microedition.khronos.opengles.GL10;

import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.canvas.CameraProviderHelper;
import io.antmedia.webrtcandroidframework.canvas.CanvasListener;
import io.antmedia.webrtcandroidframework.canvas.ImageProxyRenderer;
import io.antmedia.webrtcandroidframework.canvas.Overlay;
import io.antmedia.webrtcandroidframework.core.WebRTCClient;

public class CustomCanvasActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private GLSurfaceView surfaceView;
    private ImageProxyRenderer imageProxyRenderer;

    WebRTCClient webRTCClient;
    private View broadcastingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_custom_canvas);
        broadcastingView = findViewById(R.id.broadcasting_text_view);
        setup();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO },
                1);

    }
    CameraProviderHelper cameraProviderHelper;
    void setup() {
        cameraProviderHelper = new CameraProviderHelper(this, provider -> {
            bindImageAnalysis(provider);
        });

        String streamId = "test1";
        webRTCClient = IWebRTCClient.builder()
                .setServerUrl("ws://192.168.0.108:5080/LiveApp/websocket")
                .setActivity(this)
                .setVideoSource(IWebRTCClient.StreamSource.CUSTOM)
                .setWebRTCListener(createWebRTCListener())
                .setInitiateBeforeStream(true)
                .build();

        surfaceView = new GLSurfaceView(this);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8,8,8,8,16,0);

        imageProxyRenderer = new ImageProxyRenderer(webRTCClient,this,surfaceView, new CanvasListener(){
            boolean overlayInitialize = false;
            @Override
            public void onSurfaceInitialized() {
                if(!overlayInitialize){
                    overlayInitialize = true;
                    Overlay logo = new Overlay(getApplicationContext(), R.drawable.test,0.8f,0.8f);
                    logo.setSize(0.2f);
                    Overlay text = new Overlay(getApplicationContext(), "Hello", 64, Color.RED, 0f, -0.3f);
                    text.setSize(0.12f);
                }
            }
            @Override
            public void onOrientationChanged(int orientation) {
            }
        });
        surfaceView.setRenderer(imageProxyRenderer);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        FrameLayout local = findViewById(R.id.localPreview);
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );

        params.gravity = Gravity.CENTER;

        surfaceView.setLayoutParams(params);
        local.addView(surfaceView);

        final Button startStopBtn = findViewById(R.id.startCall);
        startStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopStream(v,streamId);
            }
        });
        final Button switchCam = findViewById(R.id.switchCam);
        switchCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraProviderHelper.switchCamera(imageAnalysis);
            }
        });
    }
    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onIceConnected(String streamId) {
                imageProxyRenderer.setCallInProgress(true);
            }
            @Override
            public void onIceDisconnected(String streamId){
                imageProxyRenderer.setCallInProgress(false);
            }
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
    public void startStopStream(View v,String streamId) {
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
    ImageAnalysis imageAnalysis;
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
         imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                 .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                imageProxyRenderer.submitImage(image);
                image.close();
            }
        });
        cameraProviderHelper.startCamera(imageAnalysis);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}