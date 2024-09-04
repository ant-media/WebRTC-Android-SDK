package io.antmedia.webrtcandroidframework.websocket;

import java.util.ArrayList;
import java.util.List;


public class Broadcast {
	private String streamId;
	private String status;
	private String playListStatus;
	private String type;
	private String publishType;
	private String name;
	private String description;
	private boolean publish = true;
	private long date;
	private long plannedStartDate;
	private long plannedEndDate;
	private long duration;
	private boolean publicStream = true;
	private boolean is360 = false;
	private String listenerHookURL;
	private String category;
	private String ipAddr;
	private String username;
	private String password;
	private String quality;
	private double speed;
	private String streamUrl;
	private String originAdress;
	private int mp4Enabled = 0;
	private int webMEnabled = 0;
	private int expireDurationMS;
	private String rtmpURL;
	private boolean zombi = false;
	private int pendingPacketSize = 0;

	private int hlsViewerCount = 0;
	private int dashViewerCount = 0;
	private int webRTCViewerCount = 0;
	private int rtmpViewerCount = 0;

	private long startTime = 0;

	private long receivedBytes = 0;

	private long bitrate = 0;

	private String userAgent = "N/A";

	private String latitude;

	private String longitude;

	private String altitude;

	private String mainTrackStreamId;

	private List<String> subTrackStreamIds = new ArrayList<String>();

	private long absoluteStartTimeMs;

	private int webRTCViewerLimit = -1;

	private int hlsViewerLimit = -1;
	
	private int dashViewerLimit = -1;

	private String subFolder;
	private int currentPlayIndex = 0;
	private String metaData = null;
	private boolean playlistLoopEnabled = true;
	private long updateTime = 0;


	public String getStreamId() {
			return streamId;
	}

	public void setStreamId(String id) {
		this.streamId = id;
	}


	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isPublish() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public long getPlannedStartDate() {
		return plannedStartDate;
	}

	public void setPlannedStartDate(long plannedStartDate) {
		this.plannedStartDate = plannedStartDate;
	}

	public long getPlannedEndDate() {
		return plannedEndDate;
	}

	public void setPlannedEndDate(long plannedEndDate) {
		this.plannedEndDate = plannedEndDate;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public boolean isIs360() {
		return is360;
	}

	public void setIs360(boolean is360) {
		this.is360 = is360;
	}

	public boolean isPublicStream() {
		return publicStream;
	}

	public void setPublicStream(boolean publicStream) {
		this.publicStream = publicStream;
	}

	public String getListenerHookURL() {
		return listenerHookURL;
	}

	public void setListenerHookURL(String listenerHookURL) {
		this.listenerHookURL = listenerHookURL;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	public int getExpireDurationMS() {
		return expireDurationMS;
	}

	public void setExpireDurationMS(int expireDurationMS) {
		this.expireDurationMS = expireDurationMS;
	}

	public String getRtmpURL() {
		return rtmpURL;
	}

	public void setRtmpURL(String rtmpURL) {
		this.rtmpURL = rtmpURL;
	}


	public boolean isZombi() {
		return zombi;
	}

	public void setZombi(boolean zombi) {
		this.zombi = zombi;
	}

	public void resetStreamId() {
		this.streamId = null;
	}

	public String getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}
	public int getHlsViewerCount() {
		return hlsViewerCount;
	}

	public void setHlsViewerCount(int hlsViewerCount) {
		this.hlsViewerCount = hlsViewerCount;
	}

	public int getWebRTCViewerCount() {
		return webRTCViewerCount;
	}

	public void setWebRTCViewerCount(int webRTCViewerCount) {
		this.webRTCViewerCount = webRTCViewerCount;
	}

	public int getRtmpViewerCount() {
		return rtmpViewerCount;
	}

	public void setRtmpViewerCount(int rtmpViewerCount) {
		this.rtmpViewerCount = rtmpViewerCount;
	}

	public int getPendingPacketSize() {
		return pendingPacketSize;
	}

	public void setPendingPacketSize(int pendingPacketSize) {
		this.pendingPacketSize = pendingPacketSize;
	}


	public String getOriginAdress() {
		return originAdress;
	}

	public void setOriginAdress(String originAdress) {
		this.originAdress = originAdress;
	}
	public int getMp4Enabled() {
		return mp4Enabled;
	}

	public void setMp4Enabled(int mp4Enabled) {
		this.mp4Enabled = mp4Enabled;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getReceivedBytes() {
		return receivedBytes;
	}

	public void setReceivedBytes(long receivedBytes) {
		this.receivedBytes = receivedBytes;
	}

	public long getBitrate() {
		return bitrate;
	}

	public void setBitrate(long bitrate) {
		this.bitrate = bitrate;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getAltitude() {
		return altitude;
	}

	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	public String getMainTrackStreamId() {
		return mainTrackStreamId;
	}

	public void setMainTrackStreamId(String mainTrackStreamId) {
		this.mainTrackStreamId = mainTrackStreamId;
	}

	public List<String> getSubTrackStreamIds() {
		return subTrackStreamIds;
	}

	public void setSubTrackStreamIds(List<String> subTrackStreamIds) {
		this.subTrackStreamIds = subTrackStreamIds;
	}

	public void setAbsoluteStartTimeMs(long absoluteStartTimeMs) {
		this.absoluteStartTimeMs = absoluteStartTimeMs;
	}

	public long getAbsoluteStartTimeMs() {
		return absoluteStartTimeMs;
	}

	public int getWebMEnabled() {
		return webMEnabled;
	}

	public void setWebMEnabled(int webMEnabled) {
		this.webMEnabled = webMEnabled;
	}

	public int getWebRTCViewerLimit() {
		return webRTCViewerLimit;
	}

	public void setWebRTCViewerLimit(int webRTCViewerLimit) {
		this.webRTCViewerLimit = webRTCViewerLimit;
	}

	public int getHlsViewerLimit() {
		return hlsViewerLimit;
	}

	public void setHlsViewerLimit(int hlsViewerLimit) {
		this.hlsViewerLimit = hlsViewerLimit;
	}

	public int getCurrentPlayIndex() {
		return currentPlayIndex ;
	}

	public void setCurrentPlayIndex(int currentPlayIndex) {
		this.currentPlayIndex = currentPlayIndex;
	}

	public void setPlayListStatus(String playListStatus) {
		this.playListStatus = playListStatus;
	}

	public String getPlayListStatus() {
		return playListStatus;
	}

	public void setSubFolder(String subFolder) { this.subFolder=subFolder; }

	public String getSubFolder() { return subFolder; }
	
	public String getPublishType() {
		return publishType;
	}

	public void setPublishType(String publishType) {
		this.publishType = publishType;
	}

	public String getMetaData() {
		return metaData;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}
	
	public boolean isPlaylistLoopEnabled() {
		return playlistLoopEnabled;
	}

	public void setPlaylistLoopEnabled(boolean playlistLoopEnabled) {
		this.playlistLoopEnabled = playlistLoopEnabled;
	}
	
	public int getDashViewerLimit() {
		return dashViewerLimit;
	}

	public void setDashViewerLimit(int dashViewerLimit) {
		this.dashViewerLimit = dashViewerLimit;
	}
	
	public int getDashViewerCount() {
		return dashViewerCount;
	}

	public void setDashViewerCount(int dashViewerCount) {
		this.dashViewerCount = dashViewerCount;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

}
