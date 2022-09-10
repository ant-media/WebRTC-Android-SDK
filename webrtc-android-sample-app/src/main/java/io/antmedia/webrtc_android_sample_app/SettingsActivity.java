package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    private Button saveButton;
    private EditText serverAddressEditText;
    private EditText serverPortEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveButton = findViewById(R.id.save_button);
        serverAddressEditText = findViewById(R.id.server_address);
        serverPortEditText = findViewById(R.id.server_port);

        SharedPreferences sharedPreferences =
                android.preference.PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverAddressEditText.setText(sharedPreferences.getString(getString(R.string.serverAddress), "192.168.1.23"));
        serverPortEditText.setText(sharedPreferences.getString(getString(R.string.serverPort), "5080"));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        String serverAddress = serverAddressEditText.getText().toString();
        String serverPort = serverPortEditText.getText().toString();

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.serverAddress), serverAddress);
        editor.putString(getString(R.string.serverPort), serverPort);
        editor.apply();

        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }
}
