package io.antmedia.webrtcandroidframework;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.webrtc.SurfaceViewRenderer;

public class TestActivity extends Activity {
        private SurfaceViewRenderer testRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        testRenderer = findViewById(R.id.camera_view_renderer);
    }

    public SurfaceViewRenderer getTestRenderer() {
        return testRenderer;
    }
}
