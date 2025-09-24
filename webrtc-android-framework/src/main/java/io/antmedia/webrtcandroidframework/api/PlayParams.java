package io.antmedia.webrtcandroidframework.api;

public class PlayParams {
    private String streamId;
    private String token;
    private String[] tracks;
    private String subscriberId;
    private String subscriberName;          // new field
    private String subscriberCode;
    private String viewerInfo;
    private boolean disableTracksByDefault; // new field

    // Constructors
    public PlayParams() {}

    public PlayParams(String streamId, String token, String[] tracks,
                      String subscriberId, String subscriberName,
                      String subscriberCode, String viewerInfo,
                      boolean disableTracksByDefault) {
        this.streamId = streamId;
        this.token = token;
        this.tracks = tracks;
        this.subscriberId = subscriberId;
        this.subscriberName = subscriberName;
        this.subscriberCode = subscriberCode;
        this.viewerInfo = viewerInfo;
        this.disableTracksByDefault = disableTracksByDefault;
    }

    // Getters and Setters
    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String[] getTracks() { return tracks; }
    public void setTracks(String[] tracks) { this.tracks = tracks; }

    public String getSubscriberId() { return subscriberId; }
    public void setSubscriberId(String subscriberId) { this.subscriberId = subscriberId; }

    public String getSubscriberName() { return subscriberName; }
    public void setSubscriberName(String subscriberName) { this.subscriberName = subscriberName; }

    public String getSubscriberCode() { return subscriberCode; }
    public void setSubscriberCode(String subscriberCode) { this.subscriberCode = subscriberCode; }

    public String getViewerInfo() { return viewerInfo; }
    public void setViewerInfo(String viewerInfo) { this.viewerInfo = viewerInfo; }

    public boolean isDisableTracksByDefault() { return disableTracksByDefault; }
    public void setDisableTracksByDefault(boolean disableTracksByDefault) { this.disableTracksByDefault = disableTracksByDefault; }
}
