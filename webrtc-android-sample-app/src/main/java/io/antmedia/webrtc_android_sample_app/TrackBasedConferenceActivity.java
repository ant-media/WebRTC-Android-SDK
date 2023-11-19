package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class TrackBasedConferenceActivity extends AbstractSampleSDKActivity {

    public static final String MIC_UNMUTED = "MIC_UNMUTED";
    public static final String MIC_MUTED = "MIC_MUTED";
    public static final String CAM_TURNED_ON = "CAM_TURNED_ON";
    public static final String CAM_TURNED_OFF = "CAM_TURNED_OFF";
    public static final String EVENT_TYPE = "eventType";
    public static final String STREAM_ID = "streamId";
    public static final String UPDATE_STATUS = "UPDATE_STATUS";
    public static final String MIC_STATUS = "mic";
    public static final String CAMERA_STATUS = "camera";

    private int STATUS_SEND_PERIOD_MILLIS = 3000;

    private WebRTCClient webRTCClient;
    private Button audioButton;
    private Button videoButton;
    private boolean stoppedStream = false;
    private TextView broadcastingView;
    private ArrayList<SurfaceViewRenderer> playViewRenderers;
    private int rendererIndex = 0;
    private boolean playOnlyMode = false;
    private boolean joined = false;
    private String roomId;
    private String streamId;

    private Handler handler = new Handler();

    private int ROOM_INFO_POLLING_MILLIS = 5000;
    private Runnable getRoomInfoRunnable = new Runnable() {
        @Override
        public void run() {
            getRoomInfo();
            handler.postDelayed(this, ROOM_INFO_POLLING_MILLIS);
        }
    };
    private String token;
    private String subscriberId;
    private String subscriberCode;
    private boolean videoCallEnabled = true;
    private boolean audioCallEnabled = true;
    private String streamName;
    private boolean playMessageSent;
    private String viewerInfo;

    private Runnable sendStatusRunnable = new Runnable() {
        @Override
        public void run() {
            sendStatusMessage();
            handler.postDelayed(this, STATUS_SEND_PERIOD_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_multitrack_conference);

        broadcastingView = findViewById(R.id.broadcasting_text_view);

        SurfaceViewRenderer publishViewRenderer = findViewById(R.id.publish_view_renderer);

        playViewRenderers = new ArrayList<>();

        playViewRenderers.add(findViewById(R.id.play_view_renderer1));
        playViewRenderers.add(findViewById(R.id.play_view_renderer2));
        playViewRenderers.add(findViewById(R.id.play_view_renderer3));
        playViewRenderers.add(findViewById(R.id.play_view_renderer4));

        Switch playOnlySwitch = findViewById(R.id.play_only_switch);
        playOnlySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            //conferenceManager.setPlayOnlyMode(b);
            playOnlyMode = b;
            publishViewRenderer.setVisibility(b ? View.GONE : View.VISIBLE);
        });

        audioButton = findViewById(R.id.control_audio_button);
        audioButton.setOnClickListener((view) -> controlAudio(view));

        videoButton = findViewById(R.id.control_video_button);
        videoButton.setOnClickListener((view) -> controlVideo(view));

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        //  this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_CALL, false);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        roomId = sharedPreferences.getString(getString(R.string.roomId), SettingsActivity.DEFAULT_ROOM_NAME);
        streamId = streamId == null ? "stream"+new Random().nextInt(99999) : streamId;


        webRTCClient = new WebRTCClient(this, this);
        webRTCClient.setMainTrackId(roomId);
        webRTCClient.setSelfStreamId(streamId);
        webRTCClient.setVideoRenderers(null, publishViewRenderer);
        webRTCClient.setRemoteRendererList(playViewRenderers);


        webRTCClient.init(serverUrl, streamId, WebRTCClient.MODE_TRACK_BASED_CONFERENCE, "", this.getIntent());
        webRTCClient.setDataChannelEnabled(true);
        webRTCClient.setDataChannelObserver(this);
        webRTCClient.setOpenFrontCamera(true);
        webRTCClient.setReconnectionEnabled(true);
    }

    private void scheduleGetRoomInfo() {
        handler.postDelayed(getRoomInfoRunnable, ROOM_INFO_POLLING_MILLIS);
    }

    private void clearGetRoomInfoSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(getRoomInfoRunnable)) {
                handler.removeCallbacks(getRoomInfoRunnable);
            }
        } else {
            handler.removeCallbacks(getRoomInfoRunnable);
        }
    }

    private void getRoomInfo() {
        webRTCClient.getRoomInfo(roomId, streamId);
    }

    public void joinConference(View v) {

        if (!joined) {
            Log.w(getClass().getSimpleName(), "Joining Conference");
            ((Button) v).setText("Leave");
            webRTCClient.joinToConferenceRoom(roomId, streamId);
        } else {
            ((Button) v).setText("Join");
            webRTCClient.leaveFromConference(roomId);
            stoppedStream = true;
        }
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();

        broadcastingView.setText("Publishing");
        broadcastingView.setVisibility(View.VISIBLE);

        scheduleSendStatusTimer();
        decrementIdle();
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
        stoppedStream = true;
    }

    @Override
    public void onDisconnected(String streamId) {
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        audioButton.setText("Disable Audio");
        videoButton.setText("Disable Video");
    }

    @Override
    public void onNewVideoTrack(VideoTrack track) {
        if(false && !track.id().contains(webRTCClient.getStreamId())) {
            SurfaceViewRenderer renderer = playViewRenderers.get(rendererIndex++%4);
            webRTCClient.addTrackToRenderer(track, renderer);
        }
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        Log.w(this.getClass().getSimpleName(), "On Joined the Room ");

        if(!webRTCClient.isReconnectionInProgress() && !playOnlyMode) {
            publishStream(streamId);
        }

        if(playOnlyMode) {
            startPlaying(streams);
        }

        joined = true;
        // start periodic polling of room info
        scheduleGetRoomInfo();
        if(streams.length > 0) {
            //on track list triggers start playing
            onTrackList(streams);
        }
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        clearGetRoomInfoSchedule();
        joined = false;
        playMessageSent = false;
        clearSendStatusSchedule();
    }

    public void publishStream(String streamId) {
        incrementIdle();
        if (!this.playOnlyMode) {
            webRTCClient.publish(streamId, token, videoCallEnabled, audioCallEnabled, subscriberId, subscriberCode, streamName, roomId);
        }
        else {
            Log.i(getClass().getSimpleName(), "Play only mode. No publishing");
        }
    }

    private void startPlaying(String[] streams) {
        if(!playMessageSent) {
            webRTCClient.play(roomId, token, streams, subscriberId, subscriberCode, viewerInfo);
            playMessageSent = true;
        }
    }

    @Override
    public void onRoomInformation(String[] streams) {
        if (webRTCClient != null) {
            startPlaying(streams);
        }
    }

    public void controlAudio(View view) {
        if (webRTCClient.isAudioOn()) {
            webRTCClient.disableAudio();
            audioButton.setText("Enable Audio");
            sendNotificationEvent(MIC_MUTED, null);
        } else {
            webRTCClient.isAudioOn();
            audioButton.setText("Disable Audio");
            sendNotificationEvent(MIC_UNMUTED, null);
        }
    }

    public void controlVideo(View view) {
        if (webRTCClient.isVideoOn()) {
            webRTCClient.disableVideo();
            videoButton.setText("Enable Video");
            sendNotificationEvent(CAM_TURNED_OFF, null);

        } else {
            webRTCClient.enableVideo();
            videoButton.setText("Disable Video");
            sendNotificationEvent(CAM_TURNED_ON, null);
        }
    }

    public void changeWifiState(boolean state) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(state);
    }


    private void scheduleSendStatusTimer() {
        handler.postDelayed(sendStatusRunnable, STATUS_SEND_PERIOD_MILLIS);
    }

    private void clearSendStatusSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (handler.hasCallbacks(sendStatusRunnable)) {
                handler.removeCallbacks(sendStatusRunnable);
            }
        } else {
            handler.removeCallbacks(sendStatusRunnable);
        }
    }

    public void sendStatusMessage() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(MIC_STATUS, webRTCClient.isAudioOn());
            jsonObject.put(CAMERA_STATUS, webRTCClient.isVideoOn());
            sendNotificationEvent(UPDATE_STATUS, jsonObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendNotificationEvent(String eventType, JSONObject data) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("streamId", streamId);
            jsonObject.put("eventType", eventType);
            jsonObject.put(STREAM_ID, streamId);
            jsonObject.put(EVENT_TYPE, eventType);
            if(data != null) {
                jsonObject.put("info", data);
            }

            String notificationEventText = jsonObject.toString();

            final ByteBuffer buffer = ByteBuffer.wrap(notificationEventText.getBytes(StandardCharsets.UTF_8));
            DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
            webRTCClient.sendMessageViaDataChannel(streamId, buf);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "JSON write error when creating notification event");
        }
    }

    public void onMessage(final DataChannel.Buffer buffer, String dataChannelLabel) {
        if (!buffer.binary) {
            ByteBuffer data = buffer.data;
            try {
                String strDataJson = new String(data.array(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(strDataJson);
                String eventType = jsonObject.getString(EVENT_TYPE);
                String streamId = jsonObject.getString(STREAM_ID);
                if(eventType.equals(MIC_MUTED)) {
                    String messageText = "Microphone is muted for " + streamId;
                    Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
                    Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
                }
                else if(eventType.equals(MIC_UNMUTED)) {
                    String messageText = "Microphone is unmuted for " + streamId;
                    Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
                    Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
                }
                else if(eventType.equals(CAM_TURNED_ON)) {
                    String messageText = "Camera is turned on for " + streamId;
                    Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
                    Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();

                }
                else if(eventType.equals(CAM_TURNED_OFF)) {
                    String messageText = "Camera is turned off for " + streamId;
                    Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
                    Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
                }
                else if(eventType.equals(UPDATE_STATUS)) {
                    JSONObject info = jsonObject.getJSONObject("info");
                    boolean micStatus = info.getBoolean(MIC_STATUS);
                    boolean cameraStatus = info.getBoolean(CAMERA_STATUS);
                    String messageText = "Status update for " + streamId + " mic: " + micStatus + " camera: " + cameraStatus;
                    Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
                    Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }




}

