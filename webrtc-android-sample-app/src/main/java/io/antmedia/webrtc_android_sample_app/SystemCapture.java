package io.antmedia.webrtc_android_sample_app;

import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.genymobile.scrcpy.AudioCapture;
import com.genymobile.scrcpy.AudioCaptureForegroundException;
import com.genymobile.scrcpy.ConfigurationException;
import com.genymobile.scrcpy.Device;
import com.genymobile.scrcpy.FakeContext;
import com.genymobile.scrcpy.Options;
import com.genymobile.scrcpy.Server;
import com.genymobile.scrcpy.Workarounds;
import com.genymobile.scrcpy.wrappers.ServiceManager;

import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import io.antmedia.webrtcandroidframework.AntMediaSignallingEvents;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.WebSocketHandler;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;
import io.antmedia.webrtcandroidframework.scrcpy.DisplayCapturerAndroid;

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

       // Looper.prepareMainLooper();
/*
        HandlerThread handlerThread = new HandlerThread("handler-thread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());

        handler.post(()-> {
                Log.d(TAG, "Handler post is running");
                WebSocketConnection ws = new WebSocketConnection();
                try {
                    ws.connect(new URI("wss://ovh36.antmedia.io:5443/WebRTCAppEE/websocket"), observer);
                } catch (WebSocketException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Leaving the connect method");
        });

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "observer is opened: " + isOpened);
*/


        Workarounds.apply(true);

        Options options = Options.parse();


        Device device = null;
        try {

            device = new Device(options);



            int width = device.getScreenInfo().getVideoSize().getWidth();
            int height = device.getScreenInfo().getVideoSize().getHeight();

            HandlerThread thread = new HandlerThread("handler-thread");
            thread.start();
            Handler handler = new Handler(thread.getLooper());

            handler.post(() -> {
                WebRTCClient webRTCClient = new WebRTCClient(systemCapture, FakeContext.get());
                webRTCClient.setAudioService(ServiceManager.getAudioManager());

                Intent intent = new Intent();
                intent.putExtra(WebRTCClient.EXTRA_DISPLAY_CAPTURE, true);
                intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, width);
                intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, height);
                Log.i(TAG, "screen width: " + width + " screen height: " + height);

                webRTCClient.init("wss://ovh36.antmedia.io:5443/WebRTCAppEE/websocket", "stream1",
                        IWebRTCClient.MODE_PUBLISH, "", intent);

                Log.i(TAG, "WebRTC Client initialized");



                webRTCClient.startStream();
            });


            Log.i(TAG, "Looping in main call");


            Server.Completion completion = new Server.Completion(1);

            completion.await();



        } catch (ConfigurationException e) {
            e.printStackTrace();
        }



        Log.i(TAG, "Leaving main application");



        //testAudioCapture();

    }


    public static void testAudioCapture() {
        Options options = Options.parse();

        AudioCapture audioCapture = new AudioCapture(options.getAudioSource());

        try {
            audioCapture.start();
        } catch (AudioCaptureForegroundException e) {
            e.printStackTrace();
        }

        if (audioCapture.getRecorder().getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.w(TAG, "Recording state is not RECORDSTATE_RECORDING");
        }
        else {
            Log.w(TAG, "Recording state is true");
        }
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
