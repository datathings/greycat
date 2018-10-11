/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
import greycat.mqtt.exceptions.BadPayloadException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import static greycat.Tasks.newTask;

public class DefaultMessageHandler extends MessageHandler {

    public DefaultMessageHandler(String lookupIndex) {
        super(lookupIndex);
    }


    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws BadPayloadException {
        String payload = new String(mqttMessage.getPayload());
        try {
            JSONObject obj = new JSONObject(payload);
            String id = null;
            if (obj.has("id")) id = obj.getString("id");
            else
                throw new BadPayloadException("Missing id"); //Extract node id from the JSON message (/!\ Id = id registered in the targeted nodes' index)

            long time = -1;
            if (obj.has("time")) time = obj.getLong("time");
            else throw new BadPayloadException("Missing id"); //Extract time from the JSON message

            if (obj.has("values")) {
                JSONObject values = obj.getJSONObject("values");
                long finalTime = time;
                String finalId = id;
                values.keySet().forEach(attributeName -> { // For each attribute stored in the JSON message, update the node's attributes
                    JSONObject attribute = values.getJSONObject(attributeName);
                    newTask().travelInTime(Long.toString(finalTime))
                            .readIndex(lookupIndex, finalId)
                            .then(CoreActions.forceAttribute(attributeName, Type.typeFromName(attribute.getString("type")), attribute.getString("value")))
                            .execute(super.getGraph(), null);
                });
                super.getGraph().save(null);
            } else throw new BadPayloadException("Missing values");
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }
}
