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
import org.mwg.plugin.Job;
import org.mwg.plugin.SchedulerAffinity;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mwg.benchmark.JsonHandler.METRIC_TEMPORAL_TRAVERSE_THEN_READ;

public class TrampolineCallback {

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
        benchmark.add("benchmark", TrampolineCallback.class.getSimpleName() + (isOffHeap ? "_offheap" : "_heap"));
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
                final DeferCounter defer = g.newCounter(times);
                final AtomicInteger timeCounter = new AtomicInteger(0);
                insert(position, timeCounter, defer, times);
                defer.then(new Job() {
                    public void run() {
                        final long afterInsert = System.currentTimeMillis();
                        double insertTimeSecond = (afterInsert - before) / 1000d;
                        System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_INSERT + " " + times / insertTimeSecond + " ops/s");
                        metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_INSERT).set("value", times / insertTimeSecond));
                        final DeferCounter readFlat = g.newCounter(times);
                        timeCounter.set(0);//reset counter
                        final AtomicInteger fakeSum = new AtomicInteger();
                        read(position, timeCounter, readFlat, times, fakeSum);
                        readFlat.then(new Job() {
                            public void run() {
                                final long afterRead = System.currentTimeMillis();
                                double insertTimeSecond = (afterRead - afterInsert) / 1000d;
                                System.out.println("\t" + JsonHandler.METRIC_TEMPORAL_READ + " " + times / insertTimeSecond + " ops/s");
                                metrics.add(new JsonObject().set("name", JsonHandler.METRIC_TEMPORAL_READ).set("value", times / insertTimeSecond));
                                timeCounter.set(0);//reset counter
                                fakeSum.set(0);
                                final DeferCounter readRelation = g.newCounter(times);
                                readAndTraverse(user, timeCounter, readRelation, times, fakeSum);
                                readRelation.then(new Job() {
                                    public void run() {
                                        long afterReadRelation = System.currentTimeMillis();
                                        double insertTimeSecond = (afterReadRelation - afterRead) / 1000d;
                                        System.out.println("\t" + METRIC_TEMPORAL_TRAVERSE_THEN_READ + " " + (times / insertTimeSecond) + " ops/s");
                                        metrics.add(new JsonObject().set("name", METRIC_TEMPORAL_TRAVERSE_THEN_READ).set("value", times / insertTimeSecond));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private static void insert(final Node position, final AtomicInteger counter, final DeferCounter defer, final int max) {
        final int time = counter.incrementAndGet();
        position.travelInTime(time, new Callback<Node>() {
            public void on(Node timedNode) {
                timedNode.set("lat", Type.DOUBLE, time + 10.5);
                timedNode.set("long", Type.DOUBLE, time + 10.5);
                defer.count();
                if (time != max) {
                    position.graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, new Job() {
                        public void run() {
                            insert(position, counter, defer, max);
                        }
                    });
                }
            }
        });
    }

    private static void read(final Node position, final AtomicInteger counter, final DeferCounter defer, final int max, final AtomicInteger fakeSum) {
        final int time = counter.incrementAndGet();
        position.travelInTime(time, new Callback<Node>() {
            public void on(Node timedNode) {
                fakeSum.addAndGet(((Double) timedNode.get("lat")).intValue());
                fakeSum.addAndGet(((Double) timedNode.get("long")).intValue());
                timedNode.free(); // optional if we want to keep everything in memory
                defer.count();
                if (time != max) {
                    position.graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, new Job() {
                        public void run() {
                            read(position, counter, defer, max, fakeSum);
                        }
                    });
                }
            }
        });
    }

    private static void readAndTraverse(final Node user, final AtomicInteger counter, final DeferCounter defer, final int max, final AtomicInteger fakeSum) {
        final int time = counter.incrementAndGet();
        user.travelInTime(time, new Callback<Node>() {
            public void on(final Node timeUser) {
                timeUser.relation("position", new Callback<Node[]>() {
                    public void on(Node[] timedPositions) {
                        final Node position = timedPositions[0];
                        fakeSum.addAndGet(((Double) position.get("lat")).intValue());
                        fakeSum.addAndGet(((Double) position.get("long")).intValue());

                        user.graph().freeNodes(timedPositions); //optional if we want to keep everything in memory
                        timeUser.free(); //optional if we want to keep everything in memory

                        defer.count();
                        if (time != max) {
                            position.graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, new Job() {
                                public void run() {
                                    readAndTraverse(user, counter, defer, max, fakeSum);
                                }
                            });
                        }
                    }
                });
            }
        });
    }


}
