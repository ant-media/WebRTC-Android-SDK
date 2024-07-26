package io.antmedia.webrtcandroidframework.websocket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;


public class WebSocketHandlerTest {

    @Mock
    private AntMediaSignallingEvents signallingListener;

    @Mock
    private Handler handler;

    private WebSocketHandler webSocketHandler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        webSocketHandler = spy(new WebSocketHandler(signallingListener, handler));
    }

    /*
    @Test
    public void registerPushNotificationToken_sendsCorrectMessage() {
        String subscriberId = "subscriberId";
        String authToken = "authToken";
        String pushNotificationToken = "pushNotificationToken";
        String tokenType = "tokenType";

        webSocketHandler.registerPushNotificationToken(subscriberId, authToken, pushNotificationToken, tokenType);

        verify(webSocketHandler, times(1)).sendTextMessage("{\"command\":\"registerPushNotificationToken\",\"subscriberId\":\"subscriberId\",\"token\":\"authToken\",\"pnsRegistrationToken\":\"pushNotificationToken\",\"pnsType\":\"tokenType\"}");
    }

    @Test
    public void sendPushNotification_sendsCorrectMessage() {
        String subscriberId = "subscriberId";
        String authToken = "authToken";
        String pushNotificationContent = "pushNotificationContent";
        String subscriberIdsToNotify = "subscriberIdsToNotify";

        webSocketHandler.sendPushNotification(subscriberId, authToken, pushNotificationContent, subscriberIdsToNotify);

        verify(webSocketHandler, times(1)).sendTextMessage("{\"command\":\"sendPushNotification\",\"subscriberId\":\"subscriberId\",\"token\":\"authToken\",\"pushNotificationContent\":\"pushNotificationContent\",\"subscriberIdsToNotify\":\"subscriberIdsToNotify\"}");
    }
    */
}