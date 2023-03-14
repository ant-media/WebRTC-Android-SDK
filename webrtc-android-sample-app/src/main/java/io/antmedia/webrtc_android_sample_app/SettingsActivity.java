package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    public static final String DEFAULT_SERVER_ADDRESS = "test.antmedia.io";
    public static final String DEFAULT_SERVER_PORT = "5080";
    public static final String DEFAULT_APP_NAME = "LiveApp";
    public static final String DEFAULT_ROOM_NAME = "room1";

    private Button saveButton;
    private EditText serverAddressEditText;
    private EditText serverPortEditText;
    private EditText applicationNameEditText;
    private EditText roomNameEditText;
    private EditText bitrateEditText;
    private EditText fpsEditText;
    private CheckBox viewerOnlyRadioButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveButton = findViewById(R.id.save_button);
        serverAddressEditText = findViewById(R.id.server_address);
        serverPortEditText = findViewById(R.id.server_port);
        applicationNameEditText = findViewById(R.id.application_name);
        roomNameEditText = findViewById(R.id.room_name);
        bitrateEditText = findViewById(R.id.bitrate);
        fpsEditText = findViewById(R.id.fps);
        viewerOnlyRadioButton = findViewById(R.id.viewer_only);



        SharedPreferences sharedPreferences =
                android.preference.PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverAddressEditText.setText(sharedPreferences.getString(getString(R.string.serverAddress), DEFAULT_SERVER_ADDRESS));
        serverPortEditText.setText(sharedPreferences.getString(getString(R.string.serverPort), DEFAULT_SERVER_PORT));
        applicationNameEditText.setText(sharedPreferences.getString(getString(R.string.app_name), DEFAULT_APP_NAME));
        roomNameEditText.setText(sharedPreferences.getString(getString(R.string.roomId), DEFAULT_ROOM_NAME));
        bitrateEditText.setText(sharedPreferences.getString(getString(R.string.bitrate), "1500"));
        fpsEditText.setText(sharedPreferences.getString(getString(R.string.fps), "30"));
        String isVieverOnly = sharedPreferences.getString(getString(R.string.viewer_only), "false");
        viewerOnlyRadioButton.setChecked(isVieverOnly.equals("true"));


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
        String applicationName = applicationNameEditText.getText().toString();
        String roomName = roomNameEditText.getText().toString();
        String bitrate = bitrateEditText.getText().toString();
        String fps = fpsEditText.getText().toString();
        boolean isViewerOnly = viewerOnlyRadioButton.isChecked();

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.serverAddress), serverAddress);
        editor.putString(getString(R.string.serverPort), serverPort);
        editor.putString(getString(R.string.app_name), applicationName);
        editor.putString(getString(R.string.roomId), roomName);
        editor.putString(getString(R.string.bitrate), bitrate);
        editor.putString(getString(R.string.fps), fps);
        editor.putString(getString(R.string.viewer_only), isViewerOnly+"");


        editor.apply();

        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }
}
