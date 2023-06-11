package io.antmedia.webrtc_android_sample_app;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_BITRATE;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_VIDEO_FPS;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import org.webrtc.DataChannel;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;
import org.webrtc.audio.CustomWebRtcAudioRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

public class CustomAudioActivity extends Activity implements IWebRTCListener, IDataChannelObserver {

    /**
     * Mode can Publish, Play or P2P
     */
    private String webRTCMode = IWebRTCClient.MODE_PUBLISH;

    private boolean enableDataChannel = true;


    private WebRTCClient webRTCClient;

    private Button startStreamingButton;
    private String operationName = "";
    private String serverUrl;

    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;
    private Spinner streamInfoListSpinner;
    public static final String WEBRTC_MODE = "WebRTC_MODE";

    public CountingIdlingResource idlingResource = new CountingIdlingResource("Load", true);

    // variables for handling reconnection attempts after disconnected
    final int RECONNECTION_PERIOD_MLS = 1000;

    final int RECONNECTION_CONTROL_PERIOD_MLS = 10000;

    private boolean stoppedStream = false;
    Handler reconnectionHandler = new Handler();
    private TextView broadcastingView;
    private EditText streamIdEditText;
    private boolean audioPushingEnabled = false;
    private String TAG = CustomAudioActivity.class.getSimpleName();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        //getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_main);

        cameraViewRenderer = findViewById(R.id.camera_view_renderer);
        pipViewRenderer = findViewById(R.id.pip_view_renderer);

        broadcastingView = findViewById(R.id.broadcasting_text_view);

        streamIdEditText = findViewById(R.id.stream_id_edittext);
        streamIdEditText.setText("streamId" + (int)(Math.random()*9999));

        startStreamingButton = findViewById(R.id.start_streaming_button);

        streamInfoListSpinner = findViewById(R.id.stream_info_list);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        serverUrl = sharedPreferences.getString(getString(R.string.serverAddress), SettingsActivity.DEFAULT_WEBSOCKET_URL);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String mode = this.getIntent().getStringExtra(WEBRTC_MODE);
        if (mode != null) {
            webRTCMode = mode;
        }

        startStreamingButton.setText("Start Publishing");
        operationName = "Publishing";

        this.getIntent().putExtra(EXTRA_VIDEO_FPS, 30);
        this.getIntent().putExtra(EXTRA_VIDEO_BITRATE, 1500);
        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, enableDataChannel);

        webRTCClient = new WebRTCClient( this,this);

        //webRTCClient.setOpenFrontCamera(false);

        String tokenId = "tokenId";
        webRTCClient.setVideoRenderers(pipViewRenderer, cameraViewRenderer);

        webRTCClient.setInputSampleRate(48000);
        webRTCClient.setStereoInput(false);
        //default AudioFormat.ENCODING_PCM_16BIT
        webRTCClient.setAudioInputFormat(CustomWebRtcAudioRecord.DEFAULT_AUDIO_FORMAT);
        webRTCClient.setCustomAudioFeed(true);

       // this.getIntent().putExtra(CallActivity.EXTRA_VIDEO_FPS, 24);
        webRTCClient.init(serverUrl, streamIdEditText.getText().toString(), webRTCMode, tokenId, this.getIntent());
        webRTCClient.setDataChannelObserver(this);


    }

    public void startStreaming(View v) {
        //update stream id if it is changed
        webRTCClient.setStreamId(streamIdEditText.getText().toString());
        idlingResource.increment();
        if (!webRTCClient.isStreaming()) {
            ((Button) v).setText("Stop " + operationName);
            Log.i(getClass().getSimpleName(), "Calling startStream");

            webRTCClient.startStream();
            stoppedStream = false;

            new Thread() {
                @Override
                public void run() {
                    CustomWebRtcAudioRecord audioInput = webRTCClient.getAudioInput();
                    while (audioInput.isStarted() == false) {
                        //It means that it's not initialized
                        try {
                            Thread.sleep(10);
                            Log.i("Audio", "Audio input is not initialized");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    CustomAudioActivity.this.audioPushingEnabled = true;
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                    Log.i("Audio ", "Audio input is started");
                    while (true) {
                        pushAudio();
                    }
                }
            }.start();

        }
        else {
            ((Button)v).setText("Start " + operationName);
            Log.i(getClass().getSimpleName(), "Calling stopStream");
            webRTCClient.stopStream();
            stoppedStream = true;
        }

    }

    public void pushAudio2() {
        CustomWebRtcAudioRecord audioInput = webRTCClient.getAudioInput();


        //WebRTC stack receives 10ms of audio data
        //By default it receives 48Khz, single channel(mono) and PCM data  AudioFormat.ENCODING_PCM_16BIT
        //It means 480 sample per ms * 10ms = 480 sample
        //It's PCM_16BIT, it's 2 byte. 480 * 2 byte = 960 byte
        //It's mono then 960 byte * 1 channel = 960 bytes

        /*
        lengths of formats
          AudioFormat.ENCODING_PCM_8BIT: 1

          AudioFormat.ENCODING_PCM_16BIT: 2 - s16le is this one
          AudioFormat.ENCODING_IEC61937: 2
          AudioFormat.ENCODING_DEFAULT: 2

          AudioFormat.ENCODING_PCM_FLOAT: 4
         */

        //We set these values above like this
        /*
         webRTCClient.setInputSampleRate(48000);
         webRTCClient.setStereoInput(false);
        //default AudioFormat.ENCODING_PCM_16BIT
          webRTCClient.setAudioInputFormat(WebRtcAudioRecord.DEFAULT_AUDIO_FORMAT);
         */

        //decoded_audio is 48Khz, 1 channel and s16le(ENCODING_PCM_16BIT) format

        int bufferLength = audioInput.getBufferByteLength(); // this is the length of 10ms data
        InputStream inputStream = getResources().openRawResource(R.raw.decoded_audio_long);

        byte[] data = new byte[bufferLength];
        int length;
        try {
            while ((length = inputStream.read(data, 0, data.length)) > 0) {
                audioInput.pushAudio(data, length);
                Log.i("Audio", "push audio: " + data[0] + " : " + data[1] + " : " + data[2] + " : " + data[3] + " : " );
                //emulate real time streaming by waiting 10ms because we're reading from the file directly
                //When you decode the audio from incoming RTSP stream, you don't need to sleep, just send it immediately when you get

                Thread.sleep(10);

                if (!this.audioPushingEnabled) {
                    break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void pushAudio() {
            try {
                CustomWebRtcAudioRecord audioInput = webRTCClient.getAudioInput();

                int bufferLength = audioInput.getBufferByteLength(); // this is the length of 10ms data

                ByteBuffer rawAudioBuffer = ByteBuffer.allocate(bufferLength*20);

                final String uriPath="android.resource://"+getPackageName()+"/raw/"+R.raw.sample4;

                final Uri uri= Uri.parse(uriPath);
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(this, uri, null);

                // Find and select the MP3 track
                MediaFormat format = null;
                int trackCount = extractor.getTrackCount();
                for (int i = 0; i < trackCount; i++) {
                    format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("audio/")) {
                        extractor.selectTrack(i);
                        break;
                    }
                }

                if (format == null) {
                    Log.e(TAG, "No audio track found in MP3 file");
                    return;
                }

                // Create a MediaCodec to decode the MP3 file
                MediaCodec codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
                codec.configure(format, null, null, 0);
                codec.start();

                ByteBuffer[] inputBuffers = codec.getInputBuffers();
                ByteBuffer[] outputBuffers = codec.getOutputBuffers();

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                boolean isEOS = false;
                long presentationTimeUs = 0;


                // Decode the MP3 file to PCM
                while (!isEOS) {
                    int inputBufferIndex = codec.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }

                    int outputBufferIndex = codec.dequeueOutputBuffer(info, 10000);
                    if (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        rawAudioBuffer.put(outputBuffer);
                        int length = rawAudioBuffer.position();

                        rawAudioBuffer.position(0);
                        Log.d(TAG, "pushAudio: length: " + length + " bufferLength: " + bufferLength);
                        while(length - rawAudioBuffer.position() >= bufferLength) {
                            byte[] pcmData = new byte[bufferLength];
                            rawAudioBuffer.get(pcmData);

                            Log.d(TAG, "length: " + length+ " position: " + rawAudioBuffer.position());


                            audioInput.pushAudio(pcmData, pcmData.length);
                            Log.i("Audio", "push audio: " + pcmData[0] + " : " + pcmData[1] + " : " + pcmData[2] + " : " + pcmData[3] + " : ");
                            //emulate real time streaming by waiting 10ms because we're reading from the file directly
                            //When you decode the audio from incoming RTSP stream, you don't need to sleep, just send it immediately when you get
                            Thread.sleep(10);
                        }

                        byte[] moreData = new byte[length - rawAudioBuffer.position()];
                        rawAudioBuffer.get(moreData);
                        rawAudioBuffer.clear();
                        rawAudioBuffer.put(moreData);



                        codec.releaseOutputBuffer(outputBufferIndex, false);
                    } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        format = codec.getOutputFormat();
                    }
                }

                // Release resources
                codec.stop();
                codec.release();
                extractor.release();
            } catch (IOException e) {
                Log.e(TAG, "Error decoding MP3 to PCM: " + e.getMessage());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
    }

    @Override
    public void onPlayStarted(String streamId) {
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.VISIBLE);
        decrementIdle();
    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_SHORT).show();
        broadcastingView.setVisibility(View.GONE);
        decrementIdle();

    }

    @Override
    public void onPlayFinished(String streamId) {
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
    }

    @Override
    public void streamIdInUse(String streamId) {
        Log.w(getClass().getSimpleName(), "streamIdInUse");
        Toast.makeText(this, "Stream id is already in use.", Toast.LENGTH_LONG).show();
        decrementIdle();
    }

    @Override
    public void onError(String description, String streamId) {
        Log.w(getClass().getSimpleName(), "onError:" + description);
        Toast.makeText(this, "Error: "  +description , Toast.LENGTH_LONG).show();
        decrementIdle();
    }

    private void decrementIdle() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (webRTCClient != null) {
            Log.i(getClass().getSimpleName(), "onStop and calling stopStream");
            webRTCClient.stopStream();
        }
        stoppedStream = true;
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code, String streamId) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDisconnected(String streamId) {
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
        startStreamingButton.setText("Stop " + operationName);
    }

    @Override
    public void onIceDisconnected(String streamId) {
        //it's called when ice is disconnected
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

    @Override
    public void onNewVideoTrack(VideoTrack track) {

    }

    @Override
    public void onVideoTrackEnded(VideoTrack track) {

    }

    @Override
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {
        Log.d(CustomAudioActivity.class.getName(), "Data channel buffered amount changed: ");
    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {
        Log.d(CustomAudioActivity.class.getName(), "Data channel state changed: ");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, String dataChannelLabel) {
        ByteBuffer data = buffer.data;
        String messageText = new String(data.array(), StandardCharsets.UTF_8);
        Toast.makeText(this, "New Message: " + messageText, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if (successful) {
            ByteBuffer data = buffer.data;
            final byte[] bytes = new byte[data.capacity()];
            data.get(bytes);
            String messageText = new String(bytes, StandardCharsets.UTF_8);

            Toast.makeText(this, "Message is sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not send the text message", Toast.LENGTH_LONG).show();
        }
    }

    public void sendTextMessage(String messageToSend) {
        final ByteBuffer buffer = ByteBuffer.wrap(messageToSend.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf = new DataChannel.Buffer(buffer, false);
        webRTCClient.sendMessageViaDataChannel(buf);
    }

    public void showSendDataChannelMessageDialog(View view) {
        if (webRTCClient != null && webRTCClient.isDataChannelEnabled()) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Send Message via Data Channel");
            // set the custom layout
            final View customLayout = getLayoutInflater().inflate(R.layout.send_message_data_channel, null);
            builder.setView(customLayout);
            // add a button
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // send data from the AlertDialog to the Activity
                    EditText editText = customLayout.findViewById(R.id.message_text_input);
                    sendTextMessage(editText.getText().toString());
                   // sendDialogDataToActivity(editText.getText().toString());
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else {
            Toast.makeText(this, R.string.data_channel_not_available, Toast.LENGTH_LONG).show();
        }
    }

    public IdlingResource getIdlingResource() {
        return idlingResource;
    }
}
