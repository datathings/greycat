package greycat.mqtt;

import greycat.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.*;

import java.io.IOException;

import static greycat.Tasks.newTask;

/**
 * Created by Cyril Cecchinel - I3S Laboratory on 02/08/2017.
 */
public class MQTTTests {
    private static Graph graph;
    private static String topic = "demo" + System.currentTimeMillis();
    private static String HOST_IP = "iot.eclipse.org";
    private static int HOST_PORT = 1883;
    @BeforeClass
    public static void initGraph() throws IOException {
        graph = GraphBuilder
                .newBuilder()
                .withPlugin(new MQTTPlugin(HOST_IP, HOST_PORT, new String[]{topic}, "theIndex"))
                .build();

        graph.connect(isConnected -> {
            Node node = graph.newNode(0, 0);
            node.set("id", Type.INT, 12);
            node.set("value", Type.DOUBLE, 12.0d);

            graph.declareIndex(0, "theIndex", index -> {
                index.update(node);
            }, "id");

        });
    }


    @Test
    public void testUpdateFromMQTT(){
        MqttClient client = null;
        final int[] counter = {0}; // Verify that the test reaches it expected end
        try {
            client = new MqttClient("tcp://" + HOST_IP + ":" + HOST_PORT, MqttClient.generateClientId());
            client.connect();
            if (client.isConnected()){
                String content = "{\"id\":\"12\"," +
                        "\"time\":123," +
                        "\"value\":\"45.6\"}";
                MqttMessage message = new MqttMessage(content.getBytes());
                client.publish(topic, message);
                client.disconnect();
                Thread.sleep(1000); // Just wait for the message to reach the broker
                newTask().travelInTime("123").readIndex("theIndex", "12")
                        .attribute("value")
                        .thenDo(ctx -> counter[0]++)
                        .thenDo(ctx-> Assert.assertEquals(45.6, ctx.doubleResult(), 0.1))
                        .thenDo(ctx -> Assert.assertEquals(1, counter[0]))
                        .execute(graph, null);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    @AfterClass
    public static void closeExternalResources(){
        graph.disconnect(null);
    }



}
