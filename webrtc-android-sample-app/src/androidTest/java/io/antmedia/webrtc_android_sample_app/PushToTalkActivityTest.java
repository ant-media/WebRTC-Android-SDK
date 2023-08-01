package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

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
public class PushToTalkActivityTest {

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(HomeActivity.REQUIRED_PERMISSIONS);

    @Before
    public void before() {
        //try before method to make @Rule run properly
    }

    @Test
    public void testCustomVideoFeed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PushToTalkActivity.class);
        intent.putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_PUBLISH);

        ActivityScenario<PushToTalkActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<PushToTalkActivity>() {
            @Override
            public void perform(PushToTalkActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.talkButton)).perform(longClick());
        onView(withId(R.id.talkButton)).check(matches(withText("Talk")));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }
}
