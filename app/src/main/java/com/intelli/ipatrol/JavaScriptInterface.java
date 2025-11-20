package com.intelli.ipatrol;

import android.content.Intent;
import android.webkit.JavascriptInterface;

public class JavaScriptInterface {

    private final MainActivity activity;

    public JavaScriptInterface(MainActivity activity) {
        this.activity = activity;
    }

    // ------------------------------------
    // 1) GPS (старото поведение)
    // ------------------------------------
    @JavascriptInterface
    public void requestGPS() {
        activity.getLocationForJS();
    }

    // ------------------------------------
    // 2) START alarm (foreground service)
    // ------------------------------------
    @JavascriptInterface
    public void startAlarm(String soundFile) {

        if (soundFile == null || soundFile.trim().isEmpty()) {
            soundFile = "alarm";  // raw/alarm.mp3
        } else {
            soundFile = soundFile.replace(".mp3", "");
        }

        Intent i = new Intent(activity, AlarmService.class);
        i.putExtra("sound_file", soundFile);

        activity.startForegroundService(i);
    }

    // ------------------------------------
    // 3) STOP alarm
    // ------------------------------------
    @JavascriptInterface
    public void stopAlarm() {
        Intent i = new Intent(activity, AlarmService.class);
        activity.stopService(i);
    }
}
