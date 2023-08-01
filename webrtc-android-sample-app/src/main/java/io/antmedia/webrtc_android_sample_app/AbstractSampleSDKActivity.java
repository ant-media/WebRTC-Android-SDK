package io.antmedia.webrtc_android_sample_app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.webrtc.DataChannel;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public abstract class AbstractSampleSDKActivity extends Activity implements IWebRTCListener, IDataChannelObserver {
    public CountingIdlingResource idlingResource = new CountingIdlingResource("Load", true);

    public void  incrementIdle() {
        idlingResource.increment();
    }
    public void decrementIdle() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    public IdlingResource getIdlingResource() {
        return idlingResource;
    }

    @SuppressLint("WrongViewCast")
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

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    @Override
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {
        String messageText = "Data channel buffered amount changed: " + dataChannelLabel + ": " + previousAmount;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {
        String messageText = "Data channel state changed: " + dataChannelLabel + ": " + state;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        ByteBuffer data = buffer.data;
        String messageText = new String(data.array(), StandardCharsets.UTF_8);
        Toast.makeText(this, "New Message: " + messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (successful) {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String messageText = new String(bytes, StandardCharsets.UTF_8);

            Toast.makeText(this, "Message is sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not send the text message", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDisconnected(String streamId) {
        String messageText = "Disconnected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPublishFinished(String streamId) {
        String messageText = "Publish finished for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlayFinished(String streamId) {
        String messageText = "Play finished for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPublishStarted(String streamId) {
        String messageText = "Publish started for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlayStarted(String streamId) {
        String messageText = "Play started for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        String messageText = "No stream exists to play for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String description, String streamId) {
        String messageText = "Error for " + streamId + " : " + description;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        String messageText = "Signal channel closed for " + streamId + " : " + code;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void streamIdInUse(String streamId) {
        String messageText = "Stream id is already in use " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onIceConnected(String streamId) {
        String messageText = "Ice connected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onIceDisconnected(String streamId) {
        String messageText = "Ice disconnected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrackList(String[] tracks) {
        String messageText = "Track list received";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {
        String messageText = "Bitrate measurement received";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {
        String messageText = "Stream info list received";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNewVideoTrack(VideoTrack track) {
        String messageText = "New video track received";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onVideoTrackEnded(VideoTrack track) {
        String messageText = "Video track ended";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onReconnectionAttempt(String streamId) {
        String messageText = "Reconnection attempt for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }
}

