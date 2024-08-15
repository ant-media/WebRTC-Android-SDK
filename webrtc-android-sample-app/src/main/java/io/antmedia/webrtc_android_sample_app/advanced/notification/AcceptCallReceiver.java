package io.antmedia.webrtc_android_sample_app.advanced.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;

public class AcceptCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Call accepted.", Toast.LENGTH_SHORT).show();

        NotificationHelper.dismissCallNotification(context);

        /*
        Bundle extras = intent.getExtras();
        if (extras != null) {
            startActivityIntent.putExtras(extras);
        }
         */

        PeerForNotificationActivity.streamId = "streamId";

        Intent callIntent = new Intent(context, PeerForNotificationActivity.class);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }
}

