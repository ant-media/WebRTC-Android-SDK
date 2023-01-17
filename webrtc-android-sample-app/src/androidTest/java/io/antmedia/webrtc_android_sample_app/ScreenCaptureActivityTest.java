package io.antmedia.webrtc_android_sample_app;

import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;


import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScreenCaptureActivityTest {

    private IdlingResource mIdlingResource;

    //match
    private static final String START_NOW_TEXT = "Start now";
    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(HomeActivity.PERMISSIONS_UNDER_ANDROID_S);


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

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Streaming")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Streaming")));
        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);



    }
}
