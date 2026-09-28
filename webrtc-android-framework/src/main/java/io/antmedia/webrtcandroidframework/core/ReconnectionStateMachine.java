package io.antmedia.webrtcandroidframework.core;

final class ReconnectionStateMachine {

    enum Phase {
        IDLE,
        SCHEDULED,
        RECONNECTING,
        WEBSOCKET_RECONNECT_REQUESTED
    }

    private Phase phase = Phase.IDLE;
    private boolean forced;
    private boolean stopSent;
    private int attemptCount;

    boolean schedule() {
        if (isActive()) {
            return false;
        }
        phase = Phase.SCHEDULED;
        stopSent = false;
        attemptCount = 0;
        return true;
    }

    void beginAttempt() {
        if (phase != Phase.WEBSOCKET_RECONNECT_REQUESTED) {
            phase = Phase.RECONNECTING;
        }
        forced = false;
        attemptCount++;
    }

    void forceNextAttempt() {
        forced = true;
    }

    boolean isForced() {
        return forced;
    }

    boolean isActive() {
        return phase != Phase.IDLE;
    }

    boolean isStopSent() {
        return stopSent;
    }

    void markStopSent() {
        stopSent = true;
    }

    int getAttemptCount() {
        return attemptCount;
    }

    boolean isWebSocketReconnectRequested() {
        return phase == Phase.WEBSOCKET_RECONNECT_REQUESTED;
    }

    void markWebSocketReconnectRequested() {
        phase = Phase.WEBSOCKET_RECONNECT_REQUESTED;
    }

    void reset() {
        phase = Phase.IDLE;
        forced = false;
        stopSent = false;
        attemptCount = 0;
    }

    Phase getPhase() {
        return phase;
    }

    @Override
    public String toString() {
        return "phase=" + phase
                + ", attempts=" + attemptCount
                + ", stopSent=" + stopSent
                + ", forced=" + forced;
    }
}
