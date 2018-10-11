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
package org.mwg.benchmark;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.mwg.*;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.memory.offheap.OffHeapMemoryPlugin;

import java.util.concurrent.atomic.AtomicInteger;

public class NoopCallback {

    public static void main(String[] args) {

        boolean isOffHeap = false;
        if (args.length > 0 && args[0].equals("offheap")) {
            isOffHeap = true;
        }
        final GraphBuilder builder = GraphBuilder
                .newBuilder()
                .withScheduler(new NoopScheduler())
                .withMemorySize(8000000);
        /*
        if (isOffHeap) {
            builder.withPlugin(new OffHeapMemoryPlugin());
        }
        */
        final Graph g = builder.build();

        final JsonObject benchmark = new JsonObject();
        benchmark.add("benchmark", NoopCallback.class.getSimpleName()+(isOffHeap ? "_offheap":"_heap"));
        final JsonArray metrics = new JsonArray();
        benchmark.add("metrics", metrics);

        JsonHandler.global.add(benchmark);

        g.connect(new Callback<Boolean>() {
            public void on(Boolean result) {
                final int times = 1000000;
                final Node user = g.newNode(0, 0);
                user.set("name", Type.STRING, "hello");
                final Node position = g.newNode(0, 0);
                user.addToRelation("position", position);

                final long before = System.currentTimeMillis();
                for (int i = 0; i < times; i++) {
                    final int finalI = i;
                    position.travelInTime(i, new Callback<Node>() {
                        public void on(Node timedNode) {
                            timedNode.set("lat", Type.DOUBLE, finalI + 10.5);
                            timedNode.set("long", Type.DOUBLE, finalI + 10.5);
                            timedNode.free();
                        }
                    });
                }
                final long afterInsert = System.currentTimeMillis();
                double insertTimeSecond = (afterInsert - before) / 1000d;
                System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_INSERT + " " + times / insertTimeSecond + " ops/s");
                metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_INSERT).set("value", times / insertTimeSecond));
                final AtomicInteger fakeSum = new AtomicInteger(0);
                for (int i = 0; i < times; i++) {
                    final int finalI = i;
                    position.travelInTime(finalI, new Callback<Node>() {
                        public void on(Node timedNode) {
                            fakeSum.addAndGet(((Double) timedNode.get("lat")).intValue());
                            fakeSum.addAndGet(((Double) timedNode.get("long")).intValue());
                            timedNode.free(); // optional if we want to keep everything in memory
                        }
                    });
                }
                final long afterRead = System.currentTimeMillis();
                double insertTimeSecondRead = (afterRead - afterInsert) / 1000d;
                System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_READ + " " + times / insertTimeSecondRead + " ops/s");

                metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_READ).set("value", times / insertTimeSecondRead));

                fakeSum.set(0);
                for (int i = 0; i < times; i++) {
                    final int finalI = i;
                    user.travelInTime(finalI, new Callback<Node>() {
                        public void on(final Node timeUser) {
                            timeUser.relation("position", new Callback<Node[]>() {
                                public void on(Node[] timedPositions) {
                                    final Node position = timedPositions[0];
                                    fakeSum.addAndGet(((Double) position.get("lat")).intValue());
                                    fakeSum.addAndGet(((Double) position.get("long")).intValue());

                                    user.graph().freeNodes(timedPositions); //optional if we want to keep everything in memory
                                    timeUser.free(); //optional if we want to keep everything in memory

                                }
                            });
                        }
                    });
                }

                final long afterReadRelation = System.currentTimeMillis();
                final double insertTimeSecondReadWithRelation = (afterReadRelation - afterRead) / 1000d;
                System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_TRAVERSE_THEN_READ + " " + (times / insertTimeSecondReadWithRelation) + " ops/s");

                metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_TRAVERSE_THEN_READ).set("value", times / insertTimeSecondRead));

            }
        });
    }

}
