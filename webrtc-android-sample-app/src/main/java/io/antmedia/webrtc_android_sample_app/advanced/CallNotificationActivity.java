package io.antmedia.webrtc_android_sample_app.advanced;

import android.app.Activity;
import android.os.Bundle;

import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtc_android_sample_app.NotificationHelper;
import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;

public class CallNotificationActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_publish);

        SurfaceViewRenderer fullScreenRenderer = findViewById(R.id.full_screen_renderer);

        IWebRTCClient webRTCClient = IWebRTCClient.builder()
                .setActivity(this)
                .setLocalVideoRenderer(fullScreenRenderer)
                .setServerUrl("wss://test.antmedia.io:5443/LiveApp/websocket")
                .build();

        //Define the subscriberId and it can be any subscriber Id
        String subscriberId = "";

        //Define the receiverSubscriberId and it can be any subscriber Id
        String receiverSubscriberId = "";

        //Get auth token for Ant Media Server to authenticate the user.
        //it's JWT token generated with Subscription Authentication Key(subscriptionAuthenticationKey) in Application settings with subscriberId claim  and it's value.
        //PushNotificationRestService can also be used to generate the authToken
        String authToken = "";

        //this is the token get from FCM or APN
        String pushNotificationToken = "";

        String tokenType = ""; //fcm or apn

        webRTCClient.registerPushNotificationToken(subscriberId, authToken, pushNotificationToken, tokenType);

        webRTCClient.sendPushNotification(subscriberId, authToken, "{\"text\":\"This is a test message\"}", receiverSubscriberId);

        NotificationHelper.showCallNotification(this);
    }
}
