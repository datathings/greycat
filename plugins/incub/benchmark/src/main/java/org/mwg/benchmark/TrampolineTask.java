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
package org.mwg.benchmark;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.mwg.*;
import org.mwg.internal.scheduler.TrampolineScheduler;
import org.mwg.memory.offheap.OffHeapMemoryPlugin;
import org.mwg.task.Task;

import static org.mwg.task.Tasks.newTask;

public class TrampolineTask {

    public static void main(String[] args) {

        boolean isOffHeap = false;
        if (args.length > 0 && args[0].equals("offheap")) {
            isOffHeap = true;
        }
        final GraphBuilder builder = GraphBuilder
                .newBuilder()
                .withScheduler(new TrampolineScheduler())
                .withMemorySize(8000000);
        /*
        if (isOffHeap) {
            builder.withPlugin(new OffHeapMemoryPlugin());
        }*/
        final Graph g = builder.build();

        final JsonObject benchmark = new JsonObject();
        benchmark.add("benchmark", TrampolineTask.class.getSimpleName() + (isOffHeap ? "_offheap" : "_heap"));
        final JsonArray metrics = new JsonArray();
        benchmark.add("metrics", metrics);

        JsonHandler.global.add(benchmark);

        g.connect(new Callback<Boolean>() {
            public void on(Boolean result) {

                final int times = 1000000;
                Task t = newTask();
                t.createNode().defineAsGlobalVar("positionPoint");
                t.createNode().setAttribute("name", Type.STRING, "hello").defineAsGlobalVar("user");
                t.addVarToRelation("position", "positionPoint");
                t.readVar("positionPoint");
                t.thenDo((ctx) -> ctx.setGlobalVariable("before", System.currentTimeMillis()).continueTask());
                //step insert
                t.loop("0", "" + times,
                        newTask().travelInTime("{{i}}").thenDo(ctx -> {
                            ctx.resultAsNodes().get(0)
                                    // .set("lat", Type.DOUBLE, "{{=i+10.5}}") //allow this writing
                                    .set("lat", Type.DOUBLE, ((Integer) ctx.variable("i").get(0)) + 10.5)
                                    .set("long", Type.DOUBLE, ((Integer) ctx.variable("i").get(0)) + 10.5);
                            //System.out.println(ctx.variable("i").get(0) + "->" + ctx.resultAsNodes().get(0));
                            ctx.continueTask();
                        })
                );
                t.thenDo(ctx -> {
                    long afterInsert = System.currentTimeMillis();
                    ctx.setGlobalVariable("afterInsert", afterInsert);
                    long before = (Long) ctx.variable("before").get(0);
                    double timeInSeconds = (afterInsert - before) / 1000d;
                    System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_INSERT + " " + times / timeInSeconds + " ops/s");
                    metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_INSERT).set("value", times / timeInSeconds));
                    ctx.continueTask();
                });
                //step read position
                t.loop("0", "" + times,
                        newTask().travelInTime("{{i}}").pipeTo(newTask().attribute("lat").attribute("long"))
                );
                t.thenDo(ctx -> {
                    long afterRead = System.currentTimeMillis();
                    ctx.setGlobalVariable("afterRead", afterRead);
                    long before = (Long) ctx.variable("afterInsert").get(0);
                    double timeInSeconds = (afterRead - before) / 1000d;
                    System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_READ + " " + times / timeInSeconds + " ops/s");
                    metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_READ).set("value", times / timeInSeconds));
                    ctx.continueTask();
                });
                t.readVar("user");
                t.loop("0", "" + times,
                        newTask().travelInTime("{{i}}").traverse("position").pipeTo(newTask().attribute("lat").attribute("long"))
                );
                t.thenDo(ctx -> {
                    long afterReadRelation = System.currentTimeMillis();
                    long before = (Long) ctx.variable("afterRead").get(0);
                    double timeInSeconds = (afterReadRelation - before) / 1000d;
                    System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_TRAVERSE_THEN_READ + " " + times / timeInSeconds + " ops/s");
                    metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_TRAVERSE_THEN_READ).set("value", times / timeInSeconds));
                    ctx.continueTask();
                });
                t.execute(g, null);
            }
        });
    }

}
