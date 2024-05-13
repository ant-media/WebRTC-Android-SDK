package io.antmedia.webrtc_android_sample_app;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.SurfaceViewRenderer;

import static org.mockito.Mockito.*;

import io.antmedia.webrtc_android_sample_app.advanced.notification.CallNotificationActivity;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class CallNotificationActivityTest {

    private CallNotificationActivity callNotificationActivity;

    @Mock
    private IWebRTCClient webRTCClient;

    @Mock
    private SurfaceViewRenderer fullScreenRenderer;
}
