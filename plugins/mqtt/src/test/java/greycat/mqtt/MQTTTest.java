/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static greycat.Tasks.newTask;

public class MQTTTest {
    private static Graph graph1;
    private static Graph graph2;
    private static String topic = "demo" + System.currentTimeMillis();
    private static String HOST_IP = ""; //Not pertinent for testing
    private static int HOST_PORT = 1883;
    private static long NETWORK_DELAY = 1000;
    private static MQTTPlugin plugin = new MQTTPlugin(HOST_IP, HOST_PORT, new String[]{topic}, "theIndex").noConnection();
    private static MQTTPlugin plugin2 = new MQTTPlugin(HOST_IP, HOST_PORT, new String[]{"customTopic"}, "theIndex", new CustomHandler("sensors")).noConnection();

    @BeforeClass
    public static void initGraph() throws IOException {
        graph1 = GraphBuilder
                .newBuilder()
                .withPlugin(plugin)
                .build();

        graph1.connect(isConnected -> {
            Node node = graph1.newNode(0, 0);
            node.set("id", Type.INT, 12);
            node.set("value", Type.DOUBLE, 12.0d);

            Node node2 = graph1.newNode(0, 0);
            node2.set("id", Type.INT, 78);
            node2.set("value", Type.DOUBLE, 1d);

            graph1.declareIndex(0, "theIndex", index -> {
                index.update(node);
                index.update(node2);
            }, "id");

        });

        graph2 = GraphBuilder
                .newBuilder()
                .withPlugin(plugin2)
                .build();

        graph2.connect(isConnected -> {
            Node node = graph2.newNode(0, 0);
            node.set("id", Type.INT, 5);
            node.set("value", Type.DOUBLE, 0d);

            graph2.declareIndex(0, "sensors", index -> {
                index.update(node);
            }, "id");
        });
    }

    @AfterClass
    public static void closeResources() {
        graph1.disconnect(null);
        graph2.disconnect(null);
    }

    @Test
    public void testNewAttributeFromMQTT() {


        MqttClient client = null;
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
            String content = "{\"id\":\"12\",\n" +
                    "\"time\":123,\n" +
                    "\"values\":{\"attr1\":{\"value\":\"42\",\"type\":\"DOUBLE\"},\n" +
                    "\"attr2\":{\"value\":\"hello\",\"type\":\"STRING\"}}}";
            MqttMessage message = new MqttMessage(content.getBytes());
            plugin.getHandler().messageArrived(topic, message);

            newTask().travelInTime("123").readIndex("theIndex", "12")
                    .pipe(
                            newTask().attribute("attr1")
                                    .thenDo(ctx -> {
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
                    .execute(graph1, null);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateAttributeFromMQTT() {
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
                String content = "{\"id\":\"78\",\n" +
                        "\"time\":123,\n" +
                        "\"values\":{\"value\":{\"value\":\"12\",\"type\":\"DOUBLE\"}}}";
            MqttMessage message = new MqttMessage(content.getBytes());
            plugin.getHandler().messageArrived(topic, message);

                newTask().travelInTime("123").readIndex("theIndex", "78")
                        .attribute("value")
                        .thenDo(ctx -> {
                            Assert.assertEquals(12.0, ctx.doubleResult(), 0.1);
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> {
                            counter[0]++;
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> Assert.assertEquals(1, counter[0]))
                        .execute(graph1, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBadNodeFromMQTT() {
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
                String content = "{\"id\":\"98\",\n" +
                        "\"time\":123,\n" +
                        "\"values\":{\"value\":{\"value\":\"12\",\"type\":\"DOUBLE\"}}}";
                MqttMessage message = new MqttMessage(content.getBytes());
              plugin.getHandler().messageArrived(topic, message);
                newTask()
                        .travelInTime("123")
                        .readIndex("theIndex", "98")
                        .thenDo(ctx -> {
                            counter[0]++;
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> {
                            Assert.assertNull(ctx.result().get(0));
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> {
                            Assert.assertEquals(1, counter[0]);
                        })
                        .execute(graph1, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void customMQTTHandler() {
        final int[] counter = {0}; // Verify that the test reaches its expected end
        try {
                String content = "5;24.5;123";
                MqttMessage message = new MqttMessage(content.getBytes());
                plugin2.getHandler().messageArrived("customTopic", message);
                Thread.sleep(NETWORK_DELAY); // Waiting for the network
                newTask()
                        .travelInTime("123")
                        .readIndex("sensors", "5")
                        .attribute("value")
                        .thenDo(ctx -> {
                            Assert.assertEquals(24.5, ctx.doubleResult(), 0.01);
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> {
                            counter[0]++;
                            ctx.continueTask();
                        })
                        .thenDo(ctx -> {
                            Assert.assertEquals(1, counter[0]);
                        })
                        .execute(graph2, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
