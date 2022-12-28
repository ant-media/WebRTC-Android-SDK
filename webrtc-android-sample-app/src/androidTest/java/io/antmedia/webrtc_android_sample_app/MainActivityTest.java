package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.idling.net.UriIdlingResource;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule
            = new ActivityScenarioRule<>(MainActivity.class);
    private IdlingResource mIdlingResource;

    @Rule public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(HomeActivity.PERMISSIONS_UNDER_ANDROID_S);


    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }

    /**
     * Write test scenarios and implement it.
     * Until that time, manuel test may be used
     */
    @Test
    public void testStartStopStream() {
        ActivityScenario<MainActivity> scenario = activityScenarioRule.getScenario();

        scenario.onActivity(new ActivityScenario.ActivityAction<MainActivity>() {
            @Override
            public void perform(MainActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
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
}
