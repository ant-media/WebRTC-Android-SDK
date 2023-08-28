package io.antmedia.webrtc_android_sample_app;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {


    @Rule
    public ActivityScenarioRule<HomeActivity> activityScenarioRule
            = new ActivityScenarioRule<>(HomeActivity.class);

}
