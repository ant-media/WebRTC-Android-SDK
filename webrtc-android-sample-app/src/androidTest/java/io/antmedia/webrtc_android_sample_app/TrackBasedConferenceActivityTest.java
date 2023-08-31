package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNotNull;

import android.app.Instrumentation;
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
public class TrackBasedConferenceActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(AbstractSampleSDKActivity.REQUIRED_PUBLISH_PERMISSIONS);

    @Rule
    public ActivityScenarioRule<TrackBasedConferenceActivity> activityScenarioRule = new ActivityScenarioRule<>(TrackBasedConferenceActivity.class);


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
        }

        protected void finished(Description description) {
            Log.i("TestWatcher", "*** "+description + " finished!\n******\n");
        }
    };

    public class NetworkClient {

        //private static final String BASE_URL = "http://192.168.1.26:3030/";
        private static final String BASE_URL = "http://10.0.2.2:3030/";

        private final OkHttpClient client = new OkHttpClient();

        public String get(String path) throws IOException {
            Request request = new Request.Builder()
                    .url(BASE_URL + path)
                    .header("Connection", "close") // <== solution, not declare in Interceptor
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            return response.body().string();
        }
    }

    class RemoteParticipant {
        NetworkClient client = new NetworkClient();
        String response = null;

        public void join() {
            try {
                response = client.get("create");
                assertNotNull(response);

                response = client.get("join");
                assertNotNull(response);

                Log.i("RemoteParticipant", "join: " + response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void leave() {
            try {
                response = client.get("leave");
                assertNotNull(response);

                response = client.get("delete");
                assertNotNull(response);

                Log.i("RemoteParticipant", "leave: " + response);

            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }
    }

    private void addParticipant() {
        RemoteParticipant participant = new RemoteParticipant();
        participant.join();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        participant.leave();
    }


    @Test
    public void testJoinMultitrackRoom() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<TrackBasedConferenceActivity>() {
            @Override
            public void perform(TrackBasedConferenceActivity activity) {

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



    @Test
    public void testJoinWithExternalParticipant() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<TrackBasedConferenceActivity>() {
            @Override
            public void perform(TrackBasedConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());


        addParticipant();


        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    //@Test
    public void testJoinWithoutVideo() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<TrackBasedConferenceActivity>() {
            @Override
            public void perform(TrackBasedConferenceActivity activity) {

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

        addParticipant();

        onView(withId(R.id.control_audio_button)).check(matches(withText("Enable Audio")));
        onView(withId(R.id.control_audio_button)).perform(click());

        onView(withId(R.id.control_video_button)).check(matches(withText("Enable Video")));
        onView(withId(R.id.control_video_button)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


    @Test
    public void testJoinPlayOnlyAsFirstPerson() {
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<TrackBasedConferenceActivity>() {
            @Override
            public void perform(TrackBasedConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        });

        onView(withId(R.id.play_only_switch)).check(matches(withText("Play Only")));
        onView(withId(R.id.play_only_switch)).perform(click());

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());

        addParticipant();


        onView(withId(R.id.join_conference_button)).check(matches(withText("Leave")));

        onView(withId(R.id.join_conference_button)).perform(click());

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    @Test
    public void testReconnect() {
        final TrackBasedConferenceActivity[] mactivity = new TrackBasedConferenceActivity[1];
        activityScenarioRule.getScenario().onActivity(new ActivityScenario.ActivityAction<TrackBasedConferenceActivity>() {
            @Override
            public void perform(TrackBasedConferenceActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                mactivity[0] = activity;
            }
        });

        onView(withId(R.id.join_conference_button)).check(matches(withText("Join Conference")));
        onView(withId(R.id.join_conference_button)).perform(click());


        addParticipant();

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

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }


}
