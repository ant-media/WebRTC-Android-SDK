package io.antmedia.webrtc_android_sample_app.chat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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

        Message message = messages.get(i);
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        TextView messageDate;
        if(message instanceof  TextMessage) {
            TextView messageBody;

            TextMessage textMessage = (TextMessage) message;
            if (message.isBelongsToCurrentUser()) {
                // this message was sent by us
                convertView = messageInflater.inflate(R.layout.my_message, null);
                messageBody = convertView.findViewById(R.id.message_body);
                messageDate = convertView.findViewById(R.id.message_date);
                messageDate.setText(textMessage.getMessageDate());
                messageBody.setText(textMessage.getMessageBody());
            } else {
                // this message was sent by someone else
                convertView = messageInflater.inflate(R.layout.their_message, null);
                messageBody = convertView.findViewById(R.id.message_body);
                messageDate = convertView.findViewById(R.id.message_date);
                messageDate.setText(textMessage.getMessageDate());
                messageBody.setText(textMessage.getMessageBody());
            }
        } else {

            ImageMessage imageMessage = (ImageMessage) message;
            ImageView imageBody;


            if (message.isBelongsToCurrentUser()) {
                convertView = messageInflater.inflate(R.layout.my_image_message, null);
                // this message was sent by us
                imageBody = convertView.findViewById(R.id.image_body_my);
                messageDate = convertView.findViewById(R.id.message_date);
                messageDate.setText(imageMessage.getMessageDate());
                imageBody.setImageBitmap(imageMessage.getImageBitmap());
            } else {
                convertView = messageInflater.inflate(R.layout.their_image_message, null);
                // this message was sent by someone else
                imageBody = convertView.findViewById(R.id.image_body_their);
                messageDate = convertView.findViewById(R.id.message_date);
                messageDate.setText(imageMessage.getMessageDate());
                imageBody.setImageBitmap(imageMessage.getImageBitmap());
            }
        }


        return convertView;
    }
}



