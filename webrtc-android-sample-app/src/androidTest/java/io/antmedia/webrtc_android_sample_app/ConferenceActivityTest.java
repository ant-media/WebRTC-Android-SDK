package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConferenceActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(AbstractSampleSDKActivity.REQUIRED_PUBLISH_PERMISSIONS);
    private String roomName;

    @Before
    public void before() {
        //try before method to make @Rule run properly
        System.out.println("before test");
    }

    @After
    public void after() {
        try {
            Thread.sleep(5000);
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
        }

        protected void finished(Description description) {
            Log.i("TestWatcher", "*** "+description + " finished!\n******\n");
        }
    };


    @Test
    public void testJoinConfereceActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConferenceActivity.class);
        roomName = "room_"+RandomStringUtils.randomNumeric(3);
        ActivityScenario<ConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

}
