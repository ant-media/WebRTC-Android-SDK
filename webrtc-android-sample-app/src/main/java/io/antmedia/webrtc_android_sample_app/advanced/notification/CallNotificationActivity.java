package io.antmedia.webrtc_android_sample_app.advanced.notification;


import android.content.SharedPreferences;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.FirebaseApp;

public class CallNotificationActivity extends ComponentActivity {

    String streamId;

    String subscriberId;

    String receiverSubscriberId;

    String authToken;

    String pushNotificationToken;

    String tokenType;

    JSONObject pushNotificationContent;

    JSONArray receiverSubscriberIdArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);

        FirebaseApp.initializeApp(this);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        //FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);
        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        IWebRTCClient webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setInitiateBeforeStream(true)
                .setWebRTCListener(createWebRTCListener())
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl(serverUrl)
                .build();

        streamId = "streamId" + (int)(Math.random()*9999);

        PeerForNotificationActivity.streamId = streamId;

        //Define the subscriberId and it can be any subscriber Id
        subscriberId = "test1@antmedia.io";

        //Define the receiverSubscriberId and it can be any subscriber Id
        receiverSubscriberId = "test2@antmedia.io";

        //Get auth token for Ant Media Server to authenticate the user.
        //it's JWT token generated with Subscription Authentication Key(subscriptionAuthenticationKey) in Application settings with subscriberId claim  and it's value.
        //PushNotificationRestService can also be used to generate the authToken
        authToken = "";

        pushNotificationToken = AntMediaFirebaseMessagingService.fcmToken;

        tokenType = "fcm";

        pushNotificationContent = new JSONObject();
        receiverSubscriberIdArray = new JSONArray();

        try {
            pushNotificationContent.put("Caller", subscriberId);
            pushNotificationContent.put("StreamId", streamId);
            receiverSubscriberIdArray.put(receiverSubscriberId);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        askNotificationPermission();
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onWebSocketConnected() {
                super.onWebSocketConnected();
                webRTCClient.registerPushNotificationToken(subscriberId, authToken, pushNotificationToken, tokenType);
                webRTCClient.sendPushNotification(subscriberId, authToken, pushNotificationContent, receiverSubscriberIdArray
                );
            }

        };
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
