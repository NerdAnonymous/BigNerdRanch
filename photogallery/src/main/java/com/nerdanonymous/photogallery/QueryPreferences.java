package com.nerdanonymous.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "search_query";
    private static final String PREF_LAST_RESULT_ID = "last_result_id";
    private static final String PREF_IS_SERVICE_ON = "is_service_on";

    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }

    public static void setLastResultId(Context context, String resultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, resultId)
                .apply();
    }

    public static boolean isServiceOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_SERVICE_ON, false);
    }

    public static void setServiceOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_SERVICE_ON, isOn)
                .apply();
    }
}
