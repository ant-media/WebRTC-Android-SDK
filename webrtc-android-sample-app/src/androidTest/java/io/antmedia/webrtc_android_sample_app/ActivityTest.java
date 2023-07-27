package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
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

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static io.antmedia.webrtc_android_sample_app.SettingsActivity.DEFAULT_WEBSOCKET_URL;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_HEIGHT;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_WIDTH;

import io.antmedia.webrtcandroidframework.IWebRTCClient;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(HomeActivity.PERMISSIONS_UNDER_ANDROID_S);

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
    public void testPlayStream() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_PLAY);

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MainActivity>() {
            @Override
            public void perform(MainActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        //stream556677i4d is the stream id in github actions
        onView(withId(R.id.stream_id_edittext)).perform(clearText(), typeText("stream556677i4d"));
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Playing")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());


        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Playing")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //Stop playing
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    /**
     * Write test scenarios and implement it.
     * Until that time, manuel test may be used
     */
    @Test
    public void testStartStopStream() {

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(EXTRA_VIDEO_WIDTH, 160);
        intent.putExtra(EXTRA_VIDEO_HEIGHT, 120);
        intent.putExtra(EXTRA_VIDEO_FPS, 15);
        intent.putExtra(EXTRA_VIDEO_BITRATE, 300);
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MainActivity>() {
            @Override
            public void perform(MainActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });
        //1. start stream and check that it's playing

        onView(withId(R.id.broadcasting_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Publishing")));
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Publishing")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

       //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    /**
     * This test should be in another method but cannot get the full logcat so it's moved here
     */
    @Test
    public void testPublishScreen() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ScreenCaptureActivity.class);
        ActivityScenario<ScreenCaptureActivity> scenario = ActivityScenario.launch(intent);


        scenario.onActivity(new ActivityScenario.ActivityAction<ScreenCaptureActivity>() {

            @Override
            public void perform(ScreenCaptureActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        onView(withId(R.id.rbScreen)).perform(click());
        UiObject2 button = device.wait(Until.findObject(By.text("Start now")), 10000);
        assertNotNull(button);
        button.click();

        //this switch operation causes to crash so that it's added here as test
        onView(withId(R.id.rbFront)).perform(click());
        onView(withId(R.id.rbScreen)).perform(click());



        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Streaming")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Streaming")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        //Publish again without because it was failing
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Streaming")));
        onView(withId(R.id.start_streaming_button)).perform(click());

        //Check it's publishing again
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Streaming")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //Stop publishing
        onView(withId(R.id.start_streaming_button)).perform(click());
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void testDataChannelOnlyActivityScreen() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DataChannelOnlyActivity.class);
        ActivityScenario<DataChannelOnlyActivity> scenario = ActivityScenario.launch(intent);


        scenario.onActivity(new ActivityScenario.ActivityAction<DataChannelOnlyActivity>() {

            @Override
            public void perform(DataChannelOnlyActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());


        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.message_text_input)).perform(typeText("hello"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.send_message_button)).perform(click());

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));


        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void testSettingsActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), SettingsActivity.class);
        ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(intent);


        scenario.onActivity(new ActivityScenario.ActivityAction<SettingsActivity>() {

            @Override
            public void perform(SettingsActivity activity) {
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        //if default value has changed, it fails.
        onView(withId(R.id.server_address)).check(matches(withText(DEFAULT_WEBSOCKET_URL)));

        String websocketURL = "ws://example.com/WebRTCAppEE/websocket";
        onView(withId(R.id.server_address)).perform(clearText());
        onView(withId(R.id.server_address)).perform(typeText(websocketURL));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.save_button)).perform(click());



        scenario.onActivity(new ActivityScenario.ActivityAction<SettingsActivity>() {

            @Override
            public void perform(SettingsActivity activity) {
                SharedPreferences sharedPreferences =
                        android.preference.PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */);
                String url = sharedPreferences.getString(activity.getString(R.string.serverAddress), DEFAULT_WEBSOCKET_URL);
                assertEquals(websocketURL, url);

            }
        });

        onView(withId(R.id.server_address)).perform(clearText());
        onView(withId(R.id.server_address)).perform(typeText(DEFAULT_WEBSOCKET_URL));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.save_button)).perform(click());


    }
}
