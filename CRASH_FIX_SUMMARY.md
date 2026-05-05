# WebRTC Multiple Connection Reconnection Race Condition Fix

## Problem Summary

The application was experiencing native crashes (SIGSEGV - Signal 11) when multiple WebRTC connections attempted to reconnect simultaneously. The crash occurred in the native `libjingle_peerconnection_so.so` library on the `network_thread`.

### Crash Details
- **Signal**: SIGSEGV (Segmentation Fault)
- **Location**: Native WebRTC library (`libjingle_peerconnection_so.so`)
- **Thread**: network_thread
- **Trigger**: Multiple WebRTC connections reconnecting at the same time

### Root Cause Analysis

The crash was caused by **critical race conditions** in the peer connection management code:

1. **Unsynchronized Access**: Multiple threads (reconnection handlers, executor threads, network threads) were accessing `PeerInfo.peerConnection` without proper synchronization.

2. **Dangling References**: After calling `pc.close()`, the `peerConnection` reference wasn't being set to `null`, allowing subsequent code to access closed connections.

3. **Timing Issues**: The native `network_thread` in WebRTC was attempting to access peer connection objects while they were being closed on another thread.

4. **Multiple Reconnection Handlers**: Three separate reconnection runnables (`publishReconnectorRunnable`, `playReconnectorRunnable`, `peerReconnectorRunnable`) could run concurrently and attempt to close/recreate the same connections.

## Solution Implemented

Added **comprehensive thread synchronization** using the `PeerInfo` object as a lock monitor for all operations that access or modify peer connections.

### Changes Made

#### 1. Reconnection Runnables (Lines 266-433)
- Added `synchronized (peerInfo)` blocks around all peer connection access
- Added `peerInfo.peerConnection = null` after closing connections
- Wrapped `pc.close()` in try-catch blocks to handle exceptions gracefully

**Example:**
```java
synchronized (peerInfo) {
    PeerConnection pc = peerInfo.peerConnection;
    if (pc != null) {
        try {
            pc.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing peer connection during reconnection", e);
        }
        peerInfo.peerConnection = null;
    }
    // ... reconnection logic
}
```

#### 2. Peer Connection Creation (Line 2196-2198)
- Added synchronization when assigning the peer connection reference

```java
synchronized (peer) {
    peer.peerConnection = peerConnection;
}
```

#### 3. SDP Observer Methods (Lines 705-759)
- Added synchronization in `onCreateSuccess()` callback
- Protected access to peer connection during SDP operations

#### 4. WebSocket Connection Handler (Lines 1029-1038)
- Added synchronization when checking if peer connections need to be created

#### 5. Configuration Handler (Lines 1635-1641)
- Added synchronization when checking for null peer connections in offer handling

#### 6. ICE Candidate Handling (Lines 2526-2546)
- Added synchronization when adding remote ICE candidates

#### 7. Cleanup Operations (Lines 2293-2322)
- Added synchronization in `closeInternal()` method
- Protected cleanup of senders, tracks, and data channels
- Added exception handling for cleanup operations

#### 8. Helper Methods
- **`getPeerConnectionFor()`** (Lines 2886-2891): Added synchronization when accessing peer connection
- **`drainCandidates()`** (Lines 2862-2885): Added synchronization for the entire candidate draining process
- **`initDataChannel()`** (Lines 2262-2270): Added synchronization when creating data channels

## Technical Details

### Synchronization Strategy

- **Lock Object**: Each `PeerInfo` instance is used as its own lock monitor
- **Granularity**: Fine-grained locking at the individual peer level (not global lock)
- **Benefits**: 
  - Allows concurrent operations on different peers
  - Prevents race conditions on the same peer
  - Minimal performance impact

### Why This Fix Works

1. **Atomicity**: Operations on peer connections are now atomic - close and null assignment happen together
2. **Visibility**: Synchronized blocks ensure memory visibility across threads
3. **Ordering**: Prevents reordering of operations that could lead to accessing closed connections
4. **Native Thread Safety**: Prevents the native `network_thread` from accessing objects being destroyed

## Testing Recommendations

1. **Stress Test**: Simulate multiple simultaneous connection drops and reconnections
2. **Conference Mode**: Test with multiple publish and play streams in conference mode
3. **Network Instability**: Test with unstable network conditions to trigger frequent reconnections
4. **Memory Profiling**: Ensure no memory leaks from the synchronization changes
5. **Performance Testing**: Verify that synchronization doesn't introduce performance degradation

## Files Modified

- `webrtc-android-framework/src/main/java/io/antmedia/webrtcandroidframework/core/WebRTCClient.java`

## Build Status

✅ Build successful - No compilation errors or lint issues

## Additional Notes

- The existing comment in the code (lines 289-291) mentions that using `dispose()` instead of `close()` causes segmentation faults. This fix addresses a related but different race condition.
- The fix is backward compatible and doesn't change the public API
- All exception handling has been added to prevent crashes from propagating

## Prevention

To prevent similar issues in the future:

1. Always synchronize access to `peerInfo.peerConnection`
2. Set references to `null` after closing native objects
3. Use try-catch blocks around native operations
4. Consider the threading model when adding new peer connection operations

## Related Issues

This fix addresses the intermittent crash that occurred "time to time when multiple webrtc connection tries to reconnect at same time" as reported in the crash logs.

