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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static io.antmedia.webrtc_android_sample_app.SettingsActivity.DEFAULT_WEBSOCKET_URL;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_HEIGHT;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_WIDTH;

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

import io.antmedia.webrtcandroidframework.IWebRTCClient;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DataChannelActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(AbstractSampleSDKActivity.REQUIRED_PUBLISH_PERMISSIONS);

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
}
