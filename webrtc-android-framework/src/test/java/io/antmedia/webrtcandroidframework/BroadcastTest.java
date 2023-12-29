package io.antmedia.webrtcandroidframework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import io.antmedia.webrtcandroidframework.websocket.Broadcast;

public class BroadcastTest {

    Broadcast broadcast;
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        broadcast = new Broadcast();
    }


    @Test
    public void testStreamId() {
        assertNull(broadcast.getStreamId());
        broadcast.setStreamId("testStreamId");
        assertEquals("testStreamId", broadcast.getStreamId());
    }

    @Test
    public void testStatus() {
        assertNull(broadcast.getStatus());
        broadcast.setStatus("testStatus");
        assertEquals("testStatus", broadcast.getStatus());
    }

    @Test
    public void testPlayListStatus() {
        assertNull(broadcast.getPlayListStatus());
        broadcast.setPlayListStatus("testPlayListStatus");
        assertEquals("testPlayListStatus", broadcast.getPlayListStatus());
    }


    @Test
    public void testUpdateTime() {
        assertEquals(0, broadcast.getUpdateTime());
        broadcast.setUpdateTime(5000);
        assertEquals(5000, broadcast.getUpdateTime());
    }

    @Test
    public void testPlannedStartDate() {
        assertEquals(0, broadcast.getPlannedStartDate());
        broadcast.setPlannedStartDate(1000);
        assertEquals(1000, broadcast.getPlannedStartDate());
    }

    @Test
    public void testPlannedEndDate() {
        assertEquals(0, broadcast.getPlannedEndDate());
        broadcast.setPlannedEndDate(2000);
        assertEquals(2000, broadcast.getPlannedEndDate());
    }

    @Test
    public void testDuration() {
        assertEquals(0, broadcast.getDuration());
        broadcast.setDuration(5000L);
        assertEquals(5000, broadcast.getDuration());
    }

    @Test
    public void testIs360() {
        assertFalse(broadcast.isIs360());
        broadcast.setIs360(true);
        assertTrue(broadcast.isIs360());
    }

    @Test
    public void testPublicStream() {
        assertTrue(broadcast.isPublicStream());
        broadcast.setPublicStream(false);
        assertFalse(broadcast.isPublicStream());
    }

    @Test
    public void testListenerHookURL() {
        assertNull(broadcast.getListenerHookURL());
        broadcast.setListenerHookURL("https://example.com/hook");
        assertEquals("https://example.com/hook", broadcast.getListenerHookURL());
    }

    @Test
    public void testCategory() {
        assertNull(broadcast.getCategory());
        broadcast.setCategory("TestCategory");
        assertEquals("TestCategory", broadcast.getCategory());
    }

    @Test
    public void testIpAddr() {
        assertNull(broadcast.getIpAddr());
        broadcast.setIpAddr("192.168.1.1");
        assertEquals("192.168.1.1", broadcast.getIpAddr());
    }

    @Test
    public void testUsername() {
        assertNull(broadcast.getUsername());
        broadcast.setUsername("testUser");
        assertEquals("testUser", broadcast.getUsername());
    }

    @Test
    public void testPassword() {
        assertNull(broadcast.getPassword());
        broadcast.setPassword("testPassword");
        assertEquals("testPassword", broadcast.getPassword());
    }

    @Test
    public void testExpireDurationMS() {
        assertEquals(0, broadcast.getExpireDurationMS());
        broadcast.setExpireDurationMS(5000);
        assertEquals(5000, broadcast.getExpireDurationMS());
    }

    @Test
    public void testRtmpURL() {
        assertNull(broadcast.getRtmpURL());
        broadcast.setRtmpURL("rtmp://example.com/live");
        assertEquals("rtmp://example.com/live", broadcast.getRtmpURL());
    }

    @Test
    public void testZombi() {
        assertFalse(broadcast.isZombi());
        broadcast.setZombi(true);
        assertTrue(broadcast.isZombi());
    }

    @Test
    public void testStreamUrl() {
        assertNull(broadcast.getStreamUrl());
        broadcast.setStreamUrl("https://example.com/stream");
        assertEquals("https://example.com/stream", broadcast.getStreamUrl());
    }

    @Test
    public void testHlsViewerCount() {
        assertEquals(0, broadcast.getHlsViewerCount());
        broadcast.setHlsViewerCount(10);
        assertEquals(10, broadcast.getHlsViewerCount());
    }

    @Test
    public void testWebRTCViewerCount() {
        assertEquals(0, broadcast.getWebRTCViewerCount());
        broadcast.setWebRTCViewerCount(5);
        assertEquals(5, broadcast.getWebRTCViewerCount());
    }

    @Test
    public void testRtmpViewerCount() {
        assertEquals(0, broadcast.getRtmpViewerCount());
        broadcast.setRtmpViewerCount(10);
        assertEquals(10, broadcast.getRtmpViewerCount());
    }

    @Test
    public void testPendingPacketSize() {
        assertEquals(0, broadcast.getPendingPacketSize());
        broadcast.setPendingPacketSize(100);
        assertEquals(100, broadcast.getPendingPacketSize());
    }

    @Test
    public void testOriginAddress() {
        assertNull(broadcast.getOriginAdress());
        broadcast.setOriginAdress("192.168.1.1");
        assertEquals("192.168.1.1", broadcast.getOriginAdress());
    }

    @Test
    public void testMp4Enabled() {
        assertEquals(0, broadcast.getMp4Enabled());
        broadcast.setMp4Enabled(1);
        assertEquals(1, broadcast.getMp4Enabled());
    }

    @Test
    public void testStartTime() {
        assertEquals(0, broadcast.getStartTime());
        broadcast.setStartTime(5000);
        assertEquals(5000, broadcast.getStartTime());
    }

    @Test
    public void testUserAgent() {
        assertEquals("N/A", broadcast.getUserAgent());
        broadcast.setUserAgent("Test User Agent");
        assertEquals("Test User Agent", broadcast.getUserAgent());
    }

    @Test
    public void testLatitude() {
        assertNull(broadcast.getLatitude());
        broadcast.setLatitude("40.7128° N");
        assertEquals("40.7128° N", broadcast.getLatitude());
    }

    @Test
    public void testLongitude() {
        assertNull(broadcast.getLongitude());
        broadcast.setLongitude("74.0060° W");
        assertEquals("74.0060° W", broadcast.getLongitude());
    }

    @Test
    public void testAltitude() {
        assertNull(broadcast.getAltitude());
        broadcast.setAltitude("100 meters");
        assertEquals("100 meters", broadcast.getAltitude());
    }

    @Test
    public void testMainTrackStreamId() {
        assertNull(broadcast.getMainTrackStreamId());
        broadcast.setMainTrackStreamId("mainStreamId");
        assertEquals("mainStreamId", broadcast.getMainTrackStreamId());
    }

    @Test
    public void testSubTrackStreamIds() {
        List<String> subTrackStreamIds = new ArrayList<>();
        subTrackStreamIds.add("sub1");
        subTrackStreamIds.add("sub2");

        broadcast.setSubTrackStreamIds(subTrackStreamIds);
        assertEquals(subTrackStreamIds, broadcast.getSubTrackStreamIds());
    }

    @Test
    public void testAbsoluteStartTimeMs() {
        assertEquals(0, broadcast.getAbsoluteStartTimeMs());
        broadcast.setAbsoluteStartTimeMs(1000);
        assertEquals(1000, broadcast.getAbsoluteStartTimeMs());
    }

    @Test
    public void testWebMEnabled() {
        assertEquals(0, broadcast.getWebMEnabled());
        broadcast.setWebMEnabled(1);
        assertEquals(1, broadcast.getWebMEnabled());
    }

    @Test
    public void testWebRTCViewerLimit() {
        assertEquals(-1, broadcast.getWebRTCViewerLimit());
        broadcast.setWebRTCViewerLimit(100);
        assertEquals(100, broadcast.getWebRTCViewerLimit());
    }

    @Test
    public void testHlsViewerLimit() {
        assertEquals(-1, broadcast.getHlsViewerLimit());
        broadcast.setHlsViewerLimit(50);
        assertEquals(50, broadcast.getHlsViewerLimit());
    }

    @Test
    public void testCurrentPlayIndex() {
        assertEquals(0, broadcast.getCurrentPlayIndex());
        broadcast.setCurrentPlayIndex(5);
        assertEquals(5, broadcast.getCurrentPlayIndex());
    }

    @Test
    public void testSubFolder() {
        assertNull(broadcast.getSubFolder());
        broadcast.setSubFolder("testSubFolder");
        assertEquals("testSubFolder", broadcast.getSubFolder());
    }

    @Test
    public void testPublishType() {
        assertNull(broadcast.getPublishType());
        broadcast.setPublishType("testPublishType");
        assertEquals("testPublishType", broadcast.getPublishType());
    }

    @Test
    public void testMetaData() {
        assertNull(broadcast.getMetaData());
        broadcast.setMetaData("testMetaData");
        assertEquals("testMetaData", broadcast.getMetaData());
    }

    @Test
    public void testPlaylistLoopEnabled() {
        assertTrue(broadcast.isPlaylistLoopEnabled());
        broadcast.setPlaylistLoopEnabled(false);
        assertFalse(broadcast.isPlaylistLoopEnabled());
    }

    @Test
    public void testDashViewerLimit() {
        assertEquals(-1, broadcast.getDashViewerLimit());
        broadcast.setDashViewerLimit(10);
        assertEquals(10, broadcast.getDashViewerLimit());
    }

    @Test
    public void testDashViewerCount() {
        assertEquals(0, broadcast.getDashViewerCount());
        broadcast.setDashViewerCount(5);
        assertEquals(5, broadcast.getDashViewerCount());
    }

    @Test
    public void testReceivedBytes() {
        assertEquals(0, broadcast.getReceivedBytes());
        broadcast.setReceivedBytes(1000);
        assertEquals(1000, broadcast.getReceivedBytes());
    }

    @Test
    public void testSpeed() {
        assertEquals(0.0, broadcast.getSpeed(), 0.0);
        broadcast.setSpeed(5.5);
        assertEquals(5.5, broadcast.getSpeed(), 0.0);
    }

    @Test
    public void testQuality() {
        assertNull(broadcast.getQuality());
        broadcast.setQuality("HD");
        assertEquals("HD", broadcast.getQuality());
    }

    @Test
    public void testResetStreamId() {
        broadcast.setStreamId("testStreamId");
        assertEquals("testStreamId", broadcast.getStreamId());

        broadcast.resetStreamId();
        assertNull(broadcast.getStreamId());
    }
}
