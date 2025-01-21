package io.antmedia.webrtc_android_sample_app;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

import io.antmedia.webrtc_android_sample_app.advanced.ConferenceActivityWithDifferentVideoSources;
import io.antmedia.webrtc_android_sample_app.advanced.notification.CallNotificationActivity;
import io.antmedia.webrtc_android_sample_app.advanced.ConferenceActivityWithSpeakerIndicator;
import io.antmedia.webrtc_android_sample_app.advanced.MP3PublishActivity;
import io.antmedia.webrtc_android_sample_app.advanced.MP4PublishActivity;
import io.antmedia.webrtc_android_sample_app.advanced.MP4PublishWithSurfaceActivity;
import io.antmedia.webrtc_android_sample_app.advanced.MultiTrackPlayActivity;
import io.antmedia.webrtc_android_sample_app.advanced.PublishActivityWithAreYouSpeaking;
import io.antmedia.webrtc_android_sample_app.advanced.USBCameraActivity;
import io.antmedia.webrtc_android_sample_app.basic.ConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.DataChannelOnlyActivity;
import io.antmedia.webrtc_android_sample_app.basic.DynamicConferenceActivity;
import io.antmedia.webrtc_android_sample_app.basic.PeerActivity;
import io.antmedia.webrtc_android_sample_app.basic.PlayActivity;
import io.antmedia.webrtc_android_sample_app.basic.PublishActivity;
import io.antmedia.webrtc_android_sample_app.basic.ScreenCaptureActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtc_android_sample_app.basic.StatsActivity;
import io.antmedia.webrtc_android_sample_app.basic.WebviewActivity;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    final private List<ActivityLink> activities = new ArrayList<>();
    private GridView list;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        list = findViewById(R.id.list);
        createList();
        setListAdapter(activities);

        FirebaseApp.initializeApp(this);
    }

    private void createList() {
        addActivity(PublishActivity.class, "Publish");
        addActivity(PlayActivity.class, "Play");
        addActivity(PeerActivity.class, "Peer");
        addActivity(DynamicConferenceActivity.class, "Dynamic Conference");
        addActivity(ConferenceActivity.class, "Conference");
        addActivity(ScreenCaptureActivity.class, "Screen Share");
        addActivity(DataChannelOnlyActivity.class, "DC Only");
        addActivity(MP3PublishActivity.class, "mp3");
        addActivity(MP4PublishActivity.class, "mp4");
        addActivity(MP4PublishWithSurfaceActivity.class, "mp4 with Surface");
        addActivity(USBCameraActivity.class, "USB Camera");
        addActivity(MultiTrackPlayActivity.class, "Multi Track");
        addActivity(CallNotificationActivity.class, "Call Notification");
        addActivity(SettingsActivity.class, "Settings");
        addActivity(ConferenceActivityWithSpeakerIndicator.class,"Conference with Speaking Indicator");
        addActivity(ConferenceActivityWithDifferentVideoSources.class,"Conference with Stream Source Switch");
        addActivity(PublishActivityWithAreYouSpeaking.class, "Publish with Are You Speaking");
        addActivity(StatsActivity.class, "Stats");
        addActivity(WebviewActivity.class, "Webview");

    }

    private void addActivity(Class<?> cls, String label) {
        activities.add(new ActivityLink(new Intent(this, cls), label));
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

    public class ActivityLink {
        private final String label;
        private final Intent intent;

        public ActivityLink(Intent intent, String label) {
            this.intent = intent;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public Intent getIntent() {
            return intent;
        }

    }

    public class ButtonAdapter extends BaseAdapter {

        private List<ActivityLink> links;

        public ButtonAdapter(List<ActivityLink> links) {
            this.links = links;
        }

        public int getCount() {
            return links.size();
        }

        public ActivityLink getItem(int position) {
            return links.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView button;
            Resources resources = parent.getResources();
            if (convertView == null) {
                button = new TextView(parent.getContext());
                button.setLayoutParams(new GridView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
                button.setGravity(Gravity.CENTER);
                button.setPadding(8, 48, 8, 48);
                button.setTextColor(ResourcesCompat.getColor(resources, R.color.textColor, null));
                button.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null));
                convertView = button;
            } else {
                button = (TextView) convertView;
            }
            button.setText(links.get(position).getLabel());
            return convertView;
        }
    }
}
