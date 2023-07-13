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
import static org.junit.Assert.assertTrue;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
            = GrantPermissionRule.grant(HomeActivity.PERMISSIONS_BELOW_ANDROID_S);

    @Before
    public void before() {

    }

    public void downloadTestFile() {
        // Specify the URL of the file you want to download
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String fileUrl = "https://file-examples.com/storage/fe56bbd83564ad7489ca047/2017/11/file_example_MP3_1MG.mp3";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "sample_44100_stereo.mp3");
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);
        assertTrue(downloadId != -1);

        waitForDownloadCompletion(downloadManager, downloadId);

        // Assert that the download is completed
        assertEquals(DownloadManager.STATUS_SUCCESSFUL, getDownloadStatus(downloadManager, downloadId));
    }

    private void waitForDownloadCompletion(DownloadManager downloadManager, long downloadId) {
        boolean downloadInProgress = true;
        while (downloadInProgress) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int status = cursor.getInt(statusIndex);
                cursor.close();
                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    downloadInProgress = false;
                }
            }
        }
    }

    private int getDownloadStatus(DownloadManager downloadManager, long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(statusIndex);
            cursor.close();
            return status;
        }
        return -1;
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
            }
        });

        downloadTestFile();

        onView(withId(R.id.broadcasting_text_view)).check(matches(not(isDisplayed())));
        onView(withId(R.id.start_streaming_button)).check(matches(withText("Start Publishing")));
        onView(withId(R.id.start_streaming_button)).perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withId(R.id.start_streaming_button)).check(matches(withText("Stop Publishing")));

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        //2. stop stream and check that it's stopped
        onView(withId(R.id.start_streaming_button)).perform(click());

        onView(withId(R.id.broadcasting_text_view)).check(ViewAssertions.matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        IdlingRegistry.getInstance().unregister(mIdlingResource);

    }
}
