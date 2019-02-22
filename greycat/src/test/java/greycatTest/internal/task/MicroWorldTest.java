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
package greycatTest.internal.task;

import greycat.Callback;
import greycat.Graph;
import greycat.Type;
import greycat.utility.VerbosePlugin;
import greycat.GraphBuilder;

import static greycat.internal.task.CoreActions.*;
import static greycat.Tasks.newTask;

public class MicroWorldTest {

    public static void main(String[] args) {

        Graph g = new GraphBuilder()
                .withMemorySize(10000)
                .withPlugin(new VerbosePlugin())
                .build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean isConnected) {

                newTask().loopPar("1", "2",
                        newTask()
                                .declareIndex("rooms","name")
                                .createNode()
                                .setAttribute("name", Type.STRING, "room_{{i}}")
                                .updateIndex("rooms")
                                .defineAsVar("parentRoom")
                                .loop("1", "3",
                                        newTask()
                                                .then(createNode())
                                                .then(setAttribute("sensor", Type.STRING, "sensor_{{i}}"))
                                        //.then(addTo("sensors", "parentRoom"))
                                )
                ).execute(g, null);


                /*
                loop("0", "3",
                        newNode()
                        .setAttribute("name", Type.STRING, "node_{{i}}")
                        .print("{{result}}")
                )
                .execute(g,null);
*/

            }
        });


    }

}
