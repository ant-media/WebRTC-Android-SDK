package io.antmedia.webrtc_android_sample_app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.antmedia.webrtcandroidframework.IWebRTCClient;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MP3PublishActivityTest {

    private IdlingResource mIdlingResource;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(AbstractSampleSDKActivity.REQUIRED_PUBLISH_PERMISSIONS);

    @Before
    public void before() {

    }

    public String downloadTestFile() {
        try {
            Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

            InputStream inputStream = appContext.getResources().openRawResource(R.raw.sample_44100_stereo);

            File destinationFile = new File(appContext.getCacheDir(), "sample_44100_stereo.mp3");

            //File destinationFile = new File(Environment.DIRECTORY_DOWNLOADS, "sample_44100_stereo.mp3");
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return destinationFile.getAbsolutePath();

            // File copied successfully
        } catch (IOException e) {
            fail("test file cannot be copied");
            e.printStackTrace();
            // Handle the exception
        }

        return null;
    }


    @Test
    public void testCustomAudioFeed() {

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MP3PublishActivity.class);
        intent.putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_PUBLISH);

        ActivityScenario<MP3PublishActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(new ActivityScenario.ActivityAction<MP3PublishActivity>() {
            @Override
            public void perform(MP3PublishActivity activity) {
                mIdlingResource = activity.getIdlingResource();
                IdlingRegistry.getInstance().register(mIdlingResource);
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                String path = downloadTestFile();
                assertNotNull(path);
                Log.d("MP3PublishActivityTest", "path: " + path);
                activity.mp3Publisher.setFilePath(path);
            }
        });



        onView(withId(R.id.broadcasting_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Publishing")));
        onView(withId(R.id.start_streaming_button)).perform(click());


        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Publishing")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }
}
