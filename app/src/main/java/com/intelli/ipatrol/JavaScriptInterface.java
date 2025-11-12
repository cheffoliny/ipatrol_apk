package com.intelli.ipatrol;

import android.content.Context;

public class JavaScriptInterface {
    private MainActivity activity;

    public JavaScriptInterface(MainActivity activity) {
        this.activity = activity;
    }

    @android.webkit.JavascriptInterface
    public void playSound(String filename, String looping) {
        activity.playSound(filename, looping);
    }
}