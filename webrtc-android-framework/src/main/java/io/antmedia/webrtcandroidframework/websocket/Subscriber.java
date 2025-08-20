package io.antmedia.webrtcandroidframework.websocket;


public class Subscriber {

    private String subscriberId;

	private String subscriberName;
	private String streamId;
    private boolean connected;
    private int currentConcurrentConnections = 0;
    private int concurrentConnectionsLimit = 1;

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}	

	public String getStreamId() {
		return streamId;
	}
	public boolean isConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public int getCurrentConcurrentConnections() {
		return currentConcurrentConnections;
	}

	public void setCurrentConcurrentConnections(int currentConcurrentConnections) {
		this.currentConcurrentConnections = currentConcurrentConnections;
	}

	public int getConcurrentConnectionsLimit() {
		return concurrentConnectionsLimit;
	}

	public void setConcurrentConnectionsLimit(int concurrentConnectionsLimit) {
		this.concurrentConnectionsLimit = concurrentConnectionsLimit;
	}

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }
}