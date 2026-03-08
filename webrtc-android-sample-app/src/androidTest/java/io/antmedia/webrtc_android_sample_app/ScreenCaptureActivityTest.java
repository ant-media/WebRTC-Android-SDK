package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.antmedia.webrtc_android_sample_app.basic.ScreenCaptureActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ScreenCaptureActivityTest {

    private final String SCREEN_SHARE_PERMISSION_DIALOG_START_NOW_TEXT ="Start now";

    private float videoBytesSent = 0;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    @Before
    public void before() {
        //try before method to make @Rule run properly
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }

    @Test
    public void testScreenCapture() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ScreenCaptureActivity.class);
        ActivityScenario<ScreenCaptureActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)));

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        performActivityClick(scenario, R.id.rbScreen);

        UiObject2 button = device.wait(
                Until.findObject(By.res("android:id/button1")),
                10000
        );
        button.click();

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);
        onView(withId(R.id.stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            videoBytesSent = value;
        });

        performActivityClick(scenario, R.id.stats_popup_close_button);

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.rbFront);

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);

        onView(withId(R.id.stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
            videoBytesSent = value;

        });

        performActivityClick(scenario, R.id.stats_popup_close_button);

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.rbRear);

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);

        //after source switch video sending should continue.
        onView(withId(R.id.stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue( value > 0);
            assertTrue( value != videoBytesSent);
        });

        performActivityClick(scenario, R.id.stats_popup_close_button);

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

    }

    private void performActivityClick(ActivityScenario<ScreenCaptureActivity> scenario, int viewId) {
        scenario.onActivity(activity -> {
            View view = activity.findViewById(viewId);
            assertNotNull(view);
            view.performClick();
        });
    }
}
