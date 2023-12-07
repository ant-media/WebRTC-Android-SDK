package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.hsj.camera.Size;
import com.hsj.camera.USBMonitor;
import com.hsj.camera.UVCCamera;

import org.apache.commons.lang3.RandomStringUtils;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;
import java.util.Timer;

import io.antmedia.webrtcandroidframework.CustomVideoCapturer;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public final class USBCameraActivity extends AbstractSampleSDKActivity  implements Handler.Callback {

    private static final String TAG = "USBCameraActivity";
    //TODO Set your usb camera display width and height
    private static int PREVIEW_WIDTH = 640;
    private static int PREVIEW_HEIGHT = 480;

    private static final int CAMERA_CREATE = 1;
    private static final int CAMERA_PREVIEW = 2;
    private static final int CAMERA_START = 3;
    private static final int CAMERA_STOP = 4;
    private static final int CAMERA_DESTROY = 5;

    private Context context;
    private USBMonitor mUSBMonitor;
    private Handler cameraHandler;
    private HandlerThread cameraThread;

    private boolean enableDataChannel = true;

    private WebRTCClient webRTCClient;

    private Button startStreamingButton;
    private String operationName = "";

    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;

    private TextView broadcastingView;
    private EditText streamIdEditText;
    private Surface surface;

    private RadioGroup bg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam);

        this.context = getApplicationContext();
        this.cameraThread = new HandlerThread("thread_uvc_camera");
        this.cameraThread.start();
        this.cameraHandler = new Handler(cameraThread.getLooper(), this);

        if (hasPermissions(Manifest.permission.CAMERA)) {
            createUsbMonitor();
        }

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

        bg = findViewById(R.id.rbGroup);
        bg.check(R.id.rbFront);
        bg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String newSource = "";
                if(checkedId == R.id.rbFront) {
                    newSource = WebRTCClient.SOURCE_FRONT;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
                    webRTCClient.setCustomCapturerEnabled(false);
                    cameraHandler.obtainMessage(CAMERA_STOP).sendToTarget();
                    webRTCClient.changeVideoSource(newSource);

                }
                else if(checkedId == R.id.rbRear) {
                    newSource = WebRTCClient.SOURCE_REAR;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
                    webRTCClient.setCustomCapturerEnabled(false);
                    cameraHandler.obtainMessage(CAMERA_STOP).sendToTarget();
                    webRTCClient.changeVideoSource(newSource);

                }
                else if(checkedId == R.id.rbUsb) {
                    newSource = WebRTCClient.SOURCE_CUSTOM;
                    getIntent().putExtra(CallActivity.EXTRA_SCREENCAPTURE, false);
                    webRTCClient.setCustomCapturerEnabled(true);
                    webRTCClient.changeVideoSource(newSource);
                    startUSBCamera();
                }

            }
        });

        webRTCClient = new WebRTCClient( this,this);

        String tokenId = "tokenId";
        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        webRTCClient.init(serverUrl, streamIdEditText.getText().toString(), IWebRTCClient.MODE_PUBLISH, tokenId, this.getIntent());
        webRTCClient.setDataChannelObserver(this);
    }

    private void startUSBCamera() {
        SurfaceTexture surfaceTexture = ((CustomVideoCapturer) webRTCClient.getVideoCapturer()).getSurfaceTextureHelper().getSurfaceTexture();
        surface = new Surface(surfaceTexture);
        cameraHandler.obtainMessage(CAMERA_PREVIEW, surface).sendToTarget();

        cameraHandler.obtainMessage(CAMERA_START).sendToTarget();
    }

    public void startStreaming(View v) {
        //update stream id if it is changed
        webRTCClient.setStreamId(streamIdEditText.getText().toString());
        incrementIdle();
        if (!webRTCClient.isStreaming()) {
            ((Button) v).setText("Stop " + operationName);
            Log.i(getClass().getSimpleName(), "Calling startStream");

            webRTCClient.startStream();
        }
        else {
            ((Button)startStreamingButton).setText("Start " + operationName);
            Log.i(getClass().getSimpleName(), "Calling stopStream");
            webRTCClient.stopStream();
            cameraHandler.obtainMessage(CAMERA_STOP).sendToTarget();
        }

    }

    @Override
    protected void onDestroy() {
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        if (cameraThread != null) {
            try {
                cameraThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cameraThread = null;
        }
        super.onDestroy();
        Log.d(TAG, "activity destroy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasAllPermissions = true;
        for (int granted : grantResults) {
            hasAllPermissions &= (granted == PackageManager.PERMISSION_GRANTED);
        }
        if (hasAllPermissions) {
            createUsbMonitor();
        }
    }

    private void createUsbMonitor() {
        this.mUSBMonitor = new USBMonitor(context, dcl);
        this.mUSBMonitor.register();
    }

    private void showToast(@NonNull String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean hasPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                ActivityCompat.requestPermissions(this, permissions, 0);
            }
        }
        return allGranted;
    }

    private final USBMonitor.OnDeviceConnectListener dcl = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice device) {
            Log.d(TAG, "Usb->onAttach->" + device.getProductId());
        }

        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
            Log.d(TAG, "Usb->onConnect->" + device.getProductId());
            cameraHandler.obtainMessage(CAMERA_CREATE, ctrlBlock).sendToTarget();
        }

        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
            Log.d(TAG, "Usb->onDisconnect->" + device.getProductId());
        }

        @Override
        public void onCancel(UsbDevice device) {
            Log.d(TAG, "Usb->onCancel->" + device.getProductId());
        }

        @Override
        public void onDetach(UsbDevice device) {
            Log.d(TAG, "Usb->onDetach->" + device.getProductId());
        }
    };

//=====================================UVCCamera Action=============================================

    private boolean isStart;
    private UVCCamera camera;

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case CAMERA_CREATE:
                initCamera((USBMonitor.UsbControlBlock) msg.obj);
                break;
            case CAMERA_PREVIEW:
                setSurface((Surface) msg.obj);
                break;
            case CAMERA_START:
                startCamera();
                break;
            case CAMERA_STOP:
                stopCamera();
                break;
            case CAMERA_DESTROY:
                destroyCamera();
                break;
            default:
                break;
        }
        return true;
    }

    private void initCamera(@NonNull USBMonitor.UsbControlBlock block) {
        long t = System.currentTimeMillis();
        if (camera != null) {
            destroyCamera();
            camera = null;
        }
        Log.d(TAG, "camera create start");
        try {
            camera = new UVCCamera();
            camera.open(block);
            camera.setPreviewRotate(UVCCamera.PREVIEW_ROTATE.ROTATE_90);
            camera.setPreviewFlip(UVCCamera.PREVIEW_FLIP.FLIP_H);
            checkSupportSize(camera);
            camera.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    UVCCamera.FRAME_FORMAT_YUYV, 1.0f);
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            e.printStackTrace();
            camera.destroy();
            camera = null;
            return;
        }
        Log.d(TAG, "camera create time=" + (System.currentTimeMillis() - t));
        if (surface != null) {
            startCamera();
        }
    }

    private void checkSupportSize(UVCCamera mCamera) {
        List<Size> sizes = mCamera.getSupportedSizeList();
        //Most UsbCamera support 640x480
        //A few UsbCamera may fail to obtain the supported resolution
        if (sizes == null || sizes.size() == 0) return;
        Log.d(TAG, mCamera.getSupportedSize());
        boolean isSupport = false;
        for (Size size : sizes) {
            if (size.width == PREVIEW_WIDTH && size.height == PREVIEW_HEIGHT) {
                isSupport = true;
                break;
            }
        }
        if (!isSupport) {
            //Use intermediate support size
            Size size = sizes.get(sizes.size() / 2);
            PREVIEW_WIDTH = size.width;
            PREVIEW_HEIGHT = size.height;
        }
        Log.d(TAG, String.format("SupportSize->with=%d,height=%d", PREVIEW_WIDTH, PREVIEW_HEIGHT));
    }

    private void setSurface(Surface surface) {
        this.surface = surface;
        if (isStart) {
            stopCamera();
            startCamera();
        } else if (camera != null) {
            startCamera();
        }
    }

    private void startCamera() {
        long start = System.currentTimeMillis();
        if (!isStart && camera != null) {
            isStart = true;
            if (surface != null) {
                //Call this method when you need show preview
                Log.d(TAG, "setPreviewDisplay()");
                camera.setPreviewDisplay(surface);
            }
            //TODO Camera frame callback
            camera.setFrameCallback(frame -> {
                Log.d(TAG,"frameSize="+frame.capacity());
                //saveFile("/sdcard/640x400.NV21",frame);
            }, UVCCamera.PIXEL_FORMAT_NV21);
            camera.startPreview();
        }
        Log.d(TAG, "camera start time=" + (System.currentTimeMillis() - start));
    }

    private void stopCamera() {
        long start = System.currentTimeMillis();
        if (isStart && camera != null) {
            isStart = false;
            camera.stopPreview();
        }
        Log.d(TAG, "camera stop time=" + (System.currentTimeMillis() - start));
    }

    private void destroyCamera() {
        long start = System.currentTimeMillis();
        stopCamera();
        if (camera != null) {
            camera.destroy();
            camera = null;
        }
        Log.d(TAG, "camera destroy time=" + (System.currentTimeMillis() - start));
    }
}


