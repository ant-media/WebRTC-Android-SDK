/*
 * Interface to feed the application with video frames.
 * Application that consumes the video frames should implement this interface.
 */

package io.antmedia.webrtcandroidframework.apprtc;

import org.webrtc.VideoFrame;

/*
 * SDK send frames with this method to the listener.
 * @param frame is the captured video frame
 * @returns VideoFrame this may have 3 different values according to the process on the application layer
 * 			1. Same as input: SDK sends this original frame to the peer
 * 			2. Modified frame: SDK sends this modified frame to the peer
 * 			3. null: SDK doesn't frame to the peer
 */
public interface IFrameListener {
    public VideoFrame onFrame(VideoFrame frame);
}
