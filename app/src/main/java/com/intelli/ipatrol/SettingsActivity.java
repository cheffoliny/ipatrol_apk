package com.intelli.ipatrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private String SERVER1;
    private String SERVER2;
    private String SERVER3;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_SERVER = "selected_server";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ✅ Зареждаме стойностите от strings.xml ТУК, вече имаме достъп до getResources()
        SERVER1 = getString(R.string.server1);
        SERVER2 = getString(R.string.server2);
        SERVER3 = getString(R.string.server3);

        RadioGroup radioGroup = findViewById(R.id.radioGroupServers);
        RadioButton radioServer1 = findViewById(R.id.radioServer1);
        RadioButton radioServer2 = findViewById(R.id.radioServer2);
        RadioButton radioServer3 = findViewById(R.id.radioServer3);
        Button buttonSave = findViewById(R.id.buttonSave);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String selectedServer = prefs.getString(KEY_SELECTED_SERVER, SERVER1);

        if (SERVER1.equals(selectedServer)) {
            radioServer1.setChecked(true);
        } else if (SERVER2.equals(selectedServer)) {
            radioServer2.setChecked(true);
        } else {
            radioServer3.setChecked(true);
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newServer = radioServer1.isChecked() ? SERVER1 : SERVER2;

                if(radioServer3.isChecked()) {
                    newServer = SERVER3;
                }

                prefs.edit().putString(KEY_SELECTED_SERVER, newServer).apply();
                finish();
            }
        });
    }
}