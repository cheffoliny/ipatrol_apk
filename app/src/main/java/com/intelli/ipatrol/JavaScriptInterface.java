package com.intelli.ipatrol;

import android.webkit.JavascriptInterface;

public class JavaScriptInterface {
    private MainActivity activity;

    public JavaScriptInterface(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void playSound(String filename, String looping) {
        activity.playSound(filename, looping);
    }

    // ✔ Нов метод — JS извиква Android
    @JavascriptInterface
    public void requestGPS() {
        activity.getLocationForJS();
    }

}
