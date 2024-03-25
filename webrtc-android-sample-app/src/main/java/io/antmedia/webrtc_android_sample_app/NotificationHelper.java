package io.antmedia.webrtc_android_sample_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static NotificationManager notificationManager = null;

    private static String callerName = "John Doe";

    private static String roomName = "Room 1";

    public static void setCallerName(String name) {
        callerName = name;
    }

    public static void setRoomName(String name) {
        roomName = name;
    }

    public static void showCallNotification(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "call_notifications";
        String channelName = "Call Notifications";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Intent for the accept action
        Intent acceptIntent = new Intent(context, AcceptCallReceiver.class);
        PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(context, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent for the decline action
        Intent declineIntent = new Intent(context, DeclineCallReceiver.class);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(context, 0, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_loopback_call)
                .setContentTitle("Incoming Call")
                .setContentText(callerName+" is calling...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .addAction(R.drawable.ic_loopback_call, "Answer", acceptPendingIntent)
                .addAction(R.drawable.disconnect, "Decline", declinePendingIntent);

        notificationManager.notify(1, builder.build());
    }

    public static void dismissCallNotification(Context context) {
        if (notificationManager != null) {
            notificationManager.cancel(1);
        }
    }
}
