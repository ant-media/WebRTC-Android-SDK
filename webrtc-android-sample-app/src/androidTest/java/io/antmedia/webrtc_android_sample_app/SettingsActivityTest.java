package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;
import static io.antmedia.webrtc_android_sample_app.basic.SettingsActivity.DEFAULT_WEBSOCKET_URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;



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

   //@Test
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
