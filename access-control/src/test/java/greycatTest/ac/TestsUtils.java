package greycatTest.ac;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.websocket.SecWSClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gregory NAIN on 07/08/2017.
 */
public class TestsUtils {

    public static void authenticateAndConnect(String login, String password, Callback<Graph> callback) {
        getAuthToken(login, password, token -> connectGraph(token, callback));
    }

    public static void authenticateAndConnect(String login, String password, String pin, Callback<Graph> callback) {
        getAuthToken(login, password, pin, token -> connectGraph(token, callback));
    }

    public static void connectGraph(String token, Callback<Graph> callback) {
        String[] params = token.split("#");
        if(params.length < 2) {
            callback.on(null);
            return;
        }
        Graph graph = GraphBuilder.newBuilder()
                .withStorage(new SecWSClient("ws://localhost:7071/ws", params[1])).build();
        graph.connect(connected -> {
            callback.on(graph);
        });

    }

    public static void getAuthToken(String login, String password, Callback<String> callback) {
        getAuthToken(login, password, null, callback);
    }

    public static void getAuthToken(String login, String password, String twoFactor, Callback<String> callback) {
        try {
            URL url = new URL("http://localhost:7071/auth");

            Map<String, Object> params = new HashMap<>();
            params.put("login", login);
            params.put("pass", password);
            if (twoFactor != null) {
                params.put("otp", twoFactor);
            }

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            String urlParameters = postData.toString();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(urlParameters);
            writer.flush();

            String result = conn.getResponseCode() + "#";
            String line;
            if (conn.getResponseCode() < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    result += line;
                }
                writer.close();
                reader.close();
                callback.on(result);
            } else {
                if (conn.getErrorStream() != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        result += line;
                    }
                    writer.close();
                    reader.close();
                }
                callback.on(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
