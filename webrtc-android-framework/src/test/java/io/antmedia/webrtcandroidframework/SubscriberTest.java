package io.antmedia.webrtcandroidframework;

import org.junit.Test;
import static org.junit.Assert.*;

import io.antmedia.webrtcandroidframework.websocket.Subscriber;

public class SubscriberTest {

    @Test
    public void testSubscriberFields() {
        Subscriber subscriber = new Subscriber();

        subscriber.setSubscriberId("sub123");
        assertEquals("sub123", subscriber.getSubscriberId());

        subscriber.setStreamId("streamABC");
        assertEquals("streamABC", subscriber.getStreamId());

        subscriber.setConnected(true);
        assertTrue(subscriber.isConnected());

        subscriber.setCurrentConcurrentConnections(3);
        assertEquals(3, subscriber.getCurrentConcurrentConnections());

        subscriber.setConcurrentConnectionsLimit(10);
        assertEquals(10, subscriber.getConcurrentConnectionsLimit());
    }

    @Test
    public void testDefaultValues() {
        Subscriber subscriber = new Subscriber();

        assertNull(subscriber.getSubscriberId());
        assertNull(subscriber.getStreamId());
        assertFalse(subscriber.isConnected());
        assertEquals(0, subscriber.getCurrentConcurrentConnections());
        assertEquals(1, subscriber.getConcurrentConnectionsLimit());
    }
}
