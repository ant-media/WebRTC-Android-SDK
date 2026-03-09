package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static io.antmedia.webrtc_android_sample_app.basic.SettingsActivity.DEFAULT_WEBSOCKET_URL;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.PeerActivity;
import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PeerActivityTest {
    private IdlingResource mIdlingResource;
    private String runningTest;
    private float videoBytesSent = 0;
    private float videoBytesReceived = 0;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);


    @Before
    public void before() throws IOException {
        connectInternet();
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

    @Rule
    public TestLogger testLogger = new TestLogger();

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }

    @Test
    public void testPeerToPeer() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PeerActivity.class);
        ActivityScenario<PeerActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Join")));
        Espresso.closeSoftKeyboard();
        String randomPeerRoomId = "p2p"+ RandomStringUtils.randomAlphanumeric(6);
        onView(withId(R.id.stream_id_edittext)).perform(replaceText(randomPeerRoomId));

        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.connecting), withText(R.string.live))));
        RemoteP2PParticipant remoteP2PParticipant = RemoteP2PParticipant.addP2PParticipant(randomPeerRoomId, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

       // Thread.sleep(3000);
        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView bytesReceivedText = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);
                    int bytesReceived = Integer.parseInt(( bytesReceivedText).getText().toString());
                    assertTrue(bytesReceived > 0);
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        //Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        remoteP2PParticipant.leave();

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    @Test
    public void testPeerToPeerReconnection() throws InterruptedException, IOException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PeerActivity.class);
        ActivityScenario<PeerActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });
        String randomPeerRoomId = "p2p"+ RandomStringUtils.randomAlphanumeric(6);

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Join")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.stream_id_edittext)).perform(replaceText(randomPeerRoomId));

        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(anyOf(withText(R.string.connecting), withText(R.string.live))));

        RemoteP2PParticipant remoteP2PParticipant = RemoteP2PParticipant.addP2PParticipant(randomPeerRoomId, runningTest);

        Thread.sleep(10000);

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        //Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));


        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            videoBytesSent = value;
        });


        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView bytesReceivedText = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);

                    int bytesReceived = Integer.parseInt(( bytesReceivedText).getText().toString());

                    assertTrue(bytesReceived > 0);
                    videoBytesReceived = bytesReceived;
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        //Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

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

        //Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview)).inRoot(isDialog()).check(matches(isDisplayed()));


        onView(withId(R.id.multitrack_stats_popup_bytes_sent_video_textview)).check((view, noViewFoundException) -> {
            String text = ((TextView) view).getText().toString();
            float value = Float.parseFloat(text);
            assertTrue(value > 0f);
            assertTrue(value > videoBytesSent);
            videoBytesSent = value;
        });


        onView(withId(R.id.multitrack_stats_popup_play_stats_video_track_recyclerview))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    RecyclerView recyclerView = (RecyclerView) view;
                    RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(0);
                    TextView bytesReceivedText = viewHolder.itemView.findViewById(R.id.track_stats_item_bytes_received_textview);

                    int bytesReceived = Integer.parseInt(( bytesReceivedText).getText().toString());

                    assertTrue(bytesReceived > 0);
                    assertTrue(bytesReceived > videoBytesReceived);
                });

        onView(withId(R.id. stats_popup_container)).perform(swipeUp());

        //Thread.sleep(3000);

        onView(withId(R.id.multitrack_stats_popup_close_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        remoteP2PParticipant.leave();

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }
    private void disconnectInternet() throws IOException {
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi disable"); // Switch off Wifi
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data disable"); // Switch off Mobile Data
    }

    private void connectInternet() throws IOException {
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc wifi enable"); // Switch Wifi on again
        UiDevice
                .getInstance(InstrumentationRegistry.getInstrumentation())
                .executeShellCommand("svc data enable"); // Switch Mobile Data on again
    }

}
