package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertNotNull;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.core.PermissionsHandler;


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
            = GrantPermissionRule.grant(PermissionsHandler.REQUIRED_EXTENDED_PERMISSIONS);

    @Rule
    public ActivityScenarioRule<ConferenceActivity> activityScenarioRule = new ActivityScenarioRule<>(ConferenceActivity.class);
    private String runningTest;
    private String roomName;

    @Before
    public void before() {
        //try before method to make @Rule run properly
        System.out.println("before test");

        getInstrumentation().waitForIdleSync();

        roomName = "room_" + RandomStringUtils.randomNumeric(3);
        Context context = getInstrumentation().getTargetContext();
        SettingsActivity.changeRoomName(context, roomName);
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
    public void testJoinMultitrackRoom() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.join_conference_button)).perform(click());

        Log.i(ConferenceActivityTest.class.getSimpleName(), "is idling idle now before sleep:"+mIdlingResource.isIdleNow());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(ConferenceActivityTest.class.getSimpleName(), "is idling idle now after sleep:"+mIdlingResource.isIdleNow());

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void testJoinWithExternalParticipant() throws InterruptedException {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    //@Test
    public void testJoinWithoutVideo() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.control_audio_button)).check(matches(withText("Disable Audio")));
        onView(withId(R.id.control_audio_button)).perform(click());

        onView(withId(R.id.control_video_button)).check(matches(withText("Disable Video")));
        onView(withId(R.id.control_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

        onView(withId(R.id.control_audio_button)).check(matches(withText("Enable Audio")));
        onView(withId(R.id.control_audio_button)).perform(click());

        onView(withId(R.id.control_video_button)).check(matches(withText("Enable Video")));
        onView(withId(R.id.control_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


    //@Test
    public void testJoinPlayOnlyAsFirstPerson() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.play_only_switch)).check(matches(withText("Play Only")));
        onView(withId(R.id.play_only_switch)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    @Test
    public void testConferenceReconnect() throws IOException, InterruptedException {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
            @Override
            public void perform(ConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

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

        Thread.sleep(3000);

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        onView(withId(R.id.join_conference_button)).perform(click());

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

        Thread.sleep(3000);

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    private void disconnectInternet() throws IOException {
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi disable"); // Switch off Wifi
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data disable"); // Switch off Mobile Data
    }

    private void connectInternet() throws IOException {
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi enable"); // Switch Wifi on again
        UiDevice
                .getInstance(androidx.test.InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data enable"); // Switch Mobile Data on again
    }


}
