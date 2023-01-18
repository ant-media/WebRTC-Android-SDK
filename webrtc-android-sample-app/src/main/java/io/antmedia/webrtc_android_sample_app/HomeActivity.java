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

public class HomeActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private List<ActivityLink> activities;
    private GridView list;

    private final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        list = findViewById(R.id.list);
        createList();
        setListAdapter(activities);

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
        }
    }

    private void createList() {
        activities = new ArrayList<>();
        activities.add(new ActivityLink(new Intent(this, MainActivity.class),
                "Default Camera"));
        activities.add(new ActivityLink(new Intent(this, MultiTrackPlayActivity.class),
                "Multi Track Play"));
        activities.add(new ActivityLink(new Intent(this, DataChannelActivity.class),
                "Data Channel "));
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

    private boolean hasPermissions(Context context, String... permissions) {
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
        if (hasPermissions(this, PERMISSIONS)) {
            ActivityLink link = activities.get(i);
            startActivity(link.getIntent());
        } else {
            showPermissionsErrorAndRequest();
        }
    }

    private void showPermissionsErrorAndRequest() {
        Toast.makeText(this, "You need permissions before", Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1);
    }

}
