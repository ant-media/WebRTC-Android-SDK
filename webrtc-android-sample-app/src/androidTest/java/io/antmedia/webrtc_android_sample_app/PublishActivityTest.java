package io.antmedia.webrtc_android_sample_app;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.test.InstrumentationRegistry;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtcandroidframework.core.PermissionHandler;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PublishActivityTest {
    private static final long STOP_TIMEOUT_MS = 10000;
    private static final long STOP_SETTLE_DELAY_MS = 3000;
    private static final long CONNECTION_TIMEOUT_MS = 60000;

    @Rule
    public GrantPermissionRule permissionRule
            = GrantPermissionRule.grant(PermissionHandler.FULL_PERMISSIONS);


    @Before
    public void before() throws IOException {
        connectInternet();
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
    public void testPublishing() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PublishActivity.class);
        ActivityScenario<PublishActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity ->
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)));

        clickStartStopButton(scenario);
        waitForBroadcastStatus(scenario, R.string.live, CONNECTION_TIMEOUT_MS);
        clickStartStopButton(scenario);
        waitForBroadcastStatus(scenario, R.string.disconnected, STOP_TIMEOUT_MS);
    }

    @Test
    public void testPublishReconnection() throws InterruptedException, IOException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), PublishActivity.class);
        ActivityScenario<PublishActivity> scenario = ActivityScenario.launch(intent);

        scenario.onActivity(activity ->
                activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)));

        clickStartStopButton(scenario);
        waitForBroadcastStatus(scenario, R.string.live, CONNECTION_TIMEOUT_MS);

        reconnectAndWaitUntilLive(scenario);

        clickStartStopButton(scenario);

        waitForBroadcastStatus(scenario, R.string.disconnected, STOP_TIMEOUT_MS);

        // onPublishFinished updates the status before peer teardown has necessarily completed.
        // Let teardown settle before publishing the same stream id again.
        Thread.sleep(STOP_SETTLE_DELAY_MS);

        clickStartStopButton(scenario);
        waitForBroadcastStatus(scenario, R.string.live, CONNECTION_TIMEOUT_MS);

        reconnectAndWaitUntilLive(scenario);

        clickStartStopButton(scenario);

        waitForBroadcastStatus(scenario, R.string.disconnected, STOP_TIMEOUT_MS);

    }

    private void reconnectAndWaitUntilLive(ActivityScenario<PublishActivity> scenario)
            throws IOException, InterruptedException {
        disconnectInternet();
        waitForAnyBroadcastStatus(scenario, CONNECTION_TIMEOUT_MS,
                R.string.disconnected, R.string.reconnecting);

        connectInternet();
        waitForBroadcastStatus(scenario, R.string.live, CONNECTION_TIMEOUT_MS);
    }

    private void clickStartStopButton(ActivityScenario<PublishActivity> scenario) {
        scenario.onActivity(activity ->
                activity.findViewById(R.id.start_streaming_button).performClick());
    }

    private void waitForBroadcastStatus(ActivityScenario<PublishActivity> scenario,
                                        int expectedStatusResId, long timeoutMs)
            throws InterruptedException {
        waitForAnyBroadcastStatus(scenario, timeoutMs, expectedStatusResId);
    }

    private void waitForAnyBroadcastStatus(ActivityScenario<PublishActivity> scenario,
                                           long timeoutMs, int... expectedStatusResIds)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String[] expectedStatuses = new String[expectedStatusResIds.length];
        for (int i = 0; i < expectedStatusResIds.length; i++) {
            expectedStatuses[i] = context.getString(expectedStatusResIds[i]);
        }
        long startTimeMs = SystemClock.elapsedRealtime();
        String[] actualStatus = {"<unavailable>"};

        while (SystemClock.elapsedRealtime() - startTimeMs < timeoutMs) {
            scenario.onActivity(activity -> {
                TextView statusView = activity.findViewById(R.id.broadcasting_text_view);
                actualStatus[0] = statusView == null ? "<missing view>" : statusView.getText().toString();
            });
            for (String expectedStatus : expectedStatuses) {
                if (expectedStatus.equals(actualStatus[0])) {
                    return;
                }
            }
            Thread.sleep(250);
        }

        throw new AssertionError("Timed out waiting for broadcast status "
                + java.util.Arrays.toString(expectedStatuses) + "; last status was " + actualStatus[0]);
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
