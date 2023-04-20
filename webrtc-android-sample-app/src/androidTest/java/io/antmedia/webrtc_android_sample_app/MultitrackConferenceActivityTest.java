package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_HEIGHT;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_WIDTH;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtcandroidframework.IWebRTCClient;
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
public class MultitrackConferenceActivityTest {

    //match
    private static final String START_NOW_TEXT = "Start now";

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(HomeActivity.PERMISSIONS_BELOW_ANDROID_S);

    @Before
    public void before() {
        //try before method to make @Rule run properly
    }

    @Test
    public void testJoinMultitrackRoom() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
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
    public void testJoinConfereceActivity() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ConferenceActivity.class);

        ActivityScenario<ConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<ConferenceActivity>() {
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

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_conference_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }

    public class NetworkClient {
        //private static final String BASE_URL = "http://54.36.113.133:3030/";
        private static final String BASE_URL = "http://192.168.1.24:3030/";

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
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testJoinWithExternalParticipant() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
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
    public void testJoinWithoutVideo() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
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
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
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
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MultitrackConferenceActivity.class);

        ActivityScenario<MultitrackConferenceActivity> scenario = ActivityScenario.launch(intent);

        final MultitrackConferenceActivity[] mactivity = new MultitrackConferenceActivity[1];
        scenario.onActivity(new ActivityScenario.ActivityAction<MultitrackConferenceActivity>() {
            @Override
            public void perform(MultitrackConferenceActivity activity) {
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
