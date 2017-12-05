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

import greycat.Type;
import greycat.internal.task.CoreActions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static greycat.Tasks.newTask;

public class CustomHandler extends MessageHandler {

    /**
     * Build the message handler
     * This handler supports the following message format:
     * node_id;value;timestamp where node_id is String, value is Double and Timestamp is long
     * <p>
     * Targeted node_id have a double value attribute
     *
     * @param lookupIndex Index's name containing the targeted nodes
     */
    public CustomHandler(String lookupIndex) {
        super(lookupIndex);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String content = new String(mqttMessage.getPayload());
        String[] elements = content.split(";");

        if (elements.length != 3)
            throw new RuntimeException("Unparsable message received: " + content);


        String id = null;
        long time = -1L;
        double value = 0;

        try {
            id = elements[0];
            value = Double.parseDouble(elements[1]);
            time = Long.parseLong(elements[2]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unparsable message received: " + content);
        }

        newTask().travelInTime(Long.toString(time))
                .readIndex(lookupIndex, id)
                .then(CoreActions.forceAttribute("value", Type.typeFromName("DOUBLE"), Double.toString(value)))
                .execute(super.getGraph(), null);


        super.getGraph().save(null);


    }
}
