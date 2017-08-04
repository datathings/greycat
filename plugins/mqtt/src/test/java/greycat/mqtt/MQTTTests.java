/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greycat.mqtt;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.Type;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static greycat.Tasks.newTask;

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

            Node node2 = graph.newNode(0, 0);
            node2.set("id", Type.INT, 78);
            node2.set("value", Type.DOUBLE, 1d);

            graph.declareIndex(0, "theIndex", index -> {
                index.update(node);
                index.update(node2);
            }, "id");

        });
    }

    @Test
    public void testNewAttributeFromMQTT(){
        MqttClient client = null;
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
            client = new MqttClient("tcp://" + HOST_IP + ":" + HOST_PORT, MqttClient.generateClientId());
            client.connect();
            if (client.isConnected()){
                String content = "{\"id\":\"12\",\n" +
                        "\"time\":123,\n" +
                        "\"values\":{\"attr1\":{\"value\":\"42\",\"type\":\"DOUBLE\"},\n" +
                        "\"attr2\":{\"value\":\"hello\",\"type\":\"STRING\"}}}";
                MqttMessage message = new MqttMessage(content.getBytes());
                client.publish(topic, message);
                client.disconnect();
                Thread.sleep(1000); // Just wait for the message to reach the broker
                newTask().travelInTime("123").readIndex("theIndex", "12")
                        .pipe(
                                newTask().attribute("attr1")
                                        .thenDo(ctx-> {
                                            Assert.assertEquals(42.0, ctx.doubleResult(), 0.1);
                                            ctx.continueTask();
                                        })
                                        .thenDo(ctx -> {
                                            counter[0]++;
                                            ctx.continueTask();
                                        }),
                                newTask().attribute("attr2")
                                        .thenDo(ctx -> {
                                            Assert.assertEquals("hello", ctx.resultAsStrings().get(0));
                                            ctx.continueTask();
                                        })
                                        .thenDo(ctx -> {
                                            counter[0]++;
                                            ctx.continueTask();
                                        })
                        ).thenDo(ctx -> Assert.assertEquals(2, counter[0]))
                        .execute(graph, null);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateAttributeFromMQTT(){
        MqttClient client = null;
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
            client = new MqttClient("tcp://" + HOST_IP + ":" + HOST_PORT, MqttClient.generateClientId());
            client.connect();
            if (client.isConnected()){
                String content = "{\"id\":\"78\",\n" +
                        "\"time\":123,\n" +
                        "\"values\":{\"value\":{\"value\":\"12\",\"type\":\"DOUBLE\"}}}";
                MqttMessage message = new MqttMessage(content.getBytes());
                client.publish(topic, message);
                client.disconnect();
                Thread.sleep(1000); // Just wait for the message to reach the broker
                newTask().travelInTime("123").readIndex("theIndex", "78")
                                     .attribute("value")
                                        .thenDo(ctx-> {
                                            Assert.assertEquals(12.0, ctx.doubleResult(), 0.1);
                                            ctx.continueTask();
                                        })
                                        .thenDo(ctx -> {
                                            counter[0]++;
                                            ctx.continueTask();
                                        })
                        .thenDo(ctx -> Assert.assertEquals(1, counter[0]))
                        .execute(graph, null);
            }

        } catch (MqttException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBadNodeFromMQTT(){
        MqttClient client = null;
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
            client = new MqttClient("tcp://" + HOST_IP + ":" + HOST_PORT, MqttClient.generateClientId());
            client.connect();
            if (client.isConnected()){
                String content = "{\"id\":\"98\",\n" +
                        "\"time\":123,\n" +
                        "\"values\":{\"value\":{\"value\":\"12\",\"type\":\"DOUBLE\"}}}";
                MqttMessage message = new MqttMessage(content.getBytes());
                client.publish(topic, message);
                client.disconnect();
                Thread.sleep(1000); // Just wait for the message to reach the broker
                newTask().travelInTime("123").readIndex("theIndex", "98").thenDo(ctx ->
                        Assert.assertNull(ctx.result().get(0))).execute(graph, null);
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
