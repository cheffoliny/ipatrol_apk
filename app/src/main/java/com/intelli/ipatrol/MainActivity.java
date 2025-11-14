package com.intelli.ipatrol;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_SERVER = "selected_server";
    private static final int LOCATION_REQUEST_CODE = 100;

    private WebView webView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Handler handler;
    private MediaPlayer mediaPlayer;
    private String selectedServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // servers from strings.xml (as you already had)
        String SERVER1 = getString(R.string.server1);
        String SERVER2 = getString(R.string.server2);
        String SERVER3 = getString(R.string.server3);

        webView = findViewById(R.id.webView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());

        // Load selected server from prefs
        selectedServer = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SELECTED_SERVER, SERVER3);

        setupWebView();
        createLocationRequest();
        createLocationCallback();
        requestLocationPermissions();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        webView.loadUrl(selectedServer);
    }

    private void createLocationRequest() {
        // high frequency suitable for vehicle tracking; adjust intervals as needed
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)         // desired interval: 5s
                .setFastestInterval(2000); // fastest acceptable: 2s
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                // send last/fresh location to JS for each update
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    sendLocationToJS(location);
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If user changed server in settings, reload
        selectedServer = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SELECTED_SERVER, selectedServer);
        webView.loadUrl(selectedServer);

        // start updates (if permission granted)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        LOCATION_REQUEST_CODE);
            }
        } else {
            // already granted
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                // Permission denied - you might want to alert user
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // sends location to JS callback: window.receiveGPS(lat, lng, acc, speed, bearing, altitude)
    private void sendLocationToJS(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        float accuracy = location.hasAccuracy() ? location.getAccuracy() : -1f;
        float speed = location.hasSpeed() ? location.getSpeed() : -1f;
        float bearing = location.hasBearing() ? location.getBearing() : 0f;
        double altitude = location.hasAltitude() ? location.getAltitude() : -1d;

        // Ensure numeric formattable string with dot decimal separator
        DecimalFormat df = new DecimalFormat("0.000000000000");
        String js = String.format("window.receiveGPS(%s, %s, %s, %s, %s, %s);",
                df.format(lat), df.format(lng),
                Float.toString(accuracy), Float.toString(speed),
                Float.toString(bearing), Double.toString(altitude));

        runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    // Public method used by JavaScriptInterface.requestGPS()
    public void getLocationForJS() {
        // single-shot: try to get last known location and send it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                sendLocationToJS(location);
            }
        });
    }

    // Media player control (kept as you had it)
    public void playSound(String filename, String looping) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if ("stop".equals(looping)) {
            return;
        }

        int resId = getResources().getIdentifier(filename.replace(".mp3", ""), "raw", getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(this, resId);
            mediaPlayer.setLooping("true".equals(looping));
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
