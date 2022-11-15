package io.antmedia.webrtcandroidframework;

import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import junit.framework.TestCase;

import org.junit.Test;
import org.mockito.Mockito;
import org.webrtc.ScreenCapturerAndroid;

public class ScreenCapturerAndroidTest extends TestCase {

    @Test
    public void testIsDeviceOrientationPortrait() {
        ScreenCapturerAndroid screenCapturerAndroid = new ScreenCapturerAndroid(null,null,null);//Mockito.mock(ScreenCapturerAndroid.class);
        WindowManager windowManager = Mockito.mock(WindowManager.class);
        Display display = Mockito.mock(Display.class);

        screenCapturerAndroid.setWindowManager(windowManager);
        Mockito.when(windowManager.getDefaultDisplay()).thenReturn(display);
        Mockito.when(display.getRotation()).thenReturn(Surface.ROTATION_0);
        
        assertTrue(screenCapturerAndroid.isDeviceOrientationPortrait());
    }
}