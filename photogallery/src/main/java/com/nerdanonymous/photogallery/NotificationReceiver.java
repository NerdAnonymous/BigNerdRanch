package com.nerdanonymous.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String NOTIFY_CHANEL_ID = AlarmService.class.getName();
    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "receiver result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {
            return;
        }

        int requestCode = intent.getIntExtra(AlarmService.REQUEST_CODE, 0);
        Notification notification = intent.getParcelableExtra(AlarmService.NOTIFICATION);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFY_CHANEL_ID,
                    NOTIFY_CHANEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(requestCode, notification);
    }
}
