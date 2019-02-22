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
package greycat.memory;

import greycat.*;
import greycat.scheduler.HybridScheduler;

import static greycat.Tasks.cond;
import static greycat.Tasks.newTask;

@SuppressWarnings("Duplicates")
public class BenchmarkParTask {

    public static void main(String[] args) {

        Graph g = new GraphBuilder()
                .withMemorySize(1000000)
                .withPlugin(new OffHeapMemoryPlugin())
                .withScheduler(new HybridScheduler())
                .build();
        g.connect(result -> {
            final long previous = System.currentTimeMillis();
            final long previousCache = g.space().available();
            newTask().loopPar("0", "9999",
                    newTask()
                            .createNode()
                            .setAttribute("name", Type.STRING, "node_{{i}}")
                            .print("{{result}}")
                            .addToGlobalIndex("nodes", "name")
                            .loop("0", "999",
                                    newTask().travelInTime("{{i}}").setAttribute("val", Type.INT, "{{i}}").clearResult())
                            .ifThen(cond("i % 100 == 0"), newTask().save())
                            .clearResult()
            ).save().readGlobalIndex("nodes").execute(g, new Callback<TaskResult>() {
                @Override
                public void on(TaskResult result) {
                    System.out.println("indexSize=" + result.size());
                    result.free();
                    long after = System.currentTimeMillis();
                    long afterCache = g.space().available();
                    System.out.println(after - previous + "ms");
                    System.out.println(previousCache + "-" + afterCache);
                    g.disconnect(new Callback<Boolean>() {
                        @Override
                        public void on(Boolean result) {
                        }
                    });
                }
            });
        });
    }

}
