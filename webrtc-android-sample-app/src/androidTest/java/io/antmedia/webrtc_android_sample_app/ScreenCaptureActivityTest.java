package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
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
    private static final long UI_WAIT_TIMEOUT_MS = 10000L;
    private static final long STATUS_WAIT_TIMEOUT_MS = 10000L;
    private static final long STATUS_POLL_INTERVAL_MS = 500L;
    private static final long STATS_RETRY_DELAY_MS = 1000L;
    private static final int STATS_RETRY_COUNT = 10;

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

        clickScreenSharePermissionButton(device);

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);
        onView(withId(R.id.stats_popup_bytes_sent_video_textview)).inRoot(isDialog()).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            videoBytesSent = value;
        });

        onView(withId(R.id.stats_popup_close_button)).inRoot(isDialog()).perform(click());

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.rbFront);

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);

        assertVideoBytesSentChanged();

        onView(withId(R.id.stats_popup_close_button)).inRoot(isDialog()).perform(click());

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.rbRear);

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        performActivityClick(scenario, R.id.show_stats_button);

        Thread.sleep(3000);

        //after source switch video sending should continue.
        assertVideoBytesSentChanged();

        onView(withId(R.id.stats_popup_close_button)).inRoot(isDialog()).perform(click());

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(5000);

        assertStatusEventually(scenario, R.string.disconnected);

    }

    private void performActivityClick(ActivityScenario<ScreenCaptureActivity> scenario, int viewId) {
        scenario.onActivity(activity -> {
            View view = activity.findViewById(viewId);
            assertNotNull(view);
            view.performClick();
        });
    }

    private void clickScreenSharePermissionButton(UiDevice device) {
        UiObject2 permissionButton = device.wait(Until.findObject(By.res("android:id/button1")), UI_WAIT_TIMEOUT_MS);

        if (permissionButton == null) {
            permissionButton = device.wait(Until.findObject(By.textContains("Start")), UI_WAIT_TIMEOUT_MS);
        }

        if (permissionButton == null) {
            permissionButton = device.wait(Until.findObject(By.textContains("Allow")), UI_WAIT_TIMEOUT_MS);
        }

        assertNotNull(permissionButton);
        permissionButton.click();
    }

    private void assertVideoBytesSentChanged() throws InterruptedException {
        float previousValue = videoBytesSent;
        float lastObservedValue = previousValue;

        for (int i = 0; i < STATS_RETRY_COUNT; i++) {
            final float[] currentValue = {-1f};

            onView(withId(R.id.stats_popup_bytes_sent_video_textview)).inRoot(isDialog()).check((view, noViewFoundException) -> {
                if (noViewFoundException != null) {
                    throw noViewFoundException;
                }
                currentValue[0] = Float.parseFloat(((TextView) view).getText().toString());
            });
            lastObservedValue = currentValue[0];

            if (currentValue[0] > 0f && currentValue[0] != previousValue) {
                videoBytesSent = currentValue[0];
                return;
            }

            Thread.sleep(STATS_RETRY_DELAY_MS);
        }

        assertTrue("Video bytes sent did not progress. Previous: " + previousValue + ", current: " + lastObservedValue,
                lastObservedValue > 0f && lastObservedValue != previousValue);
    }

    private void assertStatusEventually(ActivityScenario<ScreenCaptureActivity> scenario, int expectedStringRes) {
        String expectedText = ApplicationProvider.getApplicationContext().getString(expectedStringRes);
        AssertionError lastError = null;

        long deadline = System.currentTimeMillis() + STATUS_WAIT_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            final String[] statusText = {null};
            scenario.onActivity(activity -> {
                TextView statusView = activity.findViewById(R.id.broadcasting_text_view);
                statusText[0] = statusView.getText().toString();
            });

            if (expectedText.equals(statusText[0])) {
                return;
            }

            lastError = new AssertionError("Expected status " + expectedText + " but was " + statusText[0]);
            try {
                Thread.sleep(STATUS_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        throw lastError != null ? lastError : new AssertionError("Expected status " + expectedText);
    }
}
