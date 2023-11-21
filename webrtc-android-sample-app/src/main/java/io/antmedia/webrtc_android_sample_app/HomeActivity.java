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

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        list = findViewById(R.id.list);
        createList();
        setListAdapter(activities);
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
        activities.add(new ActivityLink(new Intent(this, TrackBasedConferenceActivity.class),
                "Multitrack Conference"));
        activities.add(new ActivityLink(new Intent(this, MP4PublishActivity.class),
                "MP4 Publish"));
    }

    private void setListAdapter(List<ActivityLink> activities) {
        list.setAdapter(new ButtonAdapter(activities));
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        ActivityLink link = activities.get(i);
        startActivity(link.getIntent());
    }
}
