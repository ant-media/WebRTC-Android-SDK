package io.antmedia.webrtcandroidframework.apprtc;

import org.webrtc.DataChannel.Buffer;

public interface IDataChannelMessageSender  {
    void sendMessageViaDataChannel(Buffer buffer);
}
