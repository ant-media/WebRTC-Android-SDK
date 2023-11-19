package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
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
import androidx.test.platform.app.InstrumentationRegistry;
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
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MultitrackConferenceActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(AbstractSampleSDKActivity.REQUIRED_PUBLISH_PERMISSIONS);
    private String runningTest;

    @Before
    public void before() {
        //try before method to make @Rule run properly
        System.out.println("before test");
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        System.out.println("after sleep");

    }

    @After
    public void after() {
        System.out.println("after test");
        try {
            Thread.sleep(10000);
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


   //@Test
    public void testJoinMultitrackRoom() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);
        final String roomName = "room_" + RandomStringUtils.randomNumeric(3);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);

                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    //@Test
    public void testJoinWithExternalParticipant() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);
        final String roomName = "room_" + RandomStringUtils.randomNumeric(3);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);

                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    //@Test
    public void testJoinWithoutVideo() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);
        final String roomName = "room_" + RandomStringUtils.randomNumeric(3);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);

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

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


    @Test
    public void testJoinPlayOnlyAsFirstPerson() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);
        final String roomName = "room_" + RandomStringUtils.randomNumeric(3);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);

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

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    //@Test
    public void testReconnect() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);
        final String roomName = "room_" + RandomStringUtils.randomNumeric(3);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        final MultitrackConferenceActivity[] mactivity = new MultitrackConferenceActivity[1];
        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
                SettingsActivity.changeRoomName(activity, roomName);

                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                mactivity[0] = activity;
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteParticipant participant = RemoteParticipant.addParticipant(roomName, runningTest);

        mactivity[0].changeWifiState(false);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mactivity[0].changeWifiState(true);



        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        //onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


}
