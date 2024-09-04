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

class RemoteConferenceParticipant {

    NetworkClient client = new NetworkClient();
    String response = null;
    String participantName = "p_"+ RandomStringUtils.randomNumeric(3);

    private String roomName;
    private String runningTest;

    public static final String CREATE_ROUTE = "createConference";
    public static final String JOIN_ROUTE = "joinConference";
    public static final String LEAVE_ROUTE = "leaveConference";
    public static final String DELETE_ROUTE = "deleteConference";


    public RemoteConferenceParticipant(String roomName, String runningTest) {
        this.roomName = roomName;
        this.runningTest = runningTest;
    }

    public class NetworkClient {

        private static final String REST_IP = "10.0.2.2";
        private static final int REST_PORT = 3030;

        private final OkHttpClient client = new OkHttpClient();

        public String get(String path, String participantName) throws IOException {
            HttpUrl httpUrl = new HttpUrl.Builder()
                    .scheme("http")
                    .host(REST_IP)
                    .port(REST_PORT)
                    .addPathSegment(path)
                    .addQueryParameter("room", roomName)
                    .addQueryParameter("test", runningTest)
                    .addQueryParameter("participant", participantName)
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
            response = client.get(CREATE_ROUTE, participantName);
            Log.i("RemoteConferenceParticipant", "create: " + response);
            assertNotNull(response);

            response = client.get(JOIN_ROUTE, participantName);
            Log.i("RemoteConferenceParticipant", "join: " + response);
            assertNotNull(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void leave() {
        try {
            response = client.get(LEAVE_ROUTE, participantName);
            Log.i("RemoteConferenceParticipant", "leave: " + response);
            assertNotNull(response);

            response = client.get(DELETE_ROUTE, participantName);
            Log.i("RemoteConferenceParticipant", "delete: " + response);
            assertNotNull(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RemoteConferenceParticipant addConferenceParticipant(String roomName, String runningTest) {
        RemoteConferenceParticipant participant = new RemoteConferenceParticipant(roomName, runningTest);
        participant.join();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return participant;
    }
}