package com.example.myapp.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class SettingsStore {
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_AUTO_REROUTE = "auto_reroute";

    private SettingsStore() {
    }

    public static boolean isAutoRerouteEnabled(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_REROUTE, true);
    }

    public static void setAutoRerouteEnabled(@NonNull Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_REROUTE, enabled).apply();
    }
}
