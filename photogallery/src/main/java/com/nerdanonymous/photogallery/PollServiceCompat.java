package com.nerdanonymous.photogallery;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class PollServiceCompat extends IntentService {

    private static final String TAG = PollServiceCompat.class.getSimpleName();
    private static final int NOTIFY_ID = 1;
    private static final String NOTIFY_CHANEL_ID = PollServiceCompat.class.getName();
    private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static final int JOB_SERVICE_ID = 1;

    public static Intent newIntent(Context context) {
        return new Intent(context, PollServiceCompat.class);
    }

    public static void setService(Context context, boolean isOn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setJobService(context, isOn);
        } else {
            setAlarmService(context, isOn);
        }
    }

    public static void setAlarmService(Context context, boolean isOn) {
        Intent service = PollServiceCompat.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, service, 0);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(ALARM_SERVICE);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL, pendingIntent);
        } else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void setJobService(Context context, boolean isOn) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (isOn) {
            JobInfo.Builder jobInfo = new JobInfo.Builder(JOB_SERVICE_ID, new ComponentName(context, PollService.class));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                jobInfo.setPeriodic(JobInfo.getMinPeriodMillis());
            } else {
                jobInfo.setPeriodic(3000);
            }
            scheduler.schedule(jobInfo.build());
        } else {
            List<JobInfo> schedulerJobs = scheduler.getAllPendingJobs();
            for (JobInfo jobInfo : schedulerJobs) {
                if (JOB_SERVICE_ID == jobInfo.getId()) {
                    scheduler.cancel(JOB_SERVICE_ID);
                    break;
                }
            }
        }
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent service = PollServiceCompat.newIntent(context);
        PendingIntent pendingIntent = PendingIntent
                .getService(context, 0, service, PendingIntent.FLAG_NO_CREATE);
        return null != pendingIntent;
    }

    public PollServiceCompat() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        if (TextUtils.isEmpty(query)) {
            items = new FlickrFetcher().fetchRecentPhotos(1);
        } else {
            items = new FlickrFetcher().searchPhotos(query);
        }

        if (null == items || items.isEmpty()) {
            return;
        }

        String resultId = items.get(0).getId();
        if (TextUtils.equals(resultId, lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources();
            Intent photoGalleryIntent = PhotoGalleryActivity.newIntent(this);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, photoGalleryIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, NOTIFY_CHANEL_ID)
                    .setTicker(resources.getString(R.string.app_name))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.app_name))
                    .setContentText(resources.getString(R.string.app_name))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(NOTIFY_CHANEL_ID,
                        NOTIFY_CHANEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            notificationManager.notify(NOTIFY_ID, notification);
        }

        QueryPreferences.setLastResultId(this, resultId);
    }

    private boolean isNetworkAvailableConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        return isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }
}
