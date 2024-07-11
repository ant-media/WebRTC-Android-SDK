package io.antmedia.webrtc_android_sample_app.basic.stats;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtcandroidframework.core.StatsCollector;
import io.antmedia.webrtcandroidframework.core.model.TrackStats;

public class TrackStatsAdapter extends RecyclerView.Adapter<TrackStatsAdapter.TrackStatsViewHolder> {

    private ArrayList<TrackStats> items;
    private Activity activity;

    public TrackStatsAdapter(ArrayList<TrackStats> items, Activity activity) {
        this.items = items;
        this.activity = activity;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public TrackStatsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.track_stats_item, parent, false);
        return new TrackStatsViewHolder(view);
    }

    private void hideVideoStatFields(TrackStatsViewHolder holder){

        holder.firCountContainer.setVisibility(View.GONE);
        holder.pliCountContainer.setVisibility(View.GONE);
        holder.nackCountContainer.setVisibility(View.GONE);
        holder.jitterContainer.setVisibility(View.GONE);
        holder.packetLostContainer.setVisibility(View.GONE);
        holder.packetsReceivedContainer.setVisibility(View.GONE);
        holder.bytesReceivedContainer.setVisibility(View.GONE);
        holder.framesEncodedContainer.setVisibility(View.GONE);
        holder.framesDecodedContainer.setVisibility(View.GONE);
        holder.framesReceivedContainer.setVisibility(View.GONE);
        holder.framesDroppedContainer.setVisibility(View.GONE);
        holder.totalFreezeDurationContainer.setVisibility(View.GONE);

    }

    private void showVideoStatFields(TrackStatsViewHolder holder){
        holder.firCountContainer.setVisibility(View.VISIBLE);
        holder.pliCountContainer.setVisibility(View.VISIBLE);
        holder.nackCountContainer.setVisibility(View.VISIBLE);
        holder.jitterContainer.setVisibility(View.VISIBLE);
        holder.packetLostContainer.setVisibility(View.VISIBLE);
        holder.packetsReceivedContainer.setVisibility(View.VISIBLE);
        holder.bytesReceivedContainer.setVisibility(View.VISIBLE);
        holder.framesEncodedContainer.setVisibility(View.VISIBLE);
        holder.framesDecodedContainer.setVisibility(View.VISIBLE);
        holder.framesReceivedContainer.setVisibility(View.VISIBLE);
        holder.framesDroppedContainer.setVisibility(View.VISIBLE);
        holder.totalFreezeDurationContainer.setVisibility(View.VISIBLE);


    }

    private void showAudioStatFields(TrackStatsViewHolder holder){

        holder.packetLostContainer.setVisibility(View.VISIBLE);
        holder.jitterContainer.setVisibility(View.VISIBLE);
        holder.rttContainer.setVisibility(View.VISIBLE);
        holder.concealmentEventsContainer.setVisibility(View.VISIBLE);

    }

    private void hideAudioStatFields(TrackStatsViewHolder holder){

        holder.packetLostContainer.setVisibility(View.GONE);
        holder.jitterContainer.setVisibility(View.GONE);
        holder.rttContainer.setVisibility(View.GONE);
        holder.concealmentEventsContainer.setVisibility(View.GONE);

    }


    @Override
    public void onBindViewHolder(TrackStatsViewHolder holder, int position) {
        TrackStats item = items.get(position);

        holder.trackIdText.setText(item.getTrackId());

        if(item.getTrackId().contains(StatsCollector.AUDIO)){

            hideVideoStatFields(holder);
            showAudioStatFields(holder);

            holder.packetLostText.setText(String.valueOf(item.getPacketsLost()));
            holder.jitterText.setText(String.valueOf(item.getJitter()));
            holder.rttText.setText(String.valueOf(item.getRoundTripTime()));
            holder.concealmentEventsText.setText(String.valueOf(item.getConcealmentEvents()));



        } else if (item.getTrackId().contains(StatsCollector.VIDEO)) {
            hideAudioStatFields(holder);
            showVideoStatFields(holder);

            holder.packetLostText.setText(String.valueOf(item.getPacketsLost()));
            holder.jitterText.setText(String.valueOf(item.getJitter()));
            holder.rttText.setText(String.valueOf(item.getRoundTripTime()));
            holder.concealmentEventsText.setText(String.valueOf(item.getConcealmentEvents()));
            holder.firCountText.setText(String.valueOf(item.getFirCount()));
            holder.pliCountText.setText(String.valueOf(item.getPliCount()));
            holder.nackCountText.setText(String.valueOf(item.getNackCount()));
            holder.packetsReceivedText.setText(String.valueOf(item.getPacketsReceived()));
            holder.bytesReceivedText.setText(String.valueOf(item.getBytesReceived()));
            holder.framesEncodedText.setText(String.valueOf(item.getFramesEncoded()));
            holder.framesDecodedText.setText(String.valueOf(item.getFramesDecoded()));
            holder.framesReceivedText.setText(String.valueOf(item.getFramesReceived()));
            holder.framesDroppedText.setText(String.valueOf(item.getFramesDropped()));
            holder.totalFreezeDurationText.setText(String.valueOf(item.getTotalFreezesDuration()));

        }

    }

    public class TrackStatsViewHolder extends RecyclerView.ViewHolder {

        LinearLayout trackIdContainer;
        LinearLayout packetLostContainer;
        LinearLayout jitterContainer;
        LinearLayout rttContainer;
        LinearLayout concealmentEventsContainer;
        LinearLayout firCountContainer;
        LinearLayout pliCountContainer;
        LinearLayout nackCountContainer;
        LinearLayout packetsReceivedContainer;
        LinearLayout bytesReceivedContainer;
        LinearLayout framesEncodedContainer;
        LinearLayout framesDecodedContainer;
        LinearLayout framesReceivedContainer;
        LinearLayout framesDroppedContainer;
        LinearLayout totalFreezeDurationContainer;

        TextView trackIdText;
        TextView packetLostText;
        TextView jitterText;
        TextView rttText;
        TextView concealmentEventsText;
        TextView firCountText;
        TextView pliCountText;
        TextView nackCountText;
        TextView packetsReceivedText;
        TextView bytesReceivedText;
        TextView framesEncodedText;
        TextView framesDecodedText;
        TextView framesReceivedText;
        TextView framesDroppedText;
        TextView totalFreezeDurationText;



        public TrackStatsViewHolder(View view) {
            super(view);
            trackIdContainer = view.findViewById(R.id.track_stats_item_track_id_container);
            packetLostContainer = view.findViewById(R.id.track_stats_item_packet_lost_container);
             jitterContainer = view.findViewById(R.id.track_stats_item_jitter_container);
             rttContainer = view.findViewById(R.id.track_stats_item_rtt_container);
             concealmentEventsContainer = view.findViewById(R.id.track_stats_item_concealment_events_container);
             firCountContainer = view.findViewById(R.id.track_stats_item_fir_count_container);
             pliCountContainer = view.findViewById(R.id.track_stats_item_pli_count_container);
             nackCountContainer = view.findViewById(R.id.track_stats_item_nack_count_container);
             packetsReceivedContainer = view.findViewById(R.id.track_stats_item_packets_received_container);
             bytesReceivedContainer = view.findViewById(R.id.track_stats_item_bytes_received_container);
             framesEncodedContainer = view.findViewById(R.id.track_stats_item_frames_encoded_container);
             framesDecodedContainer = view.findViewById(R.id.track_stats_item_frames_decoded_container);
             framesReceivedContainer = view.findViewById(R.id.track_stats_item_frames_received_container);
             framesDroppedContainer = view.findViewById(R.id.track_stats_item_frames_dropped_container);
             totalFreezeDurationContainer = view.findViewById(R.id.track_stats_item_total_freeze_duration_container);
            trackIdText = view.findViewById(R.id.track_stats_item_track_id_textview);
            packetLostText = view.findViewById(R.id.track_stats_item_packet_lost_textview);
            jitterText = view.findViewById(R.id.track_stats_item_jitter_textview);
            rttText = view.findViewById(R.id.track_stats_item_rtt_textview);
            concealmentEventsText = view.findViewById(R.id.track_stats_item_concealment_events_textview);
            firCountText = view.findViewById(R.id.track_stats_item_fir_count_textview);
            pliCountText = view.findViewById(R.id.track_stats_item_pli_count_textview);
            nackCountText = view.findViewById(R.id.track_stats_item_nack_count_textview);
            packetsReceivedText = view.findViewById(R.id.track_stats_item_packets_received_textview);
            bytesReceivedText = view.findViewById(R.id.track_stats_item_bytes_received_textview);
            framesEncodedText = view.findViewById(R.id.track_stats_item_frames_encoded_textview);
            framesDecodedText = view.findViewById(R.id.track_stats_item_frames_decoded_textview);
            framesReceivedText = view.findViewById(R.id.track_stats_item_frames_received_textview);
            framesDroppedText = view.findViewById(R.id.track_stats_item_frames_dropped_textview);
            totalFreezeDurationText = view.findViewById(R.id.track_stats_item_total_freeze_duration_textview);

        }
    }
}

