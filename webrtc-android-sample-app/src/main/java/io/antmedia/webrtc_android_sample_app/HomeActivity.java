package io.antmedia.webrtc_android_sample_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import io.antmedia.webrtcandroidframework.IWebRTCClient;

public class HomeActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {


    private List<ActivityLink> activities;
    private GridView list;


    public static final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
        new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT}
            :
        new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        list = findViewById(R.id.list);
        createList();
        setListAdapter(activities);

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, 1);
        }
    }

    private void createList() {
        activities = new ArrayList<>();
        activities.add(new ActivityLink(new Intent(this, MainActivity.class),
                "Default Camera"));
        activities.add(new ActivityLink(new Intent(this, MainActivity.class).putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_PLAY),
                "Simple Play"));
        activities.add(new ActivityLink(new Intent(this, MainActivity.class).putExtra(MainActivity.WEBRTC_MODE, IWebRTCClient.MODE_JOIN),
                "P2P"));
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
        activities.add(new ActivityLink(new Intent(this, MultitrackConferenceActivity.class),
                "Multitrack Conference"));
    }

    private void setListAdapter(List<ActivityLink> activities) {
        list.setAdapter(new ButtonAdapter(activities));
        list.setOnItemClickListener(this);
    }

    public boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(HomeActivity.class.getSimpleName(), "Permission required:"+permission);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
            ActivityLink link = activities.get(i);
            startActivity(link.getIntent());
        } else {
            showPermissionsErrorAndRequest();
        }
    }

    private void showPermissionsErrorAndRequest() {
        Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 1);
    }

}
