package io.antmedia.webrtcandroidframework.core;

public class PlayRequest {
    private String streamId;
    private String token;
    private String[] tracks;
    private String subscriberId;
    private String subscriberCode;
    private String viewerInfo;

    public PlayRequest(String streamId, String token, String[] tracks,
                       String subscriberId, String subscriberCode, String viewerInfo) {
        this.streamId = streamId;
        this.token = token;
        this.tracks = tracks;
        this.subscriberId = subscriberId;
        this.subscriberCode = subscriberCode;
        this.viewerInfo = viewerInfo;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getToken() {
        return token;
    }

    public String[] getTracks() {
        return tracks;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getSubscriberCode() {
        return subscriberCode;
    }

    public String getViewerInfo() {
        return viewerInfo;
    }
}
