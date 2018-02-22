package com.nerdanonymous.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {

    private static final String TAG = StartupReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isServiceOn(context);
        AlarmService.setService(context, isOn);
    }
}
