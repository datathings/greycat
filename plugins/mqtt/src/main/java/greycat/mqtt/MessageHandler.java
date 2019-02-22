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
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;

public abstract class MessageHandler implements MqttCallback {

    protected final String lookupIndex;
    private Graph graph = null;

    /**
     * Build the message handler
     *
     * @param lookupIndex Index's name containing the targeted nodes
     */
    public MessageHandler(String lookupIndex) {
        this.lookupIndex = lookupIndex;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph g) {
        this.graph = g;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.err.println("Connection to MQTT has been lost (Broker down? Cable unplugged? Rise of the machines?) The graph will not be updated anymore");
        throwable.printStackTrace();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
