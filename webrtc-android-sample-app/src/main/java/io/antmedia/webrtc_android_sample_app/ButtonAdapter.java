package io.antmedia.webrtc_android_sample_app;

import android.content.res.Resources;
import androidx.core.content.res.ResourcesCompat;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;
import io.antmedia.webrtc_android_sample_app.R;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

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