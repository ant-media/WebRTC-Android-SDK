package io.antmedia.webrtcandroidframework.scrcpy;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import com.genymobile.scrcpy.ConfigurationException;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.ScreenInfo;
import com.genymobile.scrcpy.wrappers.SurfaceControl;

import org.webrtc.CapturerObserver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.AbstractList;
import java.util.concurrent.atomic.AtomicBoolean;

public class DisplayCapturerAndroid implements VideoCapturer, Device.RotationListener, Device.FoldListener, VideoSink {


    private static final String TAG = DisplayCapturerAndroid.class.getSimpleName() + "-WebRTC";
    private CapturerObserver capturerObserver;
    private SurfaceTextureHelper surfaceTextureHelper;
    private Device device;
    private int width;
    private int height;
    private IBinder display = null;
    private AtomicBoolean resetCapture = new AtomicBoolean(false);
    private long lastReceivedFrameTimestamp = 0;
    private AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {

        this.capturerObserver = capturerObserver;
        this.surfaceTextureHelper = surfaceTextureHelper;

        Options options = Options.parse();
        try {
            this.device = new Device(options);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }


    }

    @Override
    public synchronized void startCapture(int width, int height, int framerate) {

        running.set(true);
        if (display != null) {
            SurfaceControl.destroyDisplay(display);
        }
        display = createDisplay();
        device.setRotationListener(this);
        device.setFoldListener(this);


        ScreenInfo screenInfo = device.getScreenInfo();
        Rect contentRect = screenInfo.getContentRect();


        Rect videoRect = screenInfo.getVideoSize().toRect();

        Rect unlockedVideoRect = screenInfo.getUnlockedVideoSize().toRect();
        int videoRotation = screenInfo.getVideoRotation();
        int layerStack = device.getLayerStack();

        this.width = screenInfo.getVideoSize().getWidth();
        this.height = screenInfo.getVideoSize().getHeight();
        Log.i(TAG, "Start capture width: " + width + " height: " + height + " framerate: " + framerate + " video rotation: " + videoRotation);

        surfaceTextureHelper.setTextureSize(this.width, this.height);
        Surface surface = new Surface(surfaceTextureHelper.getSurfaceTexture());
        setDisplaySurface(display, surface, videoRotation, contentRect, unlockedVideoRect, layerStack);

        forceFrameIfRequires();
        capturerObserver.onCapturerStarted(true);
        surfaceTextureHelper.startListening(this);
    }

    public static IBinder createDisplay() {
        // Since Android 12 (preview), secure displays could not be created with shell permissions anymore.
        // On Android 12 preview, SDK_INT is still R (not S), but CODENAME is "S".
        boolean secure = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && !"S"
                .equals(Build.VERSION.CODENAME));
        return SurfaceControl.createDisplay("scrcpy", secure);
    }

    @Override
    public synchronized void stopCapture() throws InterruptedException {
        running.set(false);
        Log.i(TAG, "StopCapture");
        device.setRotationListener(null);
        device.setFoldListener(null);
        surfaceTextureHelper.stopListening();
        capturerObserver.onCapturerStopped();

        SurfaceControl.destroyDisplay(display);
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isScreencast() {
        return true;
    }

    @Override
    public void onRotationChanged(int rotation) {
        resetCapture.set(true);
    }

    @Override
    public void onFoldChanged(int displayId, boolean folded) {
        resetCapture.set(true);
    }

    public void forceFrameIfRequires() {
        long now = System.currentTimeMillis();
        long diff = now - lastReceivedFrameTimestamp;
        if (diff >= 50) {
            surfaceTextureHelper.forceFrame();
        }
        if (running.get())
        {
            surfaceTextureHelper.getHandler().postDelayed(() -> {
                forceFrameIfRequires();
            }, 100);
        }
    }

    @Override
    public synchronized void onFrame(VideoFrame frame) {
        Log.i(TAG, "onFrame received ----> " + frame.getTimestampNs());

        lastReceivedFrameTimestamp = System.currentTimeMillis();
        capturerObserver.onFrameCaptured(frame);



        /*
        if (resetCapture.getAndSet(false)) {

            Log.i(TAG, "Rotation has changed");
            device.setRotationListener(null);
            device.setFoldListener(null);
            surfaceTextureHelper.stopListening();
            SurfaceControl.destroyDisplay(display);

            startCapture(this.width, this.height, 30);


        }
        */
    }

    private static void setDisplaySurface(IBinder display, Surface surface, int orientation, Rect deviceRect, Rect displayRect, int layerStack) {
        SurfaceControl.openTransaction();
        try {
            SurfaceControl.setDisplaySurface(display, surface);
            SurfaceControl.setDisplayProjection(display, orientation, deviceRect, displayRect);
            SurfaceControl.setDisplayLayerStack(display, layerStack);
        } finally {
            SurfaceControl.closeTransaction();
        }
    }
}
