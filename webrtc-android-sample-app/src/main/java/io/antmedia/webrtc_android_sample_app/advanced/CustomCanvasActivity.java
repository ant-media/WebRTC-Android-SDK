package io.antmedia.webrtc_android_sample_app.advanced;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import io.antmedia.webrtc_android_sample_app.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.microedition.khronos.opengles.GL10;

import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.canvas.CanvasListener;
import io.antmedia.webrtcandroidframework.canvas.ImageProxyRenderer;
import io.antmedia.webrtcandroidframework.canvas.Overlay;
import io.antmedia.webrtcandroidframework.core.WebRTCClient;

public class CustomCanvasActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private int defaultLensFacing = CameraSelector.LENS_FACING_BACK;
    private int lensFacing = defaultLensFacing;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private GLSurfaceView surfaceView;
    private ImageProxyRenderer imageProxyRenderer;

    WebRTCClient webRTCClient;
    private View broadcastingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    void setup() {
        setupCamera();

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

        imageProxyRenderer = new ImageProxyRenderer(webRTCClient,this, new CanvasListener(){

            boolean overlayInitialize = false;
            @Override
            public void onSurfaceIntialized(GL10 gl) {
                if(overlayInitialize){
                    for (Overlay overlay:Overlay.overlayArray){
                        overlay.updateRendererSize(imageProxyRenderer.frameWidth,imageProxyRenderer.frameHeight);
                    }
                }else{
                    overlayInitialize = true;
                    Overlay logo = new Overlay(getApplicationContext(), R.drawable.test,0,0);
                    logo.setSize(0.5f);
                    logo.updateRendererSize(imageProxyRenderer.frameWidth,imageProxyRenderer.frameHeight);
                    Overlay text = new Overlay(getApplicationContext(), "Hello", 64, Color.RED, 0f, 0f);
                    text.setSize(0.2f);
                }
            }
        });
        surfaceView.setRenderer(imageProxyRenderer);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);


        FrameLayout local = findViewById(R.id.localPreview);
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
                switchCamera();
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



    /*
        get interface orientation from
        https://stackoverflow.com/questions/10380989/how-do-i-get-the-current-orientation-activityinfo-screen-orientation-of-an-a/10383164
     */
    private int getScreenOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90
                        || rotation == Surface.ROTATION_270) && width > height) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        // if the device's natural orientation is landscape or if the device
        // is square:
        else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e(TAG, "Unknown screen orientation. Defaulting to " +
                            "landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }
    ProcessCameraProvider cameraProvider;
    private void setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                     cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }
    ImageAnalysis imageAnalysis;
    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        CameraResolutionPreset cameraPreset = CameraResolutionPreset.P640x480;
        int width;
        int height;
        int orientation = getScreenOrientation();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation ==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            width = cameraPreset.getWidth();
            height =  cameraPreset.getHeight();
        } else {
            width = cameraPreset.getHeight();
            height = cameraPreset.getWidth();
        }

         imageAnalysis = new ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setTargetResolution(new Size(width,height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                boolean isFront = lensFacing == CameraSelector.LENS_FACING_FRONT;
                int degrees = image.getImageInfo().getRotationDegrees();
                int applyDegrees = isFront ? degrees : -degrees;
                imageProxyRenderer.submitImage(
                        image,
                        180
                );
                image.close();
            }
        });

        startCamera(cameraProvider,imageAnalysis);

    }
    private void switchCamera() {
        // Toggle between front and back
        synchronized (this) {
            if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                lensFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }

            // Rebind with new selector
            startCamera(cameraProvider,imageAnalysis);
        }
    }
    private void startCamera(ProcessCameraProvider provider , ImageAnalysis imageAnalysis) {
        cameraProvider = provider;

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(
                (LifecycleOwner) this,
                cameraSelector,
                imageAnalysis
        );
    }
    private void restartCamera() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing) // FRONT or BACK
                .build();

        cameraProvider.bindToLifecycle(
                (LifecycleOwner) this,
                cameraSelector,
                imageAnalysis
        );
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        restartCamera();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceView != null) {
            surfaceView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (surfaceView != null) {
            surfaceView.onPause();
        }
    }

    @Override
    protected void onStop() {
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            cameraProvider.unbindAll();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



}

enum CameraResolutionPreset {
    /**
     * The 640×480 resolution preset.
     */
    P640x480(640, 480),
    /**
     * The 640×360 resolution preset.
     */
    P640x360(640, 360),
    /**
     * The 1280×720 resolution preset.
     */
    P1280x720(1280, 720),
    /**
     * The 1920×1080 resolution preset.
     */
    P1920x1080(1920, 1080);

    private int width;
    private int height;

    /**
     * Camera resolution preset constructor.
     * @param width The resolution preset width.
     * @param height The resolution preset height.
     */
    CameraResolutionPreset(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Gets resolution preset width.
     * @return Resolution preset width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets resolution preset height.
     * @return Resolution preset height.
     */
    public int getHeight() { return height; }
}
