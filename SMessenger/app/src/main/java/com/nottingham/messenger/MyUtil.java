package com.nottingham.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Me on 23/02/2017.
 */
public class MyUtil {

//    public static final String ESTABLISHING = "ESTABLISHING";
//    private static final String EMPTY = "";
    public static final String SETUP_COMPLETED = "Setup Completed";

    public static void clearSharedPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    public static boolean isSetupCompleted(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(SETUP_COMPLETED, false);
    }

    public static void setSetupCompleted(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(SETUP_COMPLETED, true).commit();
     }
}
