package io.antmedia.webrtcandroidframework;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.List;

import javax.annotation.Nullable;

class USBCameraCapturer implements CameraVideoCapturer {
    private static final String TAG = USBCameraCapturer.class.getSimpleName();
    private UVCCamera mUVCCamera;
    private final String deviceName;
    private Context applicationContext;
    private org.webrtc.CapturerObserver capturerObserver;
    private SurfaceTextureHelper surfaceHelper;
    private Handler cameraThreadHandler;

    private USBMonitor mUSBMonitor;

    private final Object mSync = new Object();

    private USBMonitor.OnDeviceConnectListener onDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice usbDevice) {
            Log.i(TAG, "--- onAttach USB Device ---");
            mUSBMonitor.requestPermission(usbDevice);
        }

        @Override
        public void onDettach(UsbDevice usbDevice) {
            Log.i(TAG, "--- onDettach USB Device ---");
        }

        @Override
        public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
            Log.i(TAG, "--- onConnect USB Device ---");

            mUVCCamera = new UVCCamera();
            mUVCCamera.open(usbControlBlock);
            chooseSizeAndStartPreview(width, height);
            capturerObserver.onCapturerStarted(true);
        }

        @Override
        public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
            Log.i(TAG, "--- onDisconnect USB Device ---");
            releaseCamera();
        }

        @Override
        public void onCancel(UsbDevice usbDevice) {
            Log.i(TAG, "--- onCancel USB Device ---");
        }
    };

    private void chooseSizeAndStartPreview(int width, int height) {
        List<Size> supportedSizeList = mUVCCamera.getSupportedSizeList();
        Size selectedSize = supportedSizeList.get(0);
        int selectedSizeDiff = Math.abs(selectedSize.width - width) + Math.abs(selectedSize.height-height);
        int tmpSizeDiff;
        for (Size size :
                supportedSizeList) {
            tmpSizeDiff = Math.abs(size.width - width) + Math.abs(size.height-height);
            if ( tmpSizeDiff < selectedSizeDiff) {
                selectedSize = size;
                selectedSizeDiff = tmpSizeDiff;
            }
        }
        surfaceHelper.setTextureSize(selectedSize.width, selectedSize.height);
        mUVCCamera.setPreviewDisplay(new Surface(surfaceHelper.getSurfaceTexture()));
        surfaceHelper.stopListening();
        surfaceHelper.startListening(videoFrame -> capturerObserver.onFrameCaptured(videoFrame));
        mUVCCamera.setPreviewSize(selectedSize.width, selectedSize.height, UVCCamera.FRAME_FORMAT_YUYV);


        mUVCCamera.startPreview();

    }

    private int width;
    private int height;
    private int framerate;

    public USBCameraCapturer(String deviceName, CameraEventsHandler cameraEventsHandler, boolean captureToTexture) {
        this.deviceName = deviceName;
    }

    @Override
    public void switchCamera(CameraSwitchHandler cameraSwitchHandler) {
        Log.e(TAG, "Switch Camera is not supported in USBCameraCapturer");
        throw new UnsupportedOperationException("Switch Camera is not supported in USBCameraCapturer");
    }

    @Override
    public void initialize(@Nullable SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, org.webrtc.CapturerObserver capturerObserver) {
        this.applicationContext = applicationContext;
        this.capturerObserver = capturerObserver;
        this.surfaceHelper = surfaceTextureHelper;
        this.cameraThreadHandler = surfaceTextureHelper == null ? null : surfaceTextureHelper.getHandler();

        this.mUSBMonitor = new USBMonitor(applicationContext, onDeviceConnectListener);
    }



    @Override
    public void startCapture(int width, int height, int framerate) {
        if (!mUSBMonitor.isRegistered()) {
            mUSBMonitor.register();
        }
        this.width = width;
        this.height = height;
        this.framerate = framerate;

    }

    @Override
    public void stopCapture() throws InterruptedException {
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
        }
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
            this.width = width;
            this.height = height;
            chooseSizeAndStartPreview(width, height);
        }
    }

    @Override
    public void dispose() {
        releaseCamera();
        if (this.mUSBMonitor != null) {
            this.mUSBMonitor.unregister();
            this.mUSBMonitor.destroy();
            this.mUSBMonitor = null;
        }
    }

    private void releaseCamera() {
        synchronized (mSync) {
            capturerObserver.onCapturerStopped();
            Log.i(TAG, "Releasing USB Camera");
            try {
                if (mUVCCamera != null) {
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                    mUVCCamera = null;
                }
            } catch (Exception e) {
                //
            }
        }
    }

    @Override
    public boolean isScreencast() {
        return false;
    }
}
