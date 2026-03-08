package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.content.Context;
import android.content.SharedPreferences;


import androidx.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;


import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.advanced.ConferenceActivityWithDifferentVideoSources;

import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConferenceActivityWithDifferentVideoSourcesTest {
    private static final long UI_WAIT_TIMEOUT_MS = 10000L;
    private static final long STATS_RETRY_DELAY_MS = 1000L;
    private static final int STATS_RETRY_COUNT = 5;

    private float videoBytesSent = 0;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityScenarioRule<ConferenceActivityWithDifferentVideoSources> conferenceActivityWithDifferentVideoSourcesScenarioRule = new ActivityScenarioRule<>(ConferenceActivityWithDifferentVideoSources.class);

    private String runningTest;
    private String roomName;

    @Before
    public void before() throws IOException {
        //try before method to make @Rule run properly
        connectInternet();
        getInstrumentation().waitForIdleSync();
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        roomName = sharedPreferences.getString(context.getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
    }

    @After
    public void after() {
        System.out.println("after test");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Rule
    public TestWatcher watchman= new TestWatcher() {

        @Override
        protected void failed(Throwable e, Description description) {
            Log.i("TestWatcher", "*** "+description + " failed!\n");
        }

        @Override
        protected void succeeded(Description description) {
            Log.i("TestWatcher", "*** "+description + " succeeded!\n");
        }

        protected void starting(Description description) {
            Log.i("TestWatcher", "******\n*** "+description + " starting!\n");
            runningTest = description.toString();
        }

        protected void finished(Description description) {
            Log.i("TestWatcher", "*** "+description + " finished!\n******\n");
        }
    };

    @Test
    public void testConferenceSwitchStreamSource() throws InterruptedException {
        conferenceActivityWithDifferentVideoSourcesScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(performClick());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(performClick());

        Thread.sleep(3000);
        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            videoBytesSent = value;
        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(performClick());

        //Thread.sleep(3000);

        onView(withId(R.id.screen_share_button)).perform(performClick());

        clickScreenSharePermissionButton(device);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(performClick());

        assertVideoBytesSentChanged();

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(performClick());

        Thread.sleep(3000);


        onView(withId(R.id.front_camera_button)).perform(performClick());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(performClick());

        assertVideoBytesSentChanged();

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(performClick());

        Thread.sleep(3000);

        onView(withId(R.id.rear_camera_button)).perform(performClick());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(performClick());

        assertVideoBytesSentChanged();

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.stats_popup_container)).perform(waitFor(2000));

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(performClick());

        Thread.sleep(3000);

        onView(withId(R.id.join_conference_button)).perform(performClick());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        onView(withId(R.id.front_camera_button)).perform(performClick());

        onView(withId(R.id.join_conference_button)).perform(performClick());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        Thread.sleep(3000);

        onView(withId(R.id.show_stats_button)).perform(performClick());

        assertVideoBytesSentChanged();

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(performClick());

        onView(withId(R.id.join_conference_button)).perform(performClick());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));
    }

    public static ViewAction waitFor(long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed(); // No constraints, can be used on any view
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    public static ViewAction performClick() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Invoke View.performClick() on a displayed view.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    private void connectInternet() throws IOException {
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi enable");
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data enable");
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

            onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
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

}
