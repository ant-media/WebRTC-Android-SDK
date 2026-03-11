package io.antmedia.webrtc_android_sample_app;

import static android.provider.Settings.System.getString;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;



import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.advanced.ConferenceActivityWithDifferentVideoSources;
import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ConferenceActivityTest {
    private static final long STATS_RETRY_DELAY_MS = 1000L;
    private static final int STATS_RETRY_COUNT = 20;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    private IdlingResource mIdlingResource;

    @Rule
    public ActivityScenarioRule<ConferenceActivity> conferenceActivityScenarioRule = new ActivityScenarioRule<>(ConferenceActivity.class);

    private String runningTest;
    private String roomName;

    @Before
    public void before() throws IOException {
        System.out.println("before test");
        connectInternet();

        getInstrumentation().waitForIdleSync();
        Context context = ApplicationProvider.getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        roomName = sharedPreferences.getString(context.getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
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
        conferenceActivityScenarioRule.getScenario().onActivity(activity -> {
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
        conferenceActivityScenarioRule.getScenario().onActivity(activity -> {
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

        Thread.sleep(10000);

        onView(withId(R.id.stats_popup_container)).perform(swipeUp());

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));


       // Thread.sleep(5000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .perform(waitForTrackStatsItem());

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    TextView textView1 = requireFirstTrackStatTextView((RecyclerView) view);
                    int bytesReceived = Integer.parseInt(( textView1).getText().toString());
                    assertTrue(bytesReceived > 0);
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

     //   Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void testJoinWithoutVideo() throws InterruptedException {
        conferenceActivityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.toggle_send_audio_button)).check(matches(withText("Disable Send Audio")));
        onView(withId(R.id.toggle_send_audio_button)).perform(click());

        onView(withId(R.id.toggle_send_video_button)).check(matches(withText("Disable Send Video")));
        onView(withId(R.id.toggle_send_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        RemoteConferenceParticipant participant = RemoteConferenceParticipant.addConferenceParticipant(roomName, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));


        onView(withId(R.id.show_stats_button)).perform(click());

        //wait to collect some stats
        Thread.sleep(5000);


        //black frame sender should be working.
        onView(withId(R.id.stats_popup_container))
                .perform(waitForNumericTextViewValueGreaterThan(
                        R.id.multitrack_stats_popup_bytes_sent_video_textview,
                        0f,
                        "Timed out waiting for sent video bytes to become positive"
                ));

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.stats_popup_container))
                .perform(waitForAnyTrackStatsItem())
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    TextView textView1 = requireFirstAvailableTrackStatTextView(view);
                    int bytesReceived = Integer.parseInt(( textView1).getText().toString());
                    assertTrue(bytesReceived > 0);
                });


        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.toggle_send_video_button)).check(matches(withText("Enable Send Video")));
        onView(withId(R.id.toggle_send_video_button)).perform(click());

        Thread.sleep(10000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);

        });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.join_conference_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        participant.leave();
        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


    //@Test TODO FIX
    public void testJoinPlayOnlyAsFirstPerson() throws InterruptedException {
        conferenceActivityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
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
                .perform(waitForTrackStatsItem())
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    TextView textView1 = requireFirstTrackStatTextView((RecyclerView) view);
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
        conferenceActivityScenarioRule.getScenario().onActivity(activity -> {
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

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        Thread.sleep(5000);
        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .perform(waitForTrackStatsItem())
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    TextView textView1 = requireFirstTrackStatTextView((RecyclerView) view);
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

    private ViewAction waitForTrackStatsItem() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "wait for track stats recycler view to contain and bind the first item";
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView recyclerView = (RecyclerView) view;
                assertNotNull("Stats RecyclerView adapter is null", recyclerView.getAdapter());

                for (int i = 0; i < STATS_RETRY_COUNT; i++) {
                    if (recyclerView.getAdapter().getItemCount() > 0) {
                        recyclerView.scrollToPosition(0);
                        uiController.loopMainThreadUntilIdle();

                        if (recyclerView.findViewHolderForAdapterPosition(0) != null) {
                            return;
                        }
                    }
                    uiController.loopMainThreadForAtLeast(STATS_RETRY_DELAY_MS);
                }

                throw new AssertionError("Stats RecyclerView has no items");
            }
        };
    }

    private ViewAction waitForAnyTrackStatsItem() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "wait for either audio or video track stats recycler view to contain and bind the first item";
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView videoRecyclerView = view.findViewById(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview);
                RecyclerView audioRecyclerView = view.findViewById(R.id.multitrack_stats_popup_play_stats_audio_track_recyclerview);

                assertNotNull("Video stats RecyclerView is missing", videoRecyclerView);
                assertNotNull("Audio stats RecyclerView is missing", audioRecyclerView);
                assertNotNull("Video stats RecyclerView adapter is null", videoRecyclerView.getAdapter());
                assertNotNull("Audio stats RecyclerView adapter is null", audioRecyclerView.getAdapter());

                for (int i = 0; i < STATS_RETRY_COUNT; i++) {
                    if (hasBoundTrackStatsItem(videoRecyclerView, uiController) || hasBoundTrackStatsItem(audioRecyclerView, uiController)) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(STATS_RETRY_DELAY_MS);
                }

                throw new AssertionError("Neither video nor audio stats RecyclerView has any items");
            }
        };
    }

    private ViewAction waitForNumericTextViewValueGreaterThan(int textViewId, float minimumValue, String timeoutMessage) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return timeoutMessage;
            }

            @Override
            public void perform(UiController uiController, View view) {
                for (int i = 0; i < STATS_RETRY_COUNT; i++) {
                    TextView textView = view.findViewById(textViewId);
                    if (textView != null) {
                        String text = textView.getText().toString().trim();
                        if (!text.isEmpty()) {
                            try {
                                float value = Float.parseFloat(text);
                                if (value > minimumValue) {
                                    return;
                                }
                            } catch (NumberFormatException ignored) {
                                // Keep polling until the stats text becomes numeric.
                            }
                        }
                    }
                    uiController.loopMainThreadForAtLeast(STATS_RETRY_DELAY_MS);
                }

                TextView textView = view.findViewById(textViewId);
                String currentValue = textView == null ? "<missing>" : textView.getText().toString();
                throw new AssertionError(timeoutMessage + ". Current value: " + currentValue);
            }
        };
    }

    private TextView requireFirstTrackStatTextView(RecyclerView recyclerView) {
        assertNotNull("Stats RecyclerView adapter is null", recyclerView.getAdapter());
        assertTrue("Stats RecyclerView has no items", recyclerView.getAdapter().getItemCount() > 0);

        recyclerView.scrollToPosition(0);
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
        assertNotNull("Stats RecyclerView item 0 is not bound yet", viewHolder);

        TextView textView = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);
        assertNotNull("Track stats bytes received text view is missing", textView);
        return textView;
    }

    private TextView requireFirstAvailableTrackStatTextView(View statsPopupView) {
        RecyclerView videoRecyclerView = statsPopupView.findViewById(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview);
        RecyclerView audioRecyclerView = statsPopupView.findViewById(R.id.multitrack_stats_popup_play_stats_audio_track_recyclerview);

        assertNotNull("Video stats RecyclerView is missing", videoRecyclerView);
        assertNotNull("Audio stats RecyclerView is missing", audioRecyclerView);

        if (videoRecyclerView.getAdapter() != null && videoRecyclerView.getAdapter().getItemCount() > 0) {
            return requireFirstTrackStatTextView(videoRecyclerView);
        }

        if (audioRecyclerView.getAdapter() != null && audioRecyclerView.getAdapter().getItemCount() > 0) {
            return requireFirstTrackStatTextView(audioRecyclerView);
        }

        throw new AssertionError("Neither video nor audio stats RecyclerView has any items");
    }

    private boolean hasBoundTrackStatsItem(RecyclerView recyclerView, UiController uiController) {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) {
            return false;
        }

        recyclerView.scrollToPosition(0);
        uiController.loopMainThreadUntilIdle();
        return recyclerView.findViewHolderForAdapterPosition(0) != null;
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
