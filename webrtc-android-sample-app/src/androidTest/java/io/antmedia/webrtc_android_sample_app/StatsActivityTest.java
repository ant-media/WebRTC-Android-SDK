package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtc_android_sample_app.basic.StatsActivity;
import io.antmedia.webrtcandroidframework.core.PermissionsHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class StatsActivityTest {
    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionsHandler.REQUIRED_EXTENDED_PERMISSIONS);

    @Before
    public void before() {
        //try before method to make @Rule run properly
    }

    @Rule
    public TestLogger testLogger = new TestLogger();

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = getInstrumentation().getTargetContext();
        assertEquals("io.antmedia.webrtc_android_sample_app", appContext.getPackageName());
    }

    @Test
    public void testStatsCollector() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), StatsActivity.class);
        ActivityScenario<StatsActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        });

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start")));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.live)));

        onView(withId(R.id.show_stats_button)).perform(click());

        Thread.sleep(3000);

        onView(withId(R.id.stats_popup_packets_lost_audio_textview)).inRoot(isDialog()).check(matches(isDisplayed()));

        onView(withId(R.id.stats_popup_packets_lost_audio_textview)).inRoot(isDialog()).check(matches(isDisplayed()));

        //below stat should be always greater than 0.
        int[] audioTrackStatsTextViewIds = {
                R.id.stats_popup_jitter_audio_textview,
                R.id.stats_popup_rtt_audio_textview,
                R.id.stats_popup_packets_sent_audio_textview,
                R.id.stats_popup_bytes_sent_audio_textview,
                R.id.stats_popup_packets_sent_per_second_audio_textview,
                R.id.stats_popup_local_audio_bitrate_textview,
                R.id.stats_popup_local_audio_level_textview
        };

        int[] videoTrackStatsTextViewIds = {
                R.id.stats_popup_jitter_video_textview,
                R.id.stats_popup_rtt_video_textview,
                R.id.stats_popup_pli_count_video_textview,
                R.id.stats_popup_nack_count_video_textview,
                R.id.stats_popup_packets_sent_video_textview,
                R.id.stats_popup_frames_encoded_video_textview,
                R.id.stats_popup_bytes_sent_video_textview,
                R.id.stats_popup_packets_sent_per_second_video_textview,
                R.id.stats_popup_local_video_bitrate_textview
        };

        // Assert for audio TextViews
        for (int id : audioTrackStatsTextViewIds) {
            onView(withId(id)).check((view, noViewFoundException) -> {
                String text = ((TextView) view).getText().toString();
                float value = Float.parseFloat(text);
                assertTrue(value > 0);
            });
        }

        for (int id : videoTrackStatsTextViewIds) {
            onView(withId(id)).check((view, noViewFoundException) -> {
                String text = ((TextView) view).getText().toString();
                float value = Float.parseFloat(text);
                assertTrue(value > 0);
            });
        }

        onView(withId(R.id.stats_popup_close_button)).perform(click());

        onView(withId(R.id.start_streaming_button)).perform(click());

        Thread.sleep(5000);

        onView(withId(R.id.broadcasting_text_view))
                .check(matches(withText(R.string.disconnected)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }
}
