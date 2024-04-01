package io.antmedia.webrtc_android_sample_app.advanced.notification;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.app.Activity;
import android.os.Bundle;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.MainActivity;
import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.concurrent.atomic.AtomicInteger;


public class CallNotificationActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);

        FirebaseApp.initializeApp(this);

        //FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);

        IWebRTCClient webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl("wss://ovh36.antmedia.io:5443/LiveApp/websocket")
                .build();

        String streamId = "streamId" + (int)(Math.random()*9999);

        PeerForNotificationActivity.streamId = streamId;

        //Define the subscriberId and it can be any subscriber Id
        String subscriberId = "test1@antmedia.io";

        //Define the receiverSubscriberId and it can be any subscriber Id
        String receiverSubscriberId = "test2@antmedia.io";

        //Get auth token for Ant Media Server to authenticate the user.
        //it's JWT token generated with Subscription Authentication Key(subscriptionAuthenticationKey) in Application settings with subscriberId claim  and it's value.
        //PushNotificationRestService can also be used to generate the authToken
        String authToken = "";

        String pushNotificationToken = AntMediaFirebaseMessagingService.fcmToken;

        String tokenType = "fcm";

        webRTCClient.registerPushNotificationToken(subscriberId, authToken, pushNotificationToken, tokenType);

        webRTCClient.sendPushNotification(subscriberId, authToken, "{\"Caller\":\""+subscriberId+"\",\"StreamId\":\""+streamId+"\"}", receiverSubscriberId);

        askNotificationPermission();
    }

    // [START ask_post_notifications]
    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
