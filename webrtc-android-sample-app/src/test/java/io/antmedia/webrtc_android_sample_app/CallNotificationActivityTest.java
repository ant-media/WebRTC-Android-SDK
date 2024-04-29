```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class CallNotificationActivityTest {

    private CallNotificationActivity callNotificationActivity;

    @Mock
    private IWebRTCClient webRTCClient;

    @Mock
    private SurfaceViewRenderer fullScreenRenderer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        callNotificationActivity = new CallNotificationActivity();
        callNotificationActivity.webRTCClient = webRTCClient;
        callNotificationActivity.fullScreenRenderer = fullScreenRenderer;
    }

    @Test
    public void onCreate_initializesFirebaseAndRegistersPushNotificationToken() {
        callNotificationActivity.onCreate(null);

        verify(webRTCClient, times(1)).registerPushNotificationToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void onCreate_sendsPushNotification() {
        callNotificationActivity.onCreate(null);

        verify(webRTCClient, times(1)).sendPushNotification(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void askNotificationPermission_requestsPermissionWhenNotGranted() {
        when(ContextCompat.checkSelfPermission(callNotificationActivity, Manifest.permission.POST_NOTIFICATIONS))
                .thenReturn(PackageManager.PERMISSION_DENIED);

        callNotificationActivity.askNotificationPermission();

        verify(callNotificationActivity.requestPermissionLauncher, times(1)).launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Test
    public void askNotificationPermission_doesNotRequestPermissionWhenAlreadyGranted() {
        when(ContextCompat.checkSelfPermission(callNotificationActivity, Manifest.permission.POST_NOTIFICATIONS))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        callNotificationActivity.askNotificationPermission();

        verify(callNotificationActivity.requestPermissionLauncher, times(0)).launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
