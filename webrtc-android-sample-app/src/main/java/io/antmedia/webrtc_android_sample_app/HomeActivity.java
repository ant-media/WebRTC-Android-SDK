package io.antmedia.webrtc_android_sample_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import io.antmedia.webrtcandroidframework.IWebRTCClient;

public class HomeActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {


    private List<ActivityLink> activities;
    private GridView list;

    public static final String[] PERMISSIONS_UNDER_ANDROID_S = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT
    };

    public static final String[] PERMISSIONS_BELOW_ANDROID_S = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        list = findViewById(R.id.list);
        createList();
        setListAdapter(activities);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermissions(this, PERMISSIONS_UNDER_ANDROID_S)) {
                requestPermissions(PERMISSIONS_UNDER_ANDROID_S, 1);
            }
        } else {
            if (!hasPermissions(this, PERMISSIONS_BELOW_ANDROID_S)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_BELOW_ANDROID_S, 1);
            }
        }

    }

    private void createList() {
        activities = new ArrayList<>();
        activities.add(new ActivityLink(new Intent(this, MainActivity.class),
                "Default Camera"));
        activities.add(new ActivityLink(new Intent(this, MainActivity.class).putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_PLAY),
                "Simple Play"));
        activities.add(new ActivityLink(new Intent(this, MultiTrackPlayActivity.class),
                "Multi Track Play"));
        activities.add(new ActivityLink(new Intent(this, DataChannelOnlyActivity.class),
                "Data Channel Only Activity"));
        activities.add(new ActivityLink(new Intent(this, ConferenceActivity.class),
                "Conference"));
        activities.add(new ActivityLink(new Intent(this, ScreenCaptureActivity.class),
                "Screen Capture"));
        activities.add(new ActivityLink(new Intent(this, SettingsActivity.class),
                "Settings"));
    }

    private void setListAdapter(List<ActivityLink> activities) {
        list.setAdapter(new ButtonAdapter(activities));
        list.setOnItemClickListener(this);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (hasPermissions(this, PERMISSIONS_UNDER_ANDROID_S)) {
                ActivityLink link = activities.get(i);
                startActivity(link.getIntent());
            } else {
                showPermissionsErrorAndRequest();
            }
        } else {
            if (hasPermissions(this, PERMISSIONS_BELOW_ANDROID_S)) {
                ActivityLink link = activities.get(i);
                startActivity(link.getIntent());
            } else {
                showPermissionsErrorAndRequest();
            }
        }
    }

    private void showPermissionsErrorAndRequest() {
        Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_UNDER_ANDROID_S, 1);
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_BELOW_ANDROID_S, 1);
        }
    }

}
