package io.antmedia.webrtc_android_sample_app;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;
import org.webrtc.SurfaceViewRenderer;

import io.antmedia.webrtcandroidframework.WebRTCClient;

public class TrackBasedConferenceActivityUnitTest {


    @Test
    public void testControlAudio() {
        TrackBasedConferenceActivity trackBasedConferenceActivity = Mockito.spy(new TrackBasedConferenceActivity());
        doNothing().when(trackBasedConferenceActivity).sendNotificationEvent(anyString(), Mockito.any(JSONObject.class));

        Button audioButton = Mockito.mock(Button.class);
        doNothing().when(audioButton).setText(anyString());

        WebRTCClient webRTCClient = Mockito.mock(WebRTCClient.class);
        doNothing().when(webRTCClient).enableAudio();
        doNothing().when(webRTCClient).disableAudio();

        trackBasedConferenceActivity.setWebRTCClient(webRTCClient);
        trackBasedConferenceActivity.setAudioButton(audioButton);

        Mockito.verify(webRTCClient, Mockito.times(0)).disableAudio();
        Mockito.verify(webRTCClient, Mockito.times(0)).enableAudio();

        when(webRTCClient.isAudioOn()).thenReturn(true);
        trackBasedConferenceActivity.controlAudio(null);
        Mockito.verify(webRTCClient, Mockito.times(1)).disableAudio();
        Mockito.verify(webRTCClient, Mockito.times(0)).enableAudio();

        when(webRTCClient.isAudioOn()).thenReturn(false);
        trackBasedConferenceActivity.controlAudio(null);
        Mockito.verify(webRTCClient, Mockito.times(1)).disableAudio();
        Mockito.verify(webRTCClient, Mockito.times(1)).enableAudio();
    }
}