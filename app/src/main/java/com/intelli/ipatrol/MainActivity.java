package com.intelli.ipatrol;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private String SERVER1;
    private String SERVER2;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_SERVER = "selected_server";
    private static final int LOCATION_REQUEST_CODE = 100;

    private WebView webView;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler;
    private Location lastLocation;
    private MediaPlayer mediaPlayer;
    private String selectedServer;
    private OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Зареждаме стойностите от strings.xml ТУК, вече имаме достъп до getResources()
        SERVER1 = getString(R.string.server1);
        SERVER2 = getString(R.string.server2);

        webView = findViewById(R.id.webView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler(Looper.getMainLooper());

        // Load selected server from prefs
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        selectedServer = prefs.getString(KEY_SELECTED_SERVER, SERVER1);  // Default to SERVER1

        setupWebView();
        requestLocationPermissions();
        startLocationUpdates();
    }
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
        webView.loadUrl(selectedServer);
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, LOCATION_REQUEST_CODE);
            }
        }
    }

    private void startLocationUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getLocationAndSendIfChanged();
                handler.postDelayed(this, 5000);  // Every 5 seconds
            }
        }, 5000);
    }

    private void getLocationAndSendIfChanged() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    if (lastLocation == null || location.distanceTo(lastLocation) > 10) {  // Significant change (>10m)
                        lastLocation = location;
                        sendLocationToApi(location.getLatitude(), location.getLongitude());
                    }
                }
            }
        });
    }

    private void sendLocationToApi(double lat, double lon) {
        String apiUrl = selectedServer + "/api/log_geo.php";
        RequestBody formBody = new FormBody.Builder()
                .add("latitude", String.valueOf(lat))
                .add("longitude", String.valueOf(lon))
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle failure
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Handle success
            }
        });
    }

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
    protected void onResume() {
        super.onResume();
        // Reload WebView if server changed
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String newServer = prefs.getString(KEY_SELECTED_SERVER, SERVER1);
        if (!newServer.equals(selectedServer)) {
            selectedServer = newServer;
            webView.loadUrl(selectedServer);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}