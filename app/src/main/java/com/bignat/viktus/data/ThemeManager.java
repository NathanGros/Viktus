package com.bignat.viktus.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static final int SYSTEM = 0;
    public static final int LIGHT = 1;
    public static final int DARK = 2;

    private static final String PREFS = "settings";
    private static final String KEY_THEME = "theme";

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int get(Context c) {
        return prefs(c).getInt(KEY_THEME, SYSTEM);
    }

    // Saves the choice and applies it right away
    public static void set(Context c, int mode) {
        prefs(c).edit().putInt(KEY_THEME, mode).apply();
        apply(mode);
    }

    public static void apply(int mode) {
        switch (mode) {
            case LIGHT: AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO); break;
            case DARK:  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            default:    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}