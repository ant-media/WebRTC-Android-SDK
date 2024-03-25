package io.antmedia.webrtc_android_sample_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import io.antmedia.webrtc_android_sample_app.basic.PeerActivity;

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

        Intent callIntent = new Intent(context, PeerActivity.class);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);
    }
}

