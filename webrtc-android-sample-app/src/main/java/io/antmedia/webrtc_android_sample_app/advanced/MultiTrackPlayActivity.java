package io.antmedia.webrtc_android_sample_app.advanced;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtc_android_sample_app.basic.SettingsActivity;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.DefaultWebRTCListener;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.api.IWebRTCListener;
import io.antmedia.webrtcandroidframework.core.DataChannelConstants;

public class MultiTrackPlayActivity extends TestableActivity {
    private IWebRTCClient webRTCClient;
    private EditText streamIdEditText;
    private String streamId;
    private Button startStreamingButton;
    private LinearLayout playersLayout;
    private LinearLayout checkboxesLayout;

    /*
     * We will receive videoTrack objects from the server through the onNewVideoTrack callback of the webrtc client listener.
     * These videoTracks are not yet assigned to a streamId. We store them inside videoTrackList.
     */
    private ArrayList<VideoTrack> videoTrackList = new ArrayList<>();

    private JSONArray trackAssignments;

    /*
     * A message will arrive with a data channel message containing the eventType VIDEO_TRACK_ASSIGNMENT_LIST.
     * This message includes a videoLabel (trackId) and trackId (actual streamId).
     * Upon receiving this message, we will match our videoTrack objects with the streamIds and store them in the map below.
     * This allows us to determine which video track belongs to which stream id.
     */
    private HashMap<String,VideoTrack> streamIdVideoTrackMap = new HashMap<>();

    private HashMap<SurfaceViewRenderer, VideoTrack> surfaceViewRendererVideoTrackMap = new HashMap<>();

    private HashMap<String, SurfaceViewRenderer> streamIdSurfaceViewRendererMap = new HashMap<>();



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multitrack);

        playersLayout = findViewById(R.id.players);
        checkboxesLayout = findViewById(R.id.checkboxes);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        Button tracksButton = findViewById(R.id.tracks_button);
        streamIdEditText = findViewById(R.id.stream_id_edittext);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        streamIdEditText.setText("mainTrack");

        webRTCClient = IWebRTCClient.builder()
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(createWebRTCListener())
                .setDataChannelObserver(createDatachannelObserver())
                .setVideoCallEnabled(false)
                .build();

        View startStreamingButton = findViewById(R.id.start_streaming_button);
        startStreamingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopStream(v);
            }
        });

        tracksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webRTCClient.getTrackList(streamIdEditText.getText().toString(), "");
            }
        });
    }

    public void startStopStream(View v) {
        incrementIdle();
        streamId = streamIdEditText.getText().toString();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Stop");
            Log.i(getClass().getSimpleName(), "Calling play start");
            webRTCClient.play(streamId);
        }
        else {
            ((Button) v).setText("Start");
            Log.i(getClass().getSimpleName(), "Calling play start");

            webRTCClient.stop(streamId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);
                try{
                    JSONObject msgJsonObj = new JSONObject(messageText);
                    if(msgJsonObj.has(DataChannelConstants.EVENT_TYPE) && msgJsonObj.getString(DataChannelConstants.EVENT_TYPE).equals(DataChannelConstants.VIDEO_TRACK_ASSIGNMENT_LIST)){
                        trackAssignments = msgJsonObj.getJSONArray(DataChannelConstants.PAYLOAD);
                        matchStreamIdAndVideoTrack();
                        matchStreamIdAndSurfaceViewRenderer();
                    }
                }catch (Exception e){
                    Log.e(getClass().getSimpleName(),"Cant parse data channel message to JSON object. "+e.getMessage());
                }

            }
        };
    }

    private void matchStreamIdAndVideoTrack(){
        try{
            for(int i=0;i<trackAssignments.length();i++){
                // inside this object videoLabel is actually trackId(it is like videoTrack0) and trackId is our actual streamId
                JSONObject videoLabelTrackIdObj = trackAssignments.getJSONObject(i);
                for(int k=0;k<videoTrackList.size();k++){
                    VideoTrack videoTrack = videoTrackList.get(k);
                    String videoTrackId = videoTrack.id().substring(DataChannelConstants.TRACK_ID_PREFIX.length());
                    if(videoLabelTrackIdObj.getString(DataChannelConstants.VIDEO_LABEL).equals(videoTrackId)){
                        streamIdVideoTrackMap.put(videoLabelTrackIdObj.getString(DataChannelConstants.TRACK_ID), videoTrack);
                    }
                }
            }
        }catch (JSONException e){
            Log.e(getClass().getSimpleName(), "Cant parse JSON on matchStreamIdAndVideoTrack method. "+ e.getMessage());
        }
    }

    private void matchStreamIdAndSurfaceViewRenderer(){
        for (Map.Entry<String, VideoTrack> entry1 : streamIdVideoTrackMap.entrySet()) {
            String streamId = entry1.getKey();
            VideoTrack videoTrack1 = entry1.getValue();
            for (Map.Entry<SurfaceViewRenderer, VideoTrack> entry2 : surfaceViewRendererVideoTrackMap.entrySet()) {
                SurfaceViewRenderer renderer = entry2.getKey();
                VideoTrack videoTrack2 = entry2.getValue();
                if(videoTrack1.equals(videoTrack2)){
                    streamIdSurfaceViewRendererMap.put(streamId, renderer);
                }
            }
        }
    }

    private IWebRTCListener createWebRTCListener() {
        return new DefaultWebRTCListener() {
            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                decrementIdle();
            }

            @Override
            public void onPlayFinished(String streamId) {
                super.onPlayFinished(streamId);
                decrementIdle();
            }

            @Override
            public void onNewVideoTrack(VideoTrack videoTrack, String trackId) {
                super.onNewVideoTrack(videoTrack, trackId);
                videoTrackList.add(videoTrack);
                if(trackAssignments != null){
                    matchStreamIdAndVideoTrack();
                }

                runOnUiThread(() -> {
                    SurfaceViewRenderer renderer = new SurfaceViewRenderer(getApplicationContext());
                    renderer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    playersLayout.addView(renderer);
                    webRTCClient.setRendererForVideoTrack(renderer, videoTrack);
                    surfaceViewRendererVideoTrackMap.put(renderer, videoTrack);
                    matchStreamIdAndSurfaceViewRenderer();
                });
            }

            @Override
            public void onTrackList(String[] tracks) {
                for (String track : tracks) {
                    CheckBox checkBox = new CheckBox(MultiTrackPlayActivity.this);
                    checkBox.setText(track);
                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                            webRTCClient.enableTrack(streamId, track, isChecked));
                    checkboxesLayout.addView(checkBox);
                }
            }
        };
    }
}
