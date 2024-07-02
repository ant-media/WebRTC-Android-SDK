package io.antmedia.webrtc_android_sample_app.advanced.notification;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AntMediaFirebaseMessagingService extends FirebaseMessagingService {

        private static final String TAG = "AntMediaFMS";

        public static String fcmToken = "";

        @Override
        public void onMessageReceived(RemoteMessage remoteMessage) {
                Log.d(TAG, "From: " + remoteMessage.getFrom());

                // Check if message contains a notification payload.
                if (remoteMessage.getNotification() != null) {
                        Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
                }

                // show call notification
                NotificationHelper.showCallNotification(this);
        }

        /**
         * There are two scenarios when onNewToken is called:
         * 1) When a new token is generated on initial app startup
         * 2) Whenever an existing token is changed
         * Under #2, there are three scenarios when the existing token is changed:
         * A) App is restored to a new device
         * B) User uninstalls/reinstalls the app
         * C) User clears app data
         */
        @Override
        public void onNewToken(@NonNull String token) {
                Log.d(TAG, "Refreshed token: " + token);

                fcmToken = token;
        }
}