package io.antmedia.webrtcandroidframework;

import org.webrtc.DataChannel;

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
