package io.antmedia.webrtcandroidframework;

import org.webrtc.DataChannel.Buffer;

public interface IDataChannelMessageSender {
    void sendMessageViaDataChannel(Buffer buffer);
}
