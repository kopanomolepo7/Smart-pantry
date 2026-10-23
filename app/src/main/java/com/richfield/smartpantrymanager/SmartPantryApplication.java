package com.richfield.smartpantrymanager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.richfield.smartpantrymanager.ui.settings.SettingsFragment;

/**
 * Applies the user's saved appearance preference (Settings screen) at
 * process startup, before any Activity is created, so the correct
 * light/dark theme is already active on the very first frame rather
 * than flashing the wrong theme and then switching.
 */
public class SmartPantryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        int savedMode = prefs.getInt(SettingsFragment.KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedMode);
    }
}
