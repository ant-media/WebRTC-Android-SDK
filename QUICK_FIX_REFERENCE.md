# Quick Reference: Race Condition Fix for Multiple WebRTC Reconnections

## What Was Fixed

Native crash (SIGSEGV) when multiple WebRTC connections reconnect simultaneously.

## Key Pattern Applied

### Before (Unsafe):
```java
PeerConnection pc = peerInfo.peerConnection;
if (pc != null) {
    pc.close();
}
// peerConnection reference still points to closed object!
```

### After (Thread-Safe):
```java
synchronized (peerInfo) {
    PeerConnection pc = peerInfo.peerConnection;
    if (pc != null) {
        try {
            pc.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing peer connection", e);
        }
        peerInfo.peerConnection = null;  // Clear reference
    }
}
```

## Critical Changes Summary

| Location | Change | Why |
|----------|--------|-----|
| Reconnection Runnables | Added `synchronized(peerInfo)` + null assignment | Prevent concurrent close/access |
| `createPeerConnectionInternal()` | Synchronized peer assignment | Prevent assignment during close |
| SDP Callbacks | Synchronized peer access | Prevent access to closing connection |
| `closeInternal()` | Synchronized cleanup loop | Prevent concurrent modification |
| `getPeerConnectionFor()` | Synchronized return | Ensure consistent view |
| `drainCandidates()` | Synchronized entire method | Prevent ICE candidate race |
| `initDataChannel()` | Synchronized channel creation | Prevent creation on closing peer |

## Rule of Thumb

**Always synchronize on `peerInfo` when:**
1. Accessing `peerInfo.peerConnection`
2. Modifying `peerInfo.peerConnection`
3. Calling methods on `peerInfo.peerConnection`
4. Closing or disposing peer connections

## Pattern to Follow

```java
PeerInfo peerInfo = getPeerInfoFor(streamId);
if (peerInfo != null) {
    synchronized (peerInfo) {
        PeerConnection pc = peerInfo.peerConnection;
        if (pc != null) {
            try {
                // Do something with pc
                pc.someMethod();
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }
    }
}
```

## What NOT to Do

❌ **Don't**: Access `peerConnection` without synchronization
❌ **Don't**: Keep references to `PeerConnection` outside synchronized blocks
❌ **Don't**: Call `close()` without setting the reference to null
❌ **Don't**: Assume the native object is valid after getting the reference

## Testing Checklist

- [ ] Multiple connections reconnecting simultaneously
- [ ] Conference mode with many participants
- [ ] Rapid connect/disconnect cycles
- [ ] Network instability simulation
- [ ] Memory leak check (no dangling references)

## Build Verification

```bash
./gradlew :webrtc-android-framework:assembleDebug
```

Expected: ✅ BUILD SUCCESSFUL

## Branch

`improveReconnection`

