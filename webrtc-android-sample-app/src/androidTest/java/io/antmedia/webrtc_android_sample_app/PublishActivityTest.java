package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PublishActivityTest {
    private static final long STOP_TIMEOUT_MS = 10000;
    private static final long STOP_SETTLE_DELAY_MS = 3000;
    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);


    @Before
    public void before() throws IOException {
        connectInternet();
    }

    @After
    public void unregisterIdlingResource() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
            mIdlingResource = null;
        }
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
    public void testPublishing() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PublishActivity.class);
        ActivityScenario<PublishActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());


        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop")));

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.connecting), withText(R.string.live))));


        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));
    }

    @Test
    public void testPublishReconnection() throws InterruptedException, IOException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PublishActivity.class);
        ActivityScenario<PublishActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        disconnectInternet();

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.disconnected), withText(R.string.reconnecting))));

        connectInternet();

        Thread.sleep(40000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.start_streaming_button)).perform(click());

        waitForBroadcastStatus(scenario, R.string.disconnected, STOP_TIMEOUT_MS);

        // onPublishFinished updates the status before peer teardown has necessarily completed.
        // Let teardown settle before publishing the same stream id again.
        Thread.sleep(STOP_SETTLE_DELAY_MS);

        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        disconnectInternet();

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.disconnected), withText(R.string.reconnecting))));

        connectInternet();

        Thread.sleep(40000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.start_streaming_button)).perform(click());

        waitForBroadcastStatus(scenario, R.string.disconnected, STOP_TIMEOUT_MS);

    }

    private void waitForBroadcastStatus(ActivityScenario<PublishActivity> scenario,
                                        int expectedStatusResId, long timeoutMs)
            throws InterruptedException {
        String expectedStatus = ApplicationProvider.getApplicationContext().getString(expectedStatusResId);
        long startTimeMs = SystemClock.elapsedRealtime();
        String[] actualStatus = {"<unavailable>"};

        while (SystemClock.elapsedRealtime() - startTimeMs < timeoutMs) {
            scenario.onActivity(activity -> {
                TextView statusView = activity.findViewById(R.id.broadcasting_text_view);
                actualStatus[0] = statusView == null ? "<missing view>" : statusView.getText().toString();
            });
            if (expectedStatus.equals(actualStatus[0])) {
                return;
            }
            Thread.sleep(250);
        }

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(expectedStatusResId)));
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
