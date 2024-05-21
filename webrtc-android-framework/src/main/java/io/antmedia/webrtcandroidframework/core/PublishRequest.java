package io.antmedia.webrtcandroidframework.core;

public class PublishRequest {
    private String streamId;
    private String token;
    private boolean videoCallEnabled;
    private boolean audioCallEnabled;
    private String subscriberId;
    private String subscriberCode;
    private String streamName;
    private String mainTrackId;

    public PublishRequest(String streamId, String token, boolean videoCallEnabled, boolean audioCallEnabled,
                          String subscriberId, String subscriberCode, String streamName, String mainTrackId) {
        this.streamId = streamId;
        this.token = token;
        this.videoCallEnabled = videoCallEnabled;
        this.audioCallEnabled = audioCallEnabled;
        this.subscriberId = subscriberId;
        this.subscriberCode = subscriberCode;
        this.streamName = streamName;
        this.mainTrackId = mainTrackId;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getToken() {
        return token;
    }

    public boolean isVideoCallEnabled() {
        return videoCallEnabled;
    }

    public boolean isAudioCallEnabled() {
        return audioCallEnabled;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getSubscriberCode() {
        return subscriberCode;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getMainTrackId() {
        return mainTrackId;
    }
}
