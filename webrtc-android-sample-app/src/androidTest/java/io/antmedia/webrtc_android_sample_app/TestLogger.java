package io.antmedia.webrtc_android_sample_app;

import android.util.Log;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestLogger extends TestWatcher {
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
}
