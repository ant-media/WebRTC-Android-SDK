package io.antmedia.webrtc_android_sample_app;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.DataChannel;
import org.webrtc.VideoTrack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import android.widget.Toast;

import java.nio.ByteBuffer;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.PermissionCallback;

public class AbstractSampleSDKActivityTest {

    @Mock
    private PermissionCallback mockPermissionCallback;

    private AbstractSampleSDKActivity activity;

    @Mock
    private DataChannel mockDataChannel;
    @Mock
    private VideoTrack mockVideoTrack;
    private DataChannel.Buffer buffer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        activity = spy(AbstractSampleSDKActivity.class);
        doNothing().when(activity).showPermissionsErrorAndRequest(anyList());
        doNothing().when(activity).makeToast(anyString(), anyInt());

        ByteBuffer bb = ByteBuffer.allocate(10);

        buffer = new DataChannel.Buffer(bb, false);
    }

    @Test
    public void checkAndRequestPermissions_AllPermissionsGranted_ReturnsTrue() {
        when(activity.hasPermissions(any(), anyList())).thenReturn(true);

        boolean result = activity.checkAndRequestPermisssions(true, mockPermissionCallback);

        assertTrue(result);
        verify(mockPermissionCallback, never()).onPermissionResult();
    }

    @Test
    public void checkAndRequestPermissions_PermissionsNotGranted_ReturnsFalse() {
        when(activity.hasPermissions(any(), anyList())).thenReturn(false);

        boolean result = activity.checkAndRequestPermisssions(true, mockPermissionCallback);

        assertFalse(result);
        verify(activity).showPermissionsErrorAndRequest(anyList());
    }

    @Test
    public void onRequestPermissionsResult_CallsPermissionCallback() {
        doReturn(false).when(activity).hasPermissions(any(), anyList());
        activity.checkAndRequestPermisssions(true, mockPermissionCallback);
        activity.onRequestPermissionsResult(1, new String[]{}, new int[]{});
        verify(mockPermissionCallback).onPermissionResult();
    }

    @Test
    public void onBufferedAmountChange() {
        activity.onBufferedAmountChange(100, "dataChannelLabel");
    }

    @Test
    public void onStateChange() {
        activity.onStateChange(DataChannel.State.OPEN, "dataChannelLabel");
    }

    @Test
    public void onMessage() {
        activity.onMessage(buffer, "dataChannelLabel");

        verify(activity).makeToast(anyString(), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onMessageSent_Successful() {
        activity.onMessageSent(buffer, true);

        verify(activity).makeToast(eq("Message is sent"), eq(Toast.LENGTH_SHORT));
    }

    @Test
    public void onMessageSent_Unsuccessful() {
        activity.onMessageSent(buffer, false);

        verify(activity).makeToast(eq("Could not send the text message"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onDisconnected() {
        activity.onDisconnected("streamId");

        verify(activity).makeToast(eq("Disconnected for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onPublishFinished() {
        activity.onPublishFinished("streamId");

        verify(activity).makeToast(eq("Publish finished for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onPlayFinished() {
        activity.onPlayFinished("streamId");

        verify(activity).makeToast(eq("Play finished for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onPublishStarted() {
        activity.onPublishStarted("streamId");

        verify(activity).makeToast(eq("Publish started for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onPlayStarted() {
        activity.onPlayStarted("streamId");

        verify(activity).makeToast(eq("Play started for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void noStreamExistsToPlay() {
        activity.noStreamExistsToPlay("streamId");

        verify(activity).makeToast(eq("No stream exists to play for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onError() {
        activity.onError("errorDescription", "streamId");

        verify(activity).makeToast(eq("Error for streamId : errorDescription"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onSignalChannelClosed() {
        activity.onSignalChannelClosed(
                WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification.NORMAL, "streamId");

        verify(activity).makeToast(eq("Signal channel closed for streamId : NORMAL"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void streamIdInUse() {
        activity.streamIdInUse("streamId");

        verify(activity).makeToast(eq("Stream id is already in use streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onIceConnected() {
        activity.onIceConnected("streamId");

        verify(activity).makeToast(eq("Ice connected for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onIceDisconnected() {
        activity.onIceDisconnected("streamId");

        verify(activity).makeToast(eq("Ice disconnected for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onNewVideoTrack() {
        activity.onNewVideoTrack(mock(VideoTrack.class));

        verify(activity).makeToast(eq("New video track received"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onVideoTrackEnded() {
        activity.onVideoTrackEnded(mock(VideoTrack.class));

        verify(activity).makeToast(eq("Video track ended"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onReconnectionAttempt() {
        activity.onReconnectionAttempt("streamId");

        verify(activity).makeToast(eq("Reconnection attempt for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onJoinedTheRoom() {
        activity.onJoinedTheRoom("streamId", new String[]{"stream1", "stream2"});

        verify(activity).makeToast(eq("Joined the room for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onRoomInformation_CallsLog() {
        activity.onRoomInformation(new String[]{"stream1", "stream2"});
    }

    @Test
    public void onLeftTheRoom_CallsLog() {
        activity.onLeftTheRoom("roomId");
    }

    @Test
    public void onMutedFor() {
        activity.onMutedFor("streamId");

        verify(activity).makeToast(eq("Microphone is muted for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onUnmutedFor() {
        activity.onUnmutedFor("streamId");

        verify(activity).makeToast(eq("Microphone is unmuted for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onCameraTurnOnFor() {
        activity.onCameraTurnOnFor("streamId");

        verify(activity).makeToast(eq("Camera is turned on for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onCameraTurnOffFor() {
        activity.onCameraTurnOffFor("streamId");

        verify(activity).makeToast(eq("Camera is turned off for streamId"), eq(Toast.LENGTH_LONG));
    }

    @Test
    public void onSatatusUpdateFor() {
        activity.onSatatusUpdateFor("streamId", true, false);

        verify(activity).makeToast(eq("Status update for streamId mic: true camera: false"), eq(Toast.LENGTH_LONG));
    }

}
