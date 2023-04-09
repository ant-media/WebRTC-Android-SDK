package io.antmedia.webrtcandroidframework;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UIRequiredTest {

    @Rule
    public ActivityScenarioRule<TestActivity> activityScenarioRule
            = new ActivityScenarioRule<>(TestActivity.class);


    @Test
    public void test() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Intent intent = Mockito.mock(Intent.class);
        activityScenarioRule.getScenario().onActivity(activity -> {
            // use 'activity'.
            TestActivity myActivity = (TestActivity) activity;
            SurfaceViewRenderer renderer = myActivity.getTestRenderer();

            WebRTCClient webRTCClient = new WebRTCClient(null, myActivity);
            webRTCClient.setVideoRenderers(null, renderer);
            webRTCClient.init("http://my.ams:5080/myapp/websocket","stream", WebRTCClient.MODE_PUBLISH, "token", intent);

            webRTCClient.changeVideoSource(WebRTCClient.SOURCE_SCREEN);

            UiDevice device = UiDevice.getInstance(getInstrumentation());
            UiObject2 button = device.wait(Until.findObject(By.text("Start now")), 10000);
            assertNotNull(button);
            button.click();


        });
    }

    /**
     * This test should be in another method but cannot get the full logcat so it's moved here
     */
    @Test
    public void testVideoCapturer() {
        IWebRTCListener listener = Mockito.mock(IWebRTCListener.class);
        Intent intent = Mockito.mock(Intent.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {


            Context context = InstrumentationRegistry.getInstrumentation().getContext();

            WebRTCClient webRTCClient = new WebRTCClient(null, context);
            VideoCapturer capturer = webRTCClient.createVideoCapturer(WebRTCClient.SOURCE_FRONT);

            //webRTCClient.init("http://my.ams:5080/myapp/websocket","stream", WebRTCClient.MODE_PUBLISH, "token", intent);
            webRTCClient.initializeRenderers();
        });
    }

}
