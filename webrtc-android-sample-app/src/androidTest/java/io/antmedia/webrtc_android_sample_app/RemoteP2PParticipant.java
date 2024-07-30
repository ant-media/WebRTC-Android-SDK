package io.antmedia.webrtc_android_sample_app;

import static org.junit.Assert.assertNotNull;

import android.util.Log;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class RemoteP2PParticipant {

    NetworkClient client = new NetworkClient();
    String response = null;

    private String streamName;
    private String runningTest;

    public static final String CREATE_ROUTE = "createP2P";
    public static final String JOIN_ROUTE = "joinP2P";
    public static final String LEAVE_ROUTE = "leaveP2P";

    public RemoteP2PParticipant(String streamName, String runningTest) {
        this.streamName = streamName;
        this.runningTest = runningTest;
    }

    public class NetworkClient {

        private static final String REST_IP = "10.0.2.2";
        private static final int REST_PORT = 3030;

        private final OkHttpClient client = new OkHttpClient();

        public String get(String path) throws IOException {
            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("http")
                    .host(REST_IP)
                    .port(REST_PORT)
                    .addPathSegment(path)
                    .addQueryParameter("streamName", streamName)
                    .addQueryParameter("test", runningTest)
                    .build();


            Request request = new Request.Builder()
                    .url(httpUrl)
                    .header("Connection", "close") // <== solution, not declare in Interceptor
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            return response.body().string();
        }
    }

    public void join() {
        try {
            response = client.get(CREATE_ROUTE);
            Log.i("RemoteParticipant", "create: " + response);
            assertNotNull(response);

            response = client.get(JOIN_ROUTE);
            Log.i("RemoteParticipant", "join: " + response);
            assertNotNull(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leave() {
        try {
            response = client.get(LEAVE_ROUTE);
            Log.i("RemoteParticipant", "leave: " + response);
            assertNotNull(response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RemoteP2PParticipant addP2PParticipant(String streamName, String runningTest) {
        RemoteP2PParticipant participant = new RemoteP2PParticipant(streamName, runningTest);
        participant.join();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return participant;
    }
}