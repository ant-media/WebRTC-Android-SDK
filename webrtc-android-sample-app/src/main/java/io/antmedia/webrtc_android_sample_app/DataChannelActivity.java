package io.antmedia.webrtc_android_sample_app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.webrtc.DataChannel;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import de.tavendo.autobahn.WebSocket;
import io.antmedia.webrtc_android_sample_app.chat.ImageMessage;
import io.antmedia.webrtc_android_sample_app.chat.Message;
import io.antmedia.webrtc_android_sample_app.chat.MessageAdapter;
import io.antmedia.webrtc_android_sample_app.chat.SettingsActivity;
import io.antmedia.webrtc_android_sample_app.chat.TextMessage;
import io.antmedia.webrtcandroidframework.IDataChannelObserver;
import io.antmedia.webrtcandroidframework.IWebRTCClient;
import io.antmedia.webrtcandroidframework.IWebRTCListener;
import io.antmedia.webrtcandroidframework.StreamInfo;
import io.antmedia.webrtcandroidframework.WebRTCClient;
import io.antmedia.webrtcandroidframework.apprtc.CallActivity;

import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED;
import static io.antmedia.webrtcandroidframework.apprtc.CallActivity.EXTRA_DATA_CHANNEL_ENABLED;

/********************
 * Take a look at the blog posts
 * https://antmedia.io/webrtc-chat-and-file-transfer-2/
 * https://antmedia.io/webrtc-chat-and-file-transfer/
 ********************/
public class DataChannelActivity extends Activity implements IWebRTCListener, IDataChannelObserver, TextView.OnEditorActionListener {


    private WebRTCClient webRTCClient;
    private String webRTCMode;
    private Button startStreamingButton;
    private String operationName = "";
    private EditText messageInput;
    private MessageAdapter messageAdapter;
    private ListView messagesView;
    private Button settingsButton;
    private Button sendImageButton;
    private SurfaceViewRenderer cameraViewRenderer;
    private SurfaceViewRenderer pipViewRenderer;
    private static int REQUEST_GET_IMAGE = 1;
    private BinaryDataSender imageSender = new BinaryDataSender();
    private BinaryDataReceiver imageReceiver = new BinaryDataReceiver();
    private String uniqueID = UUID.randomUUID().toString();
    private int lastSentMessageNum = 0;

    private String streamId;
    private String tokenId;
    private String serverURL;

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

        setContentView(R.layout.activity_data);

        messageInput = findViewById(R.id.message_text_input);
        messageInput.setOnEditorActionListener(this);
        messageInput.setEnabled(false);

        sendImageButton = findViewById(R.id.send_image_button);
        sendImageButton.setEnabled(false);

        messageAdapter = new MessageAdapter(this);
        messagesView = findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);

        webRTCClient = new WebRTCClient( this,this);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        String serverAddress = sharedPreferences.getString(getString(R.string.serverAddress), "192.168.1.23");
        String serverPort = sharedPreferences.getString(getString(R.string.serverPort), "5080");

        streamId = sharedPreferences.getString(getString(R.string.streamId), "stream1");
        tokenId = "tokenId";
        serverURL = "ws://" + serverAddress + ":" + serverPort + "/WebRTCAppEE/websocket";

        webRTCMode = sharedPreferences.getString(getString(R.string.stream_mode), IWebRTCClient.MODE_JOIN);

        cameraViewRenderer = findViewById(R.id.camera_view_renderer);

        pipViewRenderer = findViewById(R.id.pip_view_renderer);

        startStreamingButton = findViewById(R.id.start_streaming_button);
        settingsButton = findViewById(R.id.settings);
        cameraViewRenderer.setZOrderOnTop(true);
        webRTCClient.setVideoRenderers(cameraViewRenderer, pipViewRenderer);

        // Check for mandatory permissions.
        for (String permission : CallActivity.MANDATORY_PERMISSIONS) {
            if (this.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission " + permission + " is not granted", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        this.getIntent().putExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, true);
        this.getIntent().putExtra(EXTRA_DATA_CHANNEL_ENABLED, true);


        if (webRTCMode.equals(IWebRTCClient.MODE_PUBLISH)) {
            startStreamingButton.setText("Start Publishing");
            operationName = "Publishing";
        }
        else  if (webRTCMode.equals(IWebRTCClient.MODE_PLAY)) {
            startStreamingButton.setText("Start Playing");
            operationName = "Playing";
        }
        else if (webRTCMode.equals(IWebRTCClient.MODE_JOIN)) {
            startStreamingButton.setText("Start P2P");
            operationName = "P2P";
        }
        webRTCClient.setDataChannelObserver(this);

    }

    private void initStream() {
        cameraViewRenderer = findViewById(R.id.camera_view_renderer);
        pipViewRenderer = findViewById(R.id.pip_view_renderer);
        webRTCClient.setVideoRenderers(cameraViewRenderer, pipViewRenderer);
        webRTCClient.init(serverURL, streamId, webRTCMode, tokenId, this.getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();

        initStream();
    }


    public void startStreaming(View v) {

        if (!webRTCClient.isStreaming()) {
            ((Button)v).setText("Stop " + operationName);
            webRTCClient.startStream();
        }
        else {
            ((Button)v).setText("Start " + operationName);
            webRTCClient.stopStream();
        }
    }

    public void sendTextMessage() {
        String messageToSend = messageInput.getText().toString();

        String messageToSendJson = Message.createJsonTextMessage(computeMessageId(), new Date(), messageToSend);

        final ByteBuffer buffer = ByteBuffer.wrap(messageToSendJson.getBytes(StandardCharsets.UTF_8));
        DataChannel.Buffer buf= new DataChannel.Buffer(buffer,false);
        webRTCClient.sendMessageViaDataChannel(buf);
    }


    private String computeMessageId() {
        return uniqueID+lastSentMessageNum;
    }

    private void increaseMessageId() {
        lastSentMessageNum++;
    }

    public void sendImage(View view) {
        if(messageInput.isEnabled()) {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, REQUEST_GET_IMAGE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webRTCClient.setDataChannelObserver(null);
        webRTCClient = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GET_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            File imageFile = new File(picturePath);
            int size = (int) imageFile.length();
            byte[] imageBytes = new byte[size];
            try {
                FileInputStream inputStream = new FileInputStream(imageFile);
                inputStream.read(imageBytes, 0, imageBytes.length);
                inputStream.close();
                String imageHeaderInJson = Message.createJsonMessage(computeMessageId(), new Date());
                imageSender.startSending(imageBytes, imageHeaderInJson);
            } catch (FileNotFoundException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @Override
    public void onPlayStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayStarted");
        Toast.makeText(this, "Play started", Toast.LENGTH_LONG).show();
        webRTCClient.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        messageInput.setEnabled(true);
        sendImageButton.setEnabled(true);
    }

    @Override
    public void onPublishStarted(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishStarted");
        Toast.makeText(this, "Publish started", Toast.LENGTH_LONG).show();


    }

    @Override
    public void onPublishFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPublishFinished");
        Toast.makeText(this, "Publish finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPlayFinished(String streamId) {
        Log.w(getClass().getSimpleName(), "onPlayFinished");
        Toast.makeText(this, "Play finished", Toast.LENGTH_LONG).show();
    }

    @Override
    public void noStreamExistsToPlay(String streamId) {
        Log.w(getClass().getSimpleName(), "noStreamExistsToPlay");
        Toast.makeText(this, "No stream exist to play", Toast.LENGTH_LONG).show();
    }

    @Override
    public void streamIdInUse(String streamId) {
        Log.w(getClass().getSimpleName(), "streamIdInUse");
        Toast.makeText(this, "Stream id is already in use.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onError(String description, String streamId) {
        Toast.makeText(this, "Error: "  +description , Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //webRTCClient.stopStream();
        //messageInput.setEnabled(false);
        //settingsButton.setEnabled(true);
        imageReceiver.clear();
        imageSender.clear();
    }

    @Override
    public void onSignalChannelClosed(WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification code,String streamId) {
        Toast.makeText(this, "Signal channel closed with code " + code, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onIceDisconnected(String streamId) {
    }

    @Override
    public void onDisconnected(String streamId) {
        sendImageButton.setEnabled(false);
        messageInput.setEnabled(false);
        settingsButton.setEnabled(true);
        Log.w(getClass().getSimpleName(), "disconnected");
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
        startStreamingButton.setText("Start " + operationName);
        //finish();
        webRTCClient.stopStream();
        imageReceiver.clear();
        imageSender.clear();
        initStream();
    }

    @Override
    public void onIceConnected(String streamId) {
        //it is called when connected to ice
        messageInput.setEnabled(true);
        sendImageButton.setEnabled(true);
        settingsButton.setEnabled(false);
    }


    public void onOffVideo(View view) {
        if (webRTCClient.isVideoOn()) {
            webRTCClient.disableVideo();
        }
        else {
            webRTCClient.enableVideo();
        }
    }

    public void onOffAudio(View view) {
        if (webRTCClient.isAudioOn()) {
            webRTCClient.disableAudio();
        }
        else {
            webRTCClient.enableAudio();
        }
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
    public void onBufferedAmountChange(long previousAmount, String dataChannelLabel) {
        Log.d(DataChannelActivity.class.getName(), "Data channel buffered amount changed: " );
    }

    @Override
    public void onStateChange(DataChannel.State state, String dataChannelLabel) {
        Log.d(DataChannelActivity.class.getName(), "Data channel state changed: " );
    }

    @Override
    public void onMessage(final DataChannel.Buffer buffer, String dataChannelLabel) {
        if (buffer.binary) {
            Log.d(DataChannelActivity.class.getName(), "Received binary msg over " );

            imageReceiver.receiveDataChunk(buffer.data);

            if(imageReceiver.isAllDataReceived()) {
                Bitmap bmp=BitmapFactory.decodeByteArray(imageReceiver.receivedData.array(),0,imageReceiver.receivedData.capacity());
                final ImageMessage message = new ImageMessage();
                message.parseJson(imageReceiver.header.text);
                message.setImageBitmap(bmp);

                messageAdapter.add(message);
                // scroll the ListView to the last added element
                messagesView.setSelection(messagesView.getCount() - 1);

                imageReceiver.clear();
            }

        } else {
            ByteBuffer data = buffer.data;
            String strDataJson = new String(data.array(), StandardCharsets.UTF_8);

            final Message message = new TextMessage();
            message.parseJson(strDataJson);

            messageAdapter.add(message);
            // scroll the ListView to the last added element
            messagesView.setSelection(messagesView.getCount() - 1);
        }
    }

    @Override
    public void onMessageSent(DataChannel.Buffer buffer, boolean successful) {
        if(successful) {
            increaseMessageId();
            if (!buffer.binary) {
                ByteBuffer data = buffer.data;
                final byte[] bytes = new byte[data.capacity()];
                data.get(bytes);
                String strDataJson = new String(bytes, StandardCharsets.UTF_8);

                final Message message = new TextMessage();
                message.parseJson(strDataJson);
                message.setBelongsToCurrentUser(true);

                messageAdapter.add(message);
                // scroll the ListView to the last added element
                messagesView.setSelection(messagesView.getCount() - 1);
            } else {
                imageSender.updateLastChunkSent();
                if(!imageSender.isDataSendingComplete()) {
                    imageSender.sendLastChunk();
                } else {
                    Bitmap bmp=BitmapFactory.decodeByteArray(imageSender.dataBytes,0,imageSender.dataBytes.length);

                    final ImageMessage message = new ImageMessage();
                    message.setBelongsToCurrentUser(true);
                    message.parseJson(imageSender.header.text);
                    message.setImageBitmap(bmp);

                    messageAdapter.add(message);
                    // scroll the ListView to the last added element
                    messagesView.setSelection(messagesView.getCount() - 1);

                    imageSender.clear();
                }
            }
        } else {
            if(!buffer.binary) {
                Toast.makeText(this, "Could not send the text message", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Could not send the image", Toast.LENGTH_LONG).show();
                imageSender.clear();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendTextMessage();
            handled = true;
            messageInput.setText("");
        }
        return handled;
    }

    public void sendMessage(View view) {
        if (messageInput.isEnabled()) {
            sendTextMessage();
            messageInput.setText("");
        }
    }

    public void goToSettings(View view) {
        Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private class BinaryMessageHeader {
        int length;
        String text;
    }

    private class BinaryDataSender {

        boolean sending = false;
        int endChunk = 0;
        int lastOffset = 0;
        byte[] dataBytes = null;
        BinaryMessageHeader header;

        final static int CHUNK_SIZE_IN_BYTES = 10000;

        boolean isDataSendingComplete() {
            return lastOffset >= dataBytes.length;
        }

        void updateLastChunkSent() {
            lastOffset = endChunk;
        }

        void clear() {
            sending= false;
            lastOffset = 0;
            endChunk = 0;
            dataBytes = null;
            header = null;
        }

        void startSending(final byte[] imageBytes, String messageHeader ) {
            sending = true;
            lastOffset = 0;
            this.dataBytes = imageBytes;

            sendFirstChunk(messageHeader);
        }

        void sendFirstChunk(String messageHeader) {
            header = new BinaryMessageHeader();
            header.text = messageHeader;

            byte[]  messageHeaderBytes = messageHeader.getBytes(Charset.defaultCharset());
            header.length  = messageHeaderBytes.length;
            int totalMessageHeaderLength = header.length +8;

            int remainingChunkSize = CHUNK_SIZE_IN_BYTES-(totalMessageHeaderLength);
            endChunk = (lastOffset+remainingChunkSize)> dataBytes.length? dataBytes.length: (lastOffset+remainingChunkSize);

            // put length how much data will be sent in total
            ByteBuffer imageChunkData = ByteBuffer.allocate(totalMessageHeaderLength+endChunk);
            imageChunkData.putInt(dataBytes.length);
            imageChunkData.putInt(header.length);
            imageChunkData.put(messageHeaderBytes);

            byte[] chunkBytes = Arrays.copyOfRange(dataBytes,lastOffset, endChunk);
            imageChunkData.put(chunkBytes);
            imageChunkData.rewind();

            DataChannel.Buffer buf = new DataChannel.Buffer(imageChunkData, true);

            webRTCClient.sendMessageViaDataChannel(buf);
        }

        void sendLastChunk() {

            endChunk = (lastOffset+CHUNK_SIZE_IN_BYTES)> dataBytes.length? dataBytes.length: (lastOffset+CHUNK_SIZE_IN_BYTES);
            byte[] chunkBytes = Arrays.copyOfRange(dataBytes,lastOffset, endChunk);

            ByteBuffer imageChunkData = ByteBuffer.wrap(chunkBytes);
            DataChannel.Buffer buf = new DataChannel.Buffer(imageChunkData, true);

            webRTCClient.sendMessageViaDataChannel(buf);

        }
    }

    private class BinaryDataReceiver {
        ByteBuffer receivedData = null;
        int toBeReceivedBinaryDataLength = 0;
        BinaryMessageHeader header = null;

        private void clear() {
            receivedData = null;
            toBeReceivedBinaryDataLength = 0;
            header = null;
        }

        private boolean isFirstPart() {
            return receivedData == null;
        }

        private boolean isAllDataReceived() {
            return receivedData.capacity()>= toBeReceivedBinaryDataLength;
        }

        private void receiveDataChunk(ByteBuffer dataChunk) {
            if(!isFirstPart()) {
                // append two buffers if not first chunk
                receivedData = ByteBuffer.allocate(receivedData.capacity()+dataChunk.capacity()).put(receivedData);
                receivedData.put(dataChunk);
                receivedData.rewind();
            } else {
                header = new BinaryMessageHeader();
                dataChunk.order(ByteOrder.LITTLE_ENDIAN);
                toBeReceivedBinaryDataLength = dataChunk.getInt();
                header.length = dataChunk.getInt();
                int totalHeaderLength = header.length+8;

                byte[] headerMessageByte = new byte[header.length];
                dataChunk.get(headerMessageByte);
                header.text = new String(headerMessageByte, StandardCharsets.UTF_8);

                Log.w(getClass().getSimpleName(), "Total Data to receive "+ toBeReceivedBinaryDataLength);
                receivedData = ByteBuffer.allocate(dataChunk.capacity()-totalHeaderLength);
                receivedData.put(dataChunk);
                receivedData.rewind();
            }
        }
    }
}


