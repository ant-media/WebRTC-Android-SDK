package io.antmedia.webrtcandroidframework.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;

public class DefaultDataChannelObserverTest {

    private DefaultDataChannelObserver defaultDataChannelObserver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        defaultDataChannelObserver = spy(new DefaultDataChannelObserver());
    }

    @Test
    public void testOnMessage() {
        String message = "Test Message";
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        DataChannel.Buffer mockBuffer = new DataChannel.Buffer(data, false);
        defaultDataChannelObserver.onMessage(mockBuffer, "label");
        verify(defaultDataChannelObserver, times(1)).textMessageReceived(message);
    }

    @Test
    public void testToTextMessage() {
        String message = "Test Message";
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        DataChannel.Buffer mockBuffer = new DataChannel.Buffer(data, false);
        String result = defaultDataChannelObserver.toTextMessage(mockBuffer);
        assert result.equals(message);
    }

    @Test
    public void testOtherMethodsWithoutCallback() {
        String message = "Test Message";
        ByteBuffer data = ByteBuffer.wrap(message.getBytes());
        DataChannel.Buffer mockBuffer = new DataChannel.Buffer(data, false);
        defaultDataChannelObserver.onMessageSent(mockBuffer, true);
        defaultDataChannelObserver.onMessageSent(mockBuffer, false);

        defaultDataChannelObserver.onBufferedAmountChange(100, "label");

        defaultDataChannelObserver.onStateChange(DataChannel.State.OPEN, "label");
        //nothing for assertion
    }

}
