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
import org.webrtc.EglBase;
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
    private long lastReceivedFrameTimestamp = 0;
    private AtomicBoolean running = new AtomicBoolean(false);
    private Surface surface;
    private EglBase eglBase;
    private int currentRotation = 0;

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

    public void setEglBase(EglBase eglBase) {
        this.eglBase = eglBase;
    }

    @Override
    public synchronized void startCapture(int width, int height, int framerate) {

        capture();

    }

    public void capture() {

        display = createDisplay();

        ScreenInfo screenInfo = device.getScreenInfo();
        Rect contentRect = screenInfo.getContentRect();

        Rect videoRect = screenInfo.getVideoSize().toRect();

        Rect unlockedVideoRect = screenInfo.getUnlockedVideoSize().toRect();
        int videoRotation = screenInfo.getVideoRotation();
        int layerStack = device.getLayerStack();

        this.width = screenInfo.getVideoSize().getWidth();
        this.height = screenInfo.getVideoSize().getHeight();
        Log.i(TAG, "Start capture width: " + width + " height: " + height + " video rotation: " + videoRotation);

        surfaceTextureHelper = SurfaceTextureHelper.create("encoder-texture-thread", eglBase.getEglBaseContext());
        surfaceTextureHelper.setTextureSize(this.width, this.height);
        surfaceTextureHelper.setFrameRotation(this.currentRotation * 90);
        surface = new Surface(surfaceTextureHelper.getSurfaceTexture());

        setDisplaySurface(display, surface, videoRotation, contentRect, unlockedVideoRect, layerStack);

        running.set(true);
        forceFrameIfRequires();

        surfaceTextureHelper.startListening(this);
        capturerObserver.onCapturerStarted(true);
        device.setRotationListener(this);
        device.setFoldListener(this);

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
       release();
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
        Log.i(TAG, "Rotation has changed to " + rotation);
        this.currentRotation = rotation;
        release();
        capture();
    }

    public void release() {
        running.set(false);
        device.setRotationListener(null);
        device.setFoldListener(null);
        surfaceTextureHelper.getHandler().removeCallbacks(setForceFrameRunnable);
        if (display != null) {
            SurfaceControl.destroyDisplay(display);
            display = null;
        }
        surfaceTextureHelper.stopListening();
        if (surface != null) {
            surface.release();
            surface = null;
        }

        surfaceTextureHelper.dispose();

        capturerObserver.onCapturerStopped();
    }



    @Override
    public void onFoldChanged(int displayId, boolean folded) {
    }

    public void forceFrameIfRequires() {
        long now = System.currentTimeMillis();
        long diff = now - lastReceivedFrameTimestamp;
        if (diff >= 50) {
            Log.i(TAG, "force frame to produce");
            surfaceTextureHelper.forceFrame();
        }
        if (running.get())
        {
            surfaceTextureHelper.getHandler().postDelayed(setForceFrameRunnable
            , 33);
        }
    }

    public Runnable setForceFrameRunnable = new Runnable() {
        @Override
        public void run() {
            forceFrameIfRequires();
        }
    };

    @Override
    public synchronized void onFrame(VideoFrame frame) {
       // Log.i(TAG, "onFrame received ----> " + frame.getTimestampNs() + " frame width: " + frame.getRotatedWidth()
       // + " frame height: " + frame.getRotatedHeight() + " rotation: " + frame.getRotation());

        lastReceivedFrameTimestamp = System.currentTimeMillis();
        capturerObserver.onFrameCaptured(frame);

    }

    public static void setDisplaySurface(IBinder display, Surface surface, int orientation, Rect deviceRect, Rect displayRect, int layerStack) {
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
