package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.webrtc.ScreenCapturerAndroid.VIRTUAL_DISPLAY_DPI;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import org.junit.Test;
import org.mockito.Mockito;
import org.webrtc.CapturerObserver;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

public class ScreenCapturerAndroidTest {

    @Test
    public void testRotateScreen() {

        ScreenCapturerAndroid screenCapturerAndroid = spy(new ScreenCapturerAndroid(null, null));
        WindowManager windowManager = Mockito.spy(WindowManager.class);
        screenCapturerAndroid.setWindowManager(windowManager);

        Display display = mock(Display.class);
        Mockito.when(windowManager.getDefaultDisplay()).thenReturn(display);

        VirtualDisplay virtualDisplay = mock(VirtualDisplay.class);
        SurfaceTextureHelper surfaceTextureHelper = mock(SurfaceTextureHelper.class);
        screenCapturerAndroid.setVirtualDisplay(virtualDisplay);
        screenCapturerAndroid.setSurfaceTextureHelper(surfaceTextureHelper);

        CapturerObserver capturerObserver = mock(CapturerObserver.class);
        screenCapturerAndroid.setCapturerObserver(capturerObserver);

        int width = 540;
        int height = 960;
        screenCapturerAndroid.setWidth(width);
        screenCapturerAndroid.setHeight(height);

        screenCapturerAndroid.deviceRotation = 0;

        screenCapturerAndroid.rotateScreen(90);

        Mockito.when(display.getRotation()).thenReturn(1);
        screenCapturerAndroid.rotateScreen(0);

        Mockito.verify(virtualDisplay).resize(height, width, VIRTUAL_DISPLAY_DPI);
        Mockito.verify(surfaceTextureHelper).setTextureSize(height, width);

        Mockito.when(display.getRotation()).thenReturn(2);
        screenCapturerAndroid.rotateScreen(90);

        Mockito.verify(virtualDisplay).resize(width, height, VIRTUAL_DISPLAY_DPI);
        Mockito.verify(surfaceTextureHelper).setTextureSize(width,height);

        VideoFrame frame = mock(VideoFrame.class);
        screenCapturerAndroid.onFrame(frame);

        Mockito.verify(capturerObserver).onFrameCaptured(frame);
    }

    @Test
    public void testDispose() {
        ScreenCapturerAndroid screenCapturerAndroid = spy(new ScreenCapturerAndroid(null, null));
        assertFalse(screenCapturerAndroid.isDisposed());
        screenCapturerAndroid.dispose();
        assertTrue(screenCapturerAndroid.isDisposed());
    }

    @Test
    public void testCapturer() {
        ScreenCapturerAndroid screenCapturerAndroid = spy(new ScreenCapturerAndroid(null, null));
        SurfaceTextureHelper surfaceTextureHelper = mock(SurfaceTextureHelper.class);
        Context appContext = mock(Context.class);
        screenCapturerAndroid.initialize(surfaceTextureHelper, appContext, mock(CapturerObserver.class));

        verify(appContext).getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        verify(appContext).getSystemService(Context.WINDOW_SERVICE);

        MediaProjectionManager mediaProjectionManager = mock(MediaProjectionManager.class);
        screenCapturerAndroid.setMediaProjectionManager(mediaProjectionManager);
        MediaProjection mediaProjection = mock(MediaProjection.class);
        when(mediaProjectionManager.getMediaProjection(Mockito.anyInt(), Mockito.any())).thenReturn(mediaProjection);

        screenCapturerAndroid.startCapture(460, 360, 30);
        verify(mediaProjectionManager).getMediaProjection(
                Mockito.anyInt(), Mockito.any());

        verify(mediaProjection).createVirtualDisplay(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.any(), Mockito.any(), Mockito.any());



    }
}
