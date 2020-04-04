package io.antmedia.webrtc_android_sample_app.chat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.antmedia.webrtc_android_sample_app.R;

public class MessageAdapter extends BaseAdapter {
    List<Message> messages = new ArrayList<Message>();
    Context context;
    int colorCodeForReceivedMessage;

    public MessageAdapter(Context context) {
        this.context = context;
        colorCodeForReceivedMessage = Color.parseColor("#FFFFFF00");
    }

    public void add(Message message) {
        this.messages.add(message);
        notifyDataSetChanged(); // to render the list we need to notify
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    // This is the backbone of the class, it handles the creation of single ListView row (chat bubble)
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        TextView messageBody;
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Message message = messages.get(i);

        if (message.isBelongsToCurrentUser()) { // this message was sent by us so let's create a basic chat bubble on the right
            convertView = messageInflater.inflate(R.layout.my_message, null);
            messageBody = convertView.findViewById(R.id.message_body);
            messageBody.setText(message.getText());
        } else { // this message was sent by someone else so let's create an advanced chat bubble on the left
            convertView = messageInflater.inflate(R.layout.their_message, null);
            messageBody = convertView.findViewById(R.id.message_body);
            messageBody.setText(message.getText());
        }

        return convertView;
    }
}



