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
import org.json.JSONException;
import org.json.JSONObject;
import static greycat.Tasks.newTask;

/**
 * Created by Cyril Cecchinel - I3S Laboratory on 02/08/2017.
 */
public class DefaultMessageRouter extends MessageHandler {

    public DefaultMessageRouter(String lookupIndex) {
        super(lookupIndex);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        String payload = new String(mqttMessage.getPayload());
        try {
            JSONObject obj = new JSONObject(payload);
            String id = obj.getString("id");
            long time = obj.getLong("time");

            if (obj.has("value")) {
                newTask()
                        .travelInTime(Long.toString(time))
                        .readIndex(lookupIndex, id)
                        .then(CoreActions.setAttribute("value", Type.DOUBLE, obj.getString("value")))
                        .execute(super.getGraph(), null);
            }




        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }

    }
}
