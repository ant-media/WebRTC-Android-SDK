package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.PlayActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PlayActivityTest {
    private static final long STATUS_WAIT_TIMEOUT_MS = 10000L;
    private static final long STATUS_POLL_INTERVAL_MS = 500L;

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    @Before
    public void before() throws IOException {
        connectInternet();
    }

    @After
    public void after() {
        unregisterIdlingResource();
    }

    @Rule
    public TestLogger testLogger = new TestLogger();

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }

    @Test
    public void testPlaying() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayActivity.class);
        ActivityScenario<PlayActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        //stream556677i4d is the stream id in github actions
        onView(withId(R.id.stream_id_edittext)).perform(replaceText("stream556677i4d"));
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());


        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop")));

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        Thread.sleep(3000);

        //Stop playing
        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(3000);

        assertStatusEventually(scenario, R.string.disconnected);
    }

    @Test
    public void testPlayReconnection() throws InterruptedException, IOException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PlayActivity.class);
        ActivityScenario<PlayActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        //stream556677i4d is the stream id in github actions
        onView(withId(R.id.stream_id_edittext)).perform(replaceText("stream556677i4d"));

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        disconnectInternet();

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.disconnected), withText(R.string.reconnecting))));

        connectInternet();

        Thread.sleep(30000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(3000);

        assertStatusEventually(scenario, R.string.disconnected);


        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        disconnectInternet();

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.disconnected), withText(R.string.reconnecting))));

        connectInternet();

        Thread.sleep(30000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        Thread.sleep(3000);

        performActivityClick(scenario, R.id.start_streaming_button);

        Thread.sleep(3000);

        assertStatusEventually(scenario, R.string.disconnected);
    }

    private void performActivityClick(ActivityScenario<PlayActivity> scenario, int viewId) {
        scenario.onActivity(activity -> {
            View view = activity.findViewById(viewId);
            view.performClick();
        });
    }

    private void assertStatusEventually(ActivityScenario<PlayActivity> scenario, int expectedStringRes) {
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

    private void unregisterIdlingResource() {
        if (mIdlingResource == null) {
            return;
        }
        try {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        } catch (IllegalArgumentException ignored) {
            // Resource may already be unregistered.
        } finally {
            mIdlingResource = null;
        }
    }

    private void disconnectInternet() throws IOException {
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi disable"); // Switch off Wifi
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data disable"); // Switch off Mobile Data
    }

    private void connectInternet() throws IOException {
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi enable"); // Switch Wifi on again
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data enable"); // Switch Mobile Data on again
    }
}
