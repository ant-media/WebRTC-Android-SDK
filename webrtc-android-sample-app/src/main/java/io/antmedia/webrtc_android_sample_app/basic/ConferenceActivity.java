package io.antmedia.webrtc_android_sample_app.basic;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.antmedia.webrtc_android_sample_app.R;
import io.antmedia.webrtc_android_sample_app.TestableActivity;
import io.antmedia.webrtcandroidframework.api.DefaultConferenceWebRTCListener;
import io.antmedia.webrtcandroidframework.api.DefaultDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.api.IWebRTCClient;
import io.antmedia.webrtcandroidframework.websocket.Broadcast;

public class ConferenceActivity extends TestableActivity {
    private TextView broadcastingView;
    private View joinButton;
    private String streamId;
    private IWebRTCClient webRTCClient;
    private String roomId;
    private Button audioButton;
    private Button videoButton;
    private boolean playOnly;
    private HashMap<VideoTrack, String> videoTrackStreamIdMap = new HashMap<>();

    private TextView publisherSpeakingIndicatorText;
    private TextView player1SpeakingIndicatorText;
    private TextView player2SpeakingIndicatorText;
    private TextView player3SpeakingIndicatorText;
    private TextView player4SpeakingIndicatorText;

    //Audio level coming from server is between 0 and 100. Lower value means higher audio.
    //All remote conference participants audio levels come from server through data channel.
    private final int SPEAKING_AUDIO_LEVEL_THRESHOLD = 65;
    //Audio level treshold for local speaker. Higher value means higher audio.
    private final double LOCAL_SPEAKING_AUDIO_LEVEL_THRESHOLD = 0.005;

    private ScheduledFuture localAudioCheckerFuture;
    private ScheduledExecutorService localAudioCheckerExecutor;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);

        SurfaceViewRenderer publisherRenderer = findViewById(R.id.publish_view_renderer);

        SurfaceViewRenderer player1Renderer = findViewById(R.id.play_view_renderer1);
        SurfaceViewRenderer player2Renderer = findViewById(R.id.play_view_renderer2);
        SurfaceViewRenderer player3Renderer = findViewById(R.id.play_view_renderer3);
        SurfaceViewRenderer player4Renderer = findViewById(R.id.play_view_renderer4);

        publisherSpeakingIndicatorText = findViewById(R.id.publisher_speaking_indicator_text);
        player1SpeakingIndicatorText = findViewById(R.id.player1_speaking_indicator_text);
        player2SpeakingIndicatorText = findViewById(R.id.player2_speaking_indicator_text);
        player3SpeakingIndicatorText = findViewById(R.id.player3_speaking_indicator_text);
        player4SpeakingIndicatorText = findViewById(R.id.player4_speaking_indicator_text);


        broadcastingView = findViewById(R.id.broadcasting_text_view);
        joinButton = findViewById(R.id.join_conference_button);

        audioButton = findViewById(R.id.control_audio_button);
        videoButton = findViewById(R.id.control_video_button);

        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);
        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = "streamId" + (int)(Math.random()*9999);

        DefaultConferenceWebRTCListener defaultConferenceListener = createWebRTCListener(roomId, streamId);


        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            playOnly = b;
            publisherRenderer.setVisibility(b ? View.GONE : View.VISIBLE);
        });

        webRTCClient = IWebRTCClient.builder()
                .addRemoteVideoRenderer(player1Renderer, player2Renderer, player3Renderer, player4Renderer)
                .setLocalVideoRenderer(publisherRenderer)
                .setServerUrl(serverUrl)
                .setActivity(this)
                .setWebRTCListener(defaultConferenceListener)
                .setDataChannelObserver(createDatachannelObserver())
                .build();

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinLeaveRoom(v);
            }
        });
    }

    public void joinLeaveRoom(View v) {
        incrementIdle();
        if (!webRTCClient.isStreaming(streamId)) {
            ((Button) v).setText("Leave");
            Log.i(getClass().getSimpleName(), "Calling join");

            if(playOnly) {
                webRTCClient.joinToConferenceRoom(roomId);
            }
            else {
                webRTCClient.joinToConferenceRoom(roomId, streamId);
            }
        }
        else {
            ((Button) v).setText("Join");
            Log.i(getClass().getSimpleName(), "Calling leave");
            if(localAudioCheckerFuture != null){
                localAudioCheckerFuture.cancel(true);
            }
            if(localAudioCheckerExecutor != null){
                localAudioCheckerExecutor.shutdown();
            }
            webRTCClient.leaveFromConference(roomId);
        }
    }

    private IDataChannelObserver createDatachannelObserver() {
        return new DefaultDataChannelObserver() {
            @Override
            public void textMessageReceived(String messageText) {
                super.textMessageReceived(messageText);

                try {
                    JSONObject messageJsonObj = new JSONObject(messageText);
                    if (messageJsonObj.has("eventType") && messageJsonObj.getString("eventType").equals("UPDATE_AUDIO_LEVEL")) {
                        handleAudioLevelUpdate(messageJsonObj);
                    }
                } catch (JSONException e) {
                    // Handle JSON parsing exception
                    e.printStackTrace();
                }
            }

            private void handleAudioLevelUpdate(JSONObject messageJsonObj) throws JSONException {
                String speakerParticipantStreamId = messageJsonObj.getString("streamId");
                int audioLevel = messageJsonObj.getInt("audioLevel");
                //sometimes local participant audio level does not come from datachannel. thats why we have local check.
                boolean isLocalParticipant = speakerParticipantStreamId.equals(streamId);
                boolean isSpeaking = audioLevel < SPEAKING_AUDIO_LEVEL_THRESHOLD;

                runOnUiThread(() -> updateSpeakingIndicator(isLocalParticipant, speakerParticipantStreamId, isSpeaking));
            }

            private void updateSpeakingIndicator(boolean isLocalParticipant, String speakerParticipantStreamId, boolean isSpeaking) {
                if (isLocalParticipant) {
                    updateSpeakingIndicatorView(publisherSpeakingIndicatorText, isSpeaking);
                } else {
                    int speakerIndicatorIndex = getParticipantIndicator(speakerParticipantStreamId);
                    View indicatorView = getParticipantSpeakingIndicatorView(speakerIndicatorIndex);
                    updateSpeakingIndicatorView(indicatorView, isSpeaking);
                }
            }

            private void updateSpeakingIndicatorView(View indicatorView, boolean isSpeaking) {
                indicatorView.setVisibility(isSpeaking ? View.VISIBLE : View.GONE);
            }

            private View getParticipantSpeakingIndicatorView(int speakerIndicatorIndex) {
                switch (speakerIndicatorIndex) {
                    case 1:
                        return player1SpeakingIndicatorText;
                    case 2:
                        return player2SpeakingIndicatorText;
                    case 3:
                        return player3SpeakingIndicatorText;
                    case 4:
                        return player4SpeakingIndicatorText;
                    default:
                        throw new IllegalArgumentException("Invalid speaker indicator index");
                }
            }
        };
    }

    private DefaultConferenceWebRTCListener createWebRTCListener(String roomId, String streamId) {
        return new DefaultConferenceWebRTCListener(roomId, streamId) {
            @Override
            public void onNewVideoTrack(VideoTrack track, String streamId) {
                super.onNewVideoTrack(track, streamId);
                videoTrackStreamIdMap.put(track, streamId);

            }

            @Override
            public void onBroadcastObject(Broadcast broadcast) {
                super.onBroadcastObject(broadcast);
                Log.d("antmedia on br object", "");
            }

            @Override
            public void onPlayStarted(String streamId) {
                super.onPlayStarted(streamId);
                Log.d("antmedia play started",streamId);


            }

            @Override
            public void onPublishStarted(String streamId) {
                super.onPublishStarted(streamId);
                broadcastingView.setVisibility(View.VISIBLE);
                startLocalAudioChecker();
                decrementIdle();
            }

            @Override
            public void onPublishFinished(String streamId) {
                super.onPublishFinished(streamId);
                broadcastingView.setVisibility(View.GONE);
                decrementIdle();
            }
        };
    }

    //this method gets the volume of local publisher and checks if he is speaking.
    //we have this method because local participants audio level does not come from server through data channel.
    public void startLocalAudioChecker() {
        localAudioCheckerExecutor = Executors.newScheduledThreadPool(1);
        localAudioCheckerFuture = localAudioCheckerExecutor.scheduleAtFixedRate(() -> {
            double localAudioLevel = webRTCClient.getStatsCollector().getLocalAudioLevel();
            Log.d("localAudioLevel", String.valueOf(localAudioLevel));
            if (webRTCClient.getConfig().audioCallEnabled && localAudioLevel > LOCAL_SPEAKING_AUDIO_LEVEL_THRESHOLD) {
                runOnUiThread(() -> publisherSpeakingIndicatorText.setVisibility(View.VISIBLE));
            } else {
                runOnUiThread(() -> publisherSpeakingIndicatorText.setVisibility(View.GONE));
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(localAudioCheckerFuture != null){
            localAudioCheckerFuture.cancel(true);
        }
        if(localAudioCheckerExecutor != null){
            localAudioCheckerExecutor.shutdown();
        }
    }

    public void controlAudio(View view) {
        if (webRTCClient.getConfig().audioCallEnabled) {
            webRTCClient.setAudioEnabled(false);
            audioButton.setText("Enable Audio");
        } else {
            webRTCClient.setAudioEnabled(true);
            audioButton.setText("Disable Audio");
        }
    }

    public int getParticipantIndicator(String participantStreamId){
        List<SurfaceViewRenderer> remoteVideoRenderers = webRTCClient.getConfig().remoteVideoRenderers;
        for (int i = 0; i < remoteVideoRenderers.size(); i++) {
            SurfaceViewRenderer r = remoteVideoRenderers.get(i);
            Object tag = r.getTag();

            if (tag instanceof VideoTrack) {
                VideoTrack videoTrack = (VideoTrack) tag;
                String surfaceViewRendererStreamId = videoTrackStreamIdMap.get(videoTrack);
                if(surfaceViewRendererStreamId.equals(participantStreamId)){
                    return i+1;
                }

            } else {
                System.out.println("Tag is not an instance of VideoTrack.");
            }
        }

        return 1;

    }

    public void controlVideo(View view) {
        if (webRTCClient.getConfig().videoCallEnabled) {
            webRTCClient.setVideoEnabled(false);
            videoButton.setText("Enable Video");
        } else {
            webRTCClient.setVideoEnabled(true);
            videoButton.setText("Disable Video");
        }
    }


    /**
     * This method is used to change the state of the wifi for testing purposes
     * @param state
     */
    public void changeWifiState(boolean state) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }
}
