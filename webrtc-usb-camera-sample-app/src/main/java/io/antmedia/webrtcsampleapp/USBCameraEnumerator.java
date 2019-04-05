package io.antmedia.webrtcsampleapp;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;
import android.util.Log;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraVideoCapturer;

import java.util.ArrayList;
import java.util.List;

public class USBCameraEnumerator extends Camera1Enumerator {

    private static final String TAG = USBCameraEnumerator.class.getSimpleName();
    private final Activity context;
    private UVCCamera mUVCCamera;
    private final USBMonitor.OnDeviceConnectListener mFakeDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(UsbDevice usbDevice) {
           // Log.i(TAG, "--- onAttach USB Device ---");
        }

        @Override
        public void onDettach(UsbDevice usbDevice) {
           // Log.i(TAG, "--- onDettach USB Device ---");
        }

        @Override
        public void onConnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock, boolean b) {
           // Log.i(TAG, "--- onConnect USB Device ---");
        }

        @Override
        public void onDisconnect(UsbDevice usbDevice, USBMonitor.UsbControlBlock usbControlBlock) {
           // Log.i(TAG, "--- onDisconnect USB Device ---");
        }

        @Override
        public void onCancel(UsbDevice usbDevice) {
           // Log.i(TAG, "--- onCancel USB Device ---");

        }
    };
    private boolean captureToTexture = false;

    protected USBMonitor mUSBMonitor;

    USBCameraEnumerator(Activity activity, boolean captureToTexture) {
        super(captureToTexture);
        this.captureToTexture = captureToTexture;
        this.context = activity;
        this.mUSBMonitor = new USBMonitor(this.context, mFakeDeviceConnectListener);

    }


    @Override
    public String[] getDeviceNames() {
        ArrayList<String> namesList = new ArrayList();

        List<DeviceFilter> deviceFilterList = getDeviceFilters();

        List<UsbDevice> deviceList = this.mUSBMonitor.getDeviceList(deviceFilterList);
        int size = deviceList.size();
        Log.i(TAG, "USB Camera device list size: " + size);
        for(int i = 0; i < size; ++i) {
            namesList.add(deviceList.get(i).getDeviceName());

            Log.i(TAG, "Adding USB Camera "+ i +" to device list");
        }
        String[] namesArray = new String[namesList.size()];
        return namesList.toArray(namesArray);
    }

    @NonNull
    private List<DeviceFilter> getDeviceFilters() {
        return DeviceFilter.getDeviceFilters(this.context, com.jiangdg.libusbcamera.R.xml.device_filter);
    }


    @Override
    public boolean isFrontFacing(String s) {
        return false;
    }

    @Override
    public boolean isBackFacing(String s) {
        return false;
    }


    @Override
    public List<CameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String deviceName) {
         return null;
    }

    @Override
    public CameraVideoCapturer createCapturer(String deviceName, CameraVideoCapturer.CameraEventsHandler cameraEventsHandler) {
        return new USBCameraCapturer(deviceName, cameraEventsHandler, captureToTexture);
    }


}
