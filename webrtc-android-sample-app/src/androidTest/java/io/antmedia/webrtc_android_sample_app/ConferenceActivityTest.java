package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertTrue;

import static io.antmedia.webrtc_android_sample_app.TestableActivity.CONFERENCE_ROOM_ID_FOR_TEST;


import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConferenceActivityTest {

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityScenarioRule<ConferenceActivity> activityScenarioRule = new ActivityScenarioRule<>(ConferenceActivity.class);

    private String runningTest;
    private String roomName;

    @Before
    public void before() throws IOException {
        //try before method to make @Rule run properly
        System.out.println("before test");
        connectInternet();

        getInstrumentation().waitForIdleSync();

        roomName = CONFERENCE_ROOM_ID_FOR_TEST;
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
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
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
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteConferenceParticipant participant = RemoteConferenceParticipant.addConferenceParticipant(roomName, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));


        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView textView1 = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);
                    int bytesReceived = Integer.parseInt(( textView1).getText().toString());
                    assertTrue(bytesReceived > 0);
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    //@Test TODO FIX THIS FUNCTIONALITY LATER.
    public void testJoinWithoutVideo() throws InterruptedException {
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.control_audio_button)).check(matches(withText("Disable Audio")));
        onView(withId(R.id.control_audio_button)).perform(click());

        onView(withId(R.id.control_video_button)).check(matches(withText("Disable Video")));
        onView(withId(R.id.control_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteConferenceParticipant participant = RemoteConferenceParticipant.addConferenceParticipant(roomName, runningTest);

        onView(withId(R.id.control_audio_button)).check(matches(withText("Enable Audio")));
        onView(withId(R.id.control_audio_button)).perform(click());

        onView(withId(R.id.control_video_button)).check(matches(withText("Enable Video")));
        onView(withId(R.id.control_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(5000);
        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


    //@Test TODO FIX
    public void testJoinPlayOnlyAsFirstPerson() throws InterruptedException {
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


        //onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteConferenceParticipant participant = RemoteConferenceParticipant.addConferenceParticipant(roomName, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView textView1 = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);
                    int bytesReceived = Integer.parseInt(( textView1).getText().toString());
                    assertTrue(bytesReceived > 0);
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void testConferenceReconnect() throws IOException, InterruptedException {
        activityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteConferenceParticipant participant = RemoteConferenceParticipant.addConferenceParticipant(roomName, runningTest);

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

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView textView1 = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);
                    int bytesReceived = Integer.parseInt(( textView1).getText().toString());
                    assertTrue(bytesReceived > 0);

                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(5000);

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
