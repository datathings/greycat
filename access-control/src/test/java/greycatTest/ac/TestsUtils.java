package greycatTest.ac;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.websocket.SecWSClient;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gregory NAIN on 07/08/2017.
 */
public class TestsUtils {

    public static void authenticateAndConnect(String login, String password,Callback<Graph> callback) {
        getAuthToken(login, password, token -> connectGraph(token, callback));
    }

    public static void connectGraph(String token, Callback<Graph> callback) {

        Graph graph = GraphBuilder.newBuilder()
                .withStorage(new SecWSClient("ws://localhost:7071/ws", token.split("#")[0])).build();
        graph.connect(connected -> {
            callback.on(graph);
        });

    }

    public static void getAuthToken(String login, String password, Callback<String> callback) {
        try {
            URL url = new URL("http://localhost:7071/auth");

            Map<String, Object> params = new HashMap<>();
            params.put("login", login);
            params.put("pass", password);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            String urlParameters = postData.toString();
            URLConnection conn = url.openConnection();

            conn.setDoOutput(true);

            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(urlParameters);
            writer.flush();

            String result = "";
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            while ((line = reader.readLine()) != null) {
                result += line;
            }
            writer.close();
            reader.close();
            callback.on(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
