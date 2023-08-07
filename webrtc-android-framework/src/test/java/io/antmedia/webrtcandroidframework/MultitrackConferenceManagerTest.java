package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.DataChannel;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MultitrackConferenceManagerTest {

    @Mock
    private Context context;
    
    @Mock
    private IWebRTCListener webRTCListener;
    
    @Mock
    private Intent intent;
    
    @Mock
    private SurfaceViewRenderer publishViewRenderer;
    
    private ArrayList<SurfaceViewRenderer> playViewRenderers = new ArrayList<SurfaceViewRenderer>();
    
    @Mock
    private IDataChannelObserver dataChannelObserver;

    private MultitrackConferenceManager conferenceManager;

    @Mock
    private WebSocketHandler wsHandler;

    @Mock
    WebRTCClient publishWebRTCClient;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Create SurfaceViewRenderers for testing
        SurfaceViewRenderer publishViewRenderer = mock(SurfaceViewRenderer.class);
        ArrayList<SurfaceViewRenderer> playViewRenderers = new ArrayList<>();
        playViewRenderers.add(mock(SurfaceViewRenderer.class));
        playViewRenderers.add(mock(SurfaceViewRenderer.class));

        // Create an Intent for testing
        Intent intent = new Intent();

        // Initialize the MultitrackConferenceManager with mocked dependencies
        conferenceManager = spy(new MultitrackConferenceManager(
                context,
                webRTCListener,
                intent,
                "http://example.com", // Replace with your server URL
                "room123", // Replace with your room name
                publishViewRenderer,
                playViewRenderers,
                "stream456", // Replace with your stream ID
                null // No dataChannelObserver for this example
        ));

        // Set the mock WebSocketHandler to the MultitrackConferenceManager
        conferenceManager.setWsHandler(wsHandler);

        conferenceManager.setPublishWebRTCClient(publishWebRTCClient);

        when(publishWebRTCClient.isAudioOn()).thenReturn(true);
        when(publishWebRTCClient.isVideoOn()).thenReturn(true);


    }

    @Test
    public void testJoinTheConference() {
        // Create a MultitrackConferenceManager instance
        MultitrackConferenceManager conferenceManager = new MultitrackConferenceManager(
            context,
            webRTCListener,
            intent,
            "serverUrl",
            "roomName",
            publishViewRenderer,
            playViewRenderers,
            "streamId",
            dataChannelObserver
        );

        // Mock the WebSocketHandler
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        conferenceManager.setWsHandler(wsHandler);

        // Call the joinTheConference method
        conferenceManager.joinTheConference();

        // Verify that the joinToConferenceRoom method of WebSocketHandler is called with the correct arguments
        verify(wsHandler).joinToConferenceRoom("roomName", "streamId");
    }

    @Test
    public void testLeaveFromConference() {
        // Create a MultitrackConferenceManager instance
        MultitrackConferenceManager conferenceManager = new MultitrackConferenceManager(
            context,
            webRTCListener,
            intent,
            "ws:/serverUrl",
            "roomName",
            publishViewRenderer,
            playViewRenderers,
            "streamId",
            dataChannelObserver
        );

        // Mock the WebSocketHandler
        WebSocketHandler wsHandler = mock(WebSocketHandler.class);
        conferenceManager.setWsHandler(wsHandler);

        // Call the leaveFromConference method
        conferenceManager.leaveFromConference();

        // Verify that the leaveFromTheConferenceRoom method of WebSocketHandler is called with the correct argument
        verify(wsHandler).leaveFromTheConferenceRoom("roomName");

        // Verify that the joined and playMessageSent flags are updated correctly
        assertFalse(conferenceManager.isJoined());
        assertFalse(conferenceManager.isPlayMessageSent());
    }

    @Test
    public void testSwitchCamera() {
        // Create a MultitrackConferenceManager instance
        MultitrackConferenceManager conferenceManager = new MultitrackConferenceManager(
            context,
            webRTCListener,
            intent,
            "serverUrl",
            "roomName",
            publishViewRenderer,
            playViewRenderers,
            "streamId",
            dataChannelObserver
        );

        // Mock the publishWebRTCClient
        WebRTCClient publishWebRTCClient = mock(WebRTCClient.class);
        conferenceManager.setPublishWebRTCClient(publishWebRTCClient);

        // Call the switchCamera method
        conferenceManager.switchCamera();

        // Verify that the switchCamera method of publishWebRTCClient is called
        verify(publishWebRTCClient).switchCamera();
    }


    @Test
    public void testDisableVideo() {
        // Call the disableVideo method
        conferenceManager.disableVideo();

        // Verify that publishWebRTCClient.disableVideo is called once
        verify(publishWebRTCClient, times(1)).disableVideo();

        // Verify that sendNotificationEvent is called with the correct parameters
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(conferenceManager, times(1)).sendNotificationEvent(eq("CAM_TURNED_OFF"), captor.capture());
        JSONObject data = captor.getValue();
        assertNull(data);
    }
    @Test
    public void testEnableVideo() {
        // Call the enableVideo method
        conferenceManager.enableVideo();

        // Verify that publishWebRTCClient.enableVideo is called once
        verify(publishWebRTCClient, times(1)).enableVideo();

        // Verify that sendNotificationEvent is called with the correct parameters
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(conferenceManager, times(1)).sendNotificationEvent(eq("CAM_TURNED_ON"), captor.capture());
        JSONObject data = captor.getValue();
        assertNull(data);
    }

    @Test
    public void testDisableAudio() {
        // Call the disableAudio method
        conferenceManager.disableAudio();

        // Verify that publishWebRTCClient.disableAudio is called once
        verify(publishWebRTCClient, times(1)).disableAudio();

        // Verify that sendNotificationEvent is called with the correct parameters
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(conferenceManager, times(1)).sendNotificationEvent(eq("MIC_MUTED"), captor.capture());
        JSONObject data = captor.getValue();
        assertNull(data);
    }

    @Test
    public void testEnableAudio() {
        // Call the enableAudio method
        conferenceManager.enableAudio();

        // Verify that publishWebRTCClient.enableAudio is called once
        verify(publishWebRTCClient, times(1)).enableAudio();

        // Verify that sendNotificationEvent is called with the correct parameters
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(conferenceManager, times(1)).sendNotificationEvent(eq("MIC_UNMUTED"), captor.capture());
        JSONObject data = captor.getValue();
        assertNull(data);

        verify(publishWebRTCClient, times(1)).sendMessageViaDataChannel(any(DataChannel.Buffer.class));
    }

    @Test
    public void testSendStatusMessage() throws JSONException {
        // Call the sendStatusMessage method
        conferenceManager.sendStatusMessage();

        // Verify that sendNotificationEvent is called with the correct parameters
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(conferenceManager, times(1)).sendNotificationEvent(eq("UPDATE_STATUS"), captor.capture());
        JSONObject data = captor.getValue();
        assertNotNull(data);
        assert data.length() == 2;
        assert data.getBoolean("mic") == publishWebRTCClient.isAudioOn();
        assert data.getBoolean("camera") == publishWebRTCClient.isVideoOn();
    }

    @Test
    public void testOnMessage() throws JSONException {
        Collection<String> eventTypes = Arrays.asList(
                "MIC_MUTED", "MIC_UNMUTED", "CAM_TURNED_ON", "CAM_TURNED_OFF", "UPDATE_STATUS"
        );

        for (String eventType : eventTypes) {
            // Create a mock DataChannel.Buffer with the JSON payload for the eventType
            String eventJson = "{\"eventType\":\"" + eventType + "\",\"streamId\":\"s1\"}";

            if(eventTypes.contains("UPDATE_STATUS")) {
                eventJson = "{\"eventType\":\"" + eventType + "\",\"streamId\":\"s1\",\"mic\":true,\"camera\":true}";
            }

            ByteBuffer data = ByteBuffer.wrap(eventJson.getBytes(StandardCharsets.UTF_8));
            DataChannel.Buffer buffer = new DataChannel.Buffer(data, false);

            // Call the onMessage method
            conferenceManager.onMessage(buffer, "dataChannelLabel");

            // Verify that webRTCListener methods are called accordingly based on the eventType
            switch (eventType) {
                case "MIC_MUTED":
                    verify(webRTCListener, times(1)).onMutedFor(anyString());
                    break;
                case "MIC_UNMUTED":
                    verify(webRTCListener, times(1)).onUnmutedFor(anyString());
                    break;
                case "CAM_TURNED_ON":
                    verify(webRTCListener, times(1)).onCameraTurnOnFor(anyString());
                    break;
                case "CAM_TURNED_OFF":
                    verify(webRTCListener, times(1)).onCameraTurnOffFor(anyString());
                    break;
                case "UPDATE_STATUS":
                    verify(webRTCListener, times(1)).onSatatusUpdateFor(anyString(), anyBoolean(), anyBoolean());
                    break;
                default:
                    // For other event types, none of the webRTCListener methods should be called
                    verifyZeroInteractions(webRTCListener);
                    break;
            }

            // Reset the mock interactions
            reset(webRTCListener);
        }
    }
}
