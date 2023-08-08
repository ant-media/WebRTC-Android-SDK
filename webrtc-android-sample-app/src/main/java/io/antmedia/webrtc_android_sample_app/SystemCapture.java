package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.scrcpy.DisplayCapturerAndroid.createDisplay;
import static io.antmedia.webrtcandroidframework.scrcpy.DisplayCapturerAndroid.setDisplaySurface;

import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.genymobile.scrcpy.AudioCaptureForegroundException;
import com.genymobile.scrcpy.ConfigurationException;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.FakeContext;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.ScreenInfo;
import com.genymobile.scrcpy.Workarounds;
import com.genymobile.scrcpy.wrappers.ServiceManager;
import com.genymobile.scrcpy.wrappers.SurfaceControl;

import org.webrtc.EglBase;
import org.webrtc.SurfaceTextureHelper;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class SystemCapture implements IWebRTCListener {
    public final static String TAG = SystemCapture.class.getSimpleName();


    public static boolean isOpened = false;

    static WebSocket.WebSocketConnectionObserver observer = new WebSocket.WebSocketConnectionObserver() {


        @Override
        public void onOpen() {
            isOpened = true;
            Log.i(TAG, "onOpen is called");
        }

        @Override
        public void onClose(WebSocketCloseNotification webSocketCloseNotification, String s) {

        }

        @Override
        public void onTextMessage(String s) {

        }

        @Override
        public void onRawTextMessage(byte[] bytes) {

        }

        @Override
        public void onBinaryMessage(byte[] bytes) {

        }
    };

    public static void main(String[] args) {
        SystemCapture systemCapture = new SystemCapture();

        Workarounds.apply(true);


        HandlerThread thread = new HandlerThread("handler-thread");
        thread.start();
        Handler handler = new Handler(thread.getLooper());

        handler.post(() -> {
            try {


                Device device = new Device(Options.parse());
                int width = device.getScreenInfo().getVideoSize().getWidth();
                int height = device.getScreenInfo().getVideoSize().getHeight();

                WebRTCClient webRTCClient = new WebRTCClient(systemCapture, FakeContext.get());
                webRTCClient.setAudioService(ServiceManager.getAudioManager());

                Intent intent = new Intent();
                intent.putExtra(WebRTCClient.EXTRA_DISPLAY_CAPTURE, true);
                intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, width);
                intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, height);
                intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, 2500);
                Log.i(TAG, "screen width: " + width + " screen height: " + height);

                webRTCClient.setMediaRecorderAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
                webRTCClient.init("wss://ovh36.antmedia.io:5443/WebRTCAppEE/websocket", "stream1",
                        IWebRTCClient.MODE_PUBLISH, "", intent);

                Log.i(TAG, "WebRTC Client initialized");

                webRTCClient.startStream();
            }
            catch
            (ConfigurationException e) {
                e.printStackTrace();
            }
        });


        Log.i(TAG, "Looping in main call");

        Looper.loop();

        Log.i(TAG, "Leaving main application");


    }

    @Override
    public void onDisconnected(String streamId) {
        Log.i(TAG, "onDisconnected: " + streamId);
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.i(TAG, "onPublishFinished: " + streamId);
    }

    @Override
    public void onPlayFinished(String streamId) {

    }

    @Override
    public void onPublishStarted(String streamId) {

        Log.i(TAG, "onPublishStarted: " + streamId);

    }

    @Override
    public void onPlayStarted(String streamId) {

    }

    @Override
    public void noStreamExistsToPlay(String streamId) {

    }

    @Override
    public void onError(String description, String streamId) {

    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {

    }

    @Override
    public void streamIdInUse(String streamId) {

    }

    @Override
    public void onIceConnected(String streamId) {

    }

    @Override
    public void onIceDisconnected(String streamId) {

    }

    @Override
    public void onTrackList(String[] tracks) {

    }

    @Override
    public void onBitrateMeasurement(String streamId, int targetBitrate, int videoBitrate, int audioBitrate) {

    }

    @Override
    public void onStreamInfoList(String streamId, ArrayList<StreamInfo> streamInfoList) {

    }
}
