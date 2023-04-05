package io.antmedia.webrtcandroidframework;

import static org.mockito.Mockito.spy;
import static org.webrtc.ScreenCapturerAndroid.VIRTUAL_DISPLAY_DPI;

import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.view.Display;
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
    public void testOnFrameRotation() {

        ScreenCapturerAndroid screenCapturerAndroid = spy(new ScreenCapturerAndroid(null, null));
        WindowManager windowManager = Mockito.spy(WindowManager.class);
        screenCapturerAndroid.setWindowManager(windowManager);

        Display display = Mockito.mock(Display.class);
        Mockito.when(windowManager.getDefaultDisplay()).thenReturn(display);

        VirtualDisplay virtualDisplay = Mockito.mock(VirtualDisplay.class);
        SurfaceTextureHelper surfaceTextureHelper = Mockito.mock(SurfaceTextureHelper.class);
        screenCapturerAndroid.setVirtualDisplay(virtualDisplay);
        screenCapturerAndroid.setSurfaceTextureHelper(surfaceTextureHelper);

        screenCapturerAndroid.setCapturerObserver(Mockito.mock(CapturerObserver.class));

        int width = 540;
        int height = 960;
        screenCapturerAndroid.setWidth(width);
        screenCapturerAndroid.setHeight(height);

        VideoFrame frame = new VideoFrame(Mockito.mock(VideoFrame.Buffer.class), 0, 0);
        screenCapturerAndroid.onFrame(frame);

        Mockito.when(display.getRotation()).thenReturn(1);
        screenCapturerAndroid.onFrame(frame);

        Mockito.verify(virtualDisplay).resize(height, width, VIRTUAL_DISPLAY_DPI);
        Mockito.verify(surfaceTextureHelper).setTextureSize(height, width);

        Mockito.when(display.getRotation()).thenReturn(2);
        screenCapturerAndroid.onFrame(frame);

        Mockito.verify(virtualDisplay).resize(width, height, VIRTUAL_DISPLAY_DPI);
        Mockito.verify(surfaceTextureHelper).setTextureSize(width,height);

    }
}
