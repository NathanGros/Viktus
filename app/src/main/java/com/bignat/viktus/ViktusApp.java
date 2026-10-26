package com.bignat.viktus;

import android.app.Application;

import com.bignat.viktus.data.ThemeManager;

public class ViktusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.apply(ThemeManager.get(this));
    }
}