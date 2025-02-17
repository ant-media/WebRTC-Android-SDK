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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;



import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;

import androidx.test.core.app.ApplicationProvider;
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
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.lang.reflect.Field;

import io.antmedia.webrtc_android_sample_app.basic.DynamicConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;
import io.antmedia.webrtcandroidframework.core.WebRTCClient;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DynamicConferenceActivityTest {

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);

    private IdlingResource mIdlingResource;
    @Rule
    public ActivityScenarioRule<DynamicConferenceActivity> dynamicConferenceActivityScenarioRule = new ActivityScenarioRule<>(DynamicConferenceActivity.class);


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

    @Test
    public void dynamicAddRemoveRendererTest()  {
        dynamicConferenceActivityScenarioRule.getScenario().onActivity(activity -> {
            mIdlingResource = activity.getIdlingResource();
            IdlingRegistry.getInstance().register(mIdlingResource);
            activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            SurfaceViewRenderer renderer = null;
            for(int i=0;i<10;i++)
                renderer = activity.addSurfaceViewRenderer();

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            GridLayout remoteParticipantsGridLayout = activity.findViewById(R.id.remote_participant_renderer);
            assertEquals(remoteParticipantsGridLayout.getChildCount(),10);

            activity.removeSurfaceViewRenderer(renderer);
            assertEquals(remoteParticipantsGridLayout.getChildCount(),9);

            activity.removeAllRenderers();
            assertEquals(remoteParticipantsGridLayout.getChildCount(),0);
        });
    }

}