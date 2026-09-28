package io.antmedia.webrtcandroidframework.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReconnectionStateMachineTest {

    @Test
    public void testReconnectLifecycle() {
        ReconnectionStateMachine state = new ReconnectionStateMachine();

        assertEquals(ReconnectionStateMachine.Phase.IDLE, state.getPhase());
        assertTrue(state.schedule());
        assertEquals(ReconnectionStateMachine.Phase.SCHEDULED, state.getPhase());
        assertFalse(state.schedule());

        state.forceNextAttempt();
        state.beginAttempt();
        assertEquals(ReconnectionStateMachine.Phase.RECONNECTING, state.getPhase());
        assertFalse(state.isForced());
        assertEquals(1, state.getAttemptCount());

        state.markStopSent();
        state.markWebSocketReconnectRequested();
        assertTrue(state.isStopSent());
        assertTrue(state.isWebSocketReconnectRequested());

        state.beginAttempt();
        assertEquals(ReconnectionStateMachine.Phase.WEBSOCKET_RECONNECT_REQUESTED, state.getPhase());
        assertEquals(2, state.getAttemptCount());

        state.reset();
        assertEquals(ReconnectionStateMachine.Phase.IDLE, state.getPhase());
        assertFalse(state.isActive());
        assertFalse(state.isForced());
        assertFalse(state.isStopSent());
        assertEquals(0, state.getAttemptCount());
    }

    @Test
    public void testForceIsPreservedWhileReconnectIsScheduled() {
        ReconnectionStateMachine state = new ReconnectionStateMachine();

        state.forceNextAttempt();
        assertTrue(state.schedule());

        assertTrue(state.isForced());
    }
}
