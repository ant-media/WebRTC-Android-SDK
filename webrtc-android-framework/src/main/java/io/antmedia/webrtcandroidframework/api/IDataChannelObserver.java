package io.antmedia.webrtcandroidframework.api;

import org.webrtc.DataChannel;

/**
 * This interface is used to observe the data channel events
 * You can use this interface to observe the data channel events or you can extend {@link DefaultDataChannelObserver}
 */
public interface IDataChannelObserver {

    /** The data channel's bufferedAmount has changed. */
    void onBufferedAmountChange(long previousAmount, String dataChannelLabel);
    /** The data channel state has changed. */
    void onStateChange(DataChannel.State state, String dataChannelLabel);
    /**
     * A data buffer was successfully received.
     */
    void onMessage(DataChannel.Buffer buffer, String dataChannelLabel);

    void onMessageSent(DataChannel.Buffer buffer, boolean successful);
}
