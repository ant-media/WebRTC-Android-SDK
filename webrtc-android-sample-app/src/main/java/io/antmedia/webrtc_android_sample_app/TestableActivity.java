package io.antmedia.webrtc_android_sample_app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.apache.commons.lang3.RandomStringUtils;

public abstract class TestableActivity extends AppCompatActivity {
    public CountingIdlingResource idlingResource = new CountingIdlingResource("Load", true);
    protected SharedPreferences sharedPreferences;
    public static String PEER_ROOM_ID_FOR_TEST = "p2p"+ RandomStringUtils.randomAlphanumeric(3);

    public void  incrementIdle() {
        idlingResource.increment();
    }
    public void decrementIdle() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    public IdlingResource getIdlingResource() {
        return idlingResource;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getSupportActionBar().hide();

        sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);

    }
}

