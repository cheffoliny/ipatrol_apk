package com.intelli.ipatrol;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
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

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_SERVER = "selected_server";
    private static final int LOCATION_REQUEST_CODE = 100;

    private WebView webView;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Handler handler;
    private String selectedServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // servers from strings.xml
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
        webSettings.setMediaPlaybackRequiresUserGesture(false); // Allows autoplay from web
        webSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        webView.loadUrl(selectedServer);
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)         // 5 s
                .setFastestInterval(2000); // 2 s
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                // send continuous updates if needed
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
        selectedServer = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SELECTED_SERVER, selectedServer);
        webView.loadUrl(selectedServer);

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
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    /** Called from JavaScript via Android.requestGPS() */
    public void getLocationForJS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                float accuracy  = location.hasAccuracy() ? location.getAccuracy() : -1f;
                float speed     = location.hasSpeed() ? location.getSpeed() : -1f;
                float bearing   = location.hasBearing() ? location.getBearing() : -1f;
                double altitude = location.hasAltitude() ? location.getAltitude() : -1d;

                try {
                    JSONObject json = new JSONObject();
                    json.put("lat", lat);
                    json.put("lng", lng);
                    json.put("accuracy", accuracy);
                    json.put("speed", speed);
                    json.put("bearing", bearing);
                    json.put("altitude", altitude);

                    String safe = JSONObject.quote(json.toString());
                    String js = "window.receiveGPSJSON(" + safe + ");";

                    runOnUiThread(() -> webView.evaluateJavascript(js, null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}