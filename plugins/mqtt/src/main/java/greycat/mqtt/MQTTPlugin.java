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
import greycat.plugin.Plugin;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Arrays;

public class MQTTPlugin implements Plugin {

    private final String brokerURL;
    private final int brokerPort;
    private final String[] subscriptions;
    private final MessageHandler handler;

    private IMqttClient client;

    /**
     * Build the MQTT plugin
     *
     * @param brokerURL     MQTT Broker URL
     * @param brokerPort    MQTT Broker IP
     * @param subscriptions List of subscriptions
     * @param lookupIndex   Index's name containing the targeted nodes
     */
    public MQTTPlugin(String brokerURL, int brokerPort, String[] subscriptions, String lookupIndex) {
        this.brokerURL = brokerURL;
        this.brokerPort = brokerPort;
        this.subscriptions = subscriptions;
        this.handler = new DefaultMessageHandler(lookupIndex);
    }


    /**
     * Build the MQTT plugin with a custom message handler
     *
     * @param brokerURL     MQTT Broker URL
     * @param brokerPort    MQTT Broker IP
     * @param subscriptions List of subscriptions
     * @param lookupIndex   Index's name containing the targeted nodes
     * @param customHandler Message handler
     */
    public MQTTPlugin(String brokerURL, int brokerPort, String[] subscriptions, String lookupIndex, MessageHandler customHandler) {
        this.brokerURL = brokerURL;
        this.brokerPort = brokerPort;
        this.subscriptions = subscriptions;
        this.handler = customHandler;
    }

    @Override
    public void start(Graph graph) {
        try {
            handler.setGraph(graph);
            client = new MqttClient("tcp://" + this.brokerURL + ":" + this.brokerPort, MqttClient.generateClientId());
            client.setCallback(handler);
            client.connect();
            if (client.isConnected()) {
                Arrays.stream(subscriptions).forEach(s -> {
                    try {
                        client.subscribe(s);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                System.err.println("Not connected client");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void stop() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
