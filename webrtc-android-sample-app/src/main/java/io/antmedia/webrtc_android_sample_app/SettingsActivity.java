package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

public class SettingsActivity extends Activity {

    public static final String DEFAULT_WEBSOCKET_URL = "wss://test.antmedia.io:5443/LiveApp/websocket";
    public static final String DEFAULT_ROOM_NAME = "room1";

    private Button saveButton;
    private EditText serverAddressEditText;
    private EditText roomNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_settings);

        saveButton = findViewById(R.id.save_button);
        serverAddressEditText = findViewById(R.id.server_address);
        roomNameEditText = findViewById(R.id.room_name);

        SharedPreferences sharedPreferences =
                android.preference.PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverAddressEditText.setText(sharedPreferences.getString(getString(R.string.serverAddress), DEFAULT_WEBSOCKET_URL));
        roomNameEditText.setText(sharedPreferences.getString(getString(R.string.roomId), DEFAULT_ROOM_NAME));

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        String serverWebsocketURL = serverAddressEditText.getText().toString();
        String roomName = roomNameEditText.getText().toString();

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(getString(R.string.serverAddress), serverWebsocketURL);
        editor.putString(getString(R.string.roomId), roomName);
        editor.apply();

        Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
    }

    public static void changeRoomName(Context context, String roomName) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.roomId), roomName);
        editor.apply();
    }
}
