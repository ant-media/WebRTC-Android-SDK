package io.antmedia.webrtc_android_sample_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.webrtc.DataChannel;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.PermissionCallback;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;

public abstract class AbstractSampleSDKActivity extends Activity implements IWebRTCListener, IDataChannelObserver {
    public CountingIdlingResource idlingResource = new CountingIdlingResource("Load", true);
    private PermissionCallback permissionCallback;

    public void  incrementIdle() {
        idlingResource.increment();
    }
    public void decrementIdle() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    public static final String[] REQUIRED_PUBLISH_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
            new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_CONNECT}
            :
            new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

    // List of mandatory application permissions.
    public static final String[] REQUIRED_MINIMUM_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.INTERNET"};

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
        makeToast("New Message: " + messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (successful) {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String messageText = new String(bytes, StandardCharsets.UTF_8);

            makeToast("Message is sent", Toast.LENGTH_SHORT);
        } else {
            makeToast("Could not send the text message", Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onDisconnected(String streamId) {
        String messageText = "Disconnected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onPublishFinished(String streamId) {
        String messageText = "Publish finished for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onPlayFinished(String streamId) {
        String messageText = "Play finished for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onPublishStarted(String streamId) {
        String messageText = "Publish started for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onPlayStarted(String streamId) {
        String messageText = "Play started for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        String messageText = "No stream exists to play for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onError(String description, String streamId) {
        String messageText = "Error for " + streamId + " : " + description;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        String messageText = "Signal channel closed for " + streamId + " : " + code;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void streamIdInUse(String streamId) {
        String messageText = "Stream id is already in use " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onIceConnected(String streamId) {
        String messageText = "Ice connected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onIceDisconnected(String streamId) {
        String messageText = "Ice disconnected for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    public void makeToast(String messageText, int lengthLong) {
        runOnUiThread(() -> Toast.makeText(AbstractSampleSDKActivity.this, messageText, lengthLong).show());
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
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onVideoTrackEnded(VideoTrack track) {
        String messageText = "Video track ended";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onReconnectionAttempt(String streamId) {
        String messageText = "Reconnection attempt for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onJoinedTheRoom(String streamId, String[] streams) {
        String messageText = "Joined the room for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }


    @Override
    public void onRoomInformation(String[] streams) {
        String messageText = "Room information received";
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLeftTheRoom(String roomId) {
        String messageText = "Left the room for " + roomId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        //Toast.makeText(this, messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMutedFor(String streamId) {
        String messageText = "Microphone is muted for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }
    @Override
    public void onUnmutedFor(String streamId) {
        String messageText = "Microphone is unmuted for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onCameraTurnOnFor(String streamId) {
        String messageText = "Camera is turned on for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onCameraTurnOffFor(String streamId) {
        String messageText = "Camera is turned off for " + streamId;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public void onSatatusUpdateFor(String streamId, boolean micStatus, boolean cameraStatus) {
        String messageText = "Status update for " + streamId + " mic: " + micStatus + " camera: " + cameraStatus;
        Log.d(AbstractSampleSDKActivity.class.getName(), messageText);
        makeToast(messageText, Toast.LENGTH_LONG);
    }

    @Override
    public boolean checkAndRequestPermisssions(boolean isForPublish, PermissionCallback permissionCallback) {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.addAll(Arrays.asList(REQUIRED_MINIMUM_PERMISSIONS));
        if(isForPublish) {
            permissions.addAll(Arrays.asList(REQUIRED_PUBLISH_PERMISSIONS));
        }

        if (hasPermissions(this, permissions)) {
            return true;
        }
        else {
            this.permissionCallback = permissionCallback;
            showPermissionsErrorAndRequest(permissions);
            return false;
        }
    }

    public boolean hasPermissions(Context context, List<String> permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(HomeActivity.class.getSimpleName(), "Permission required:"+permission);
                    return false;
                }
            }
        }
        return true;
    }
    public void showPermissionsErrorAndRequest(List<String> permissions) {
        makeToast("You need permissions before", Toast.LENGTH_SHORT);
        String[] permissionArray = new String[permissions.size()];
        permissions.toArray(permissionArray);
        ActivityCompat.requestPermissions(this, permissionArray, 1);
    }

    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        permissionCallback.onPermissionResult();
    }

}

