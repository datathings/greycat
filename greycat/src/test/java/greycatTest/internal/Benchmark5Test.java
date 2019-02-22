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
package greycatTest.internal;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.scheduler.ExecutorScheduler;
import greycat.ActionFunction;
import greycat.TaskContext;
import greycat.TaskResult;
import greycat.Tasks;

import java.util.Random;

/**
 * @ignore ts
 */
public class Benchmark5Test {

    public static void main(String[] args) {
        Graph g = new GraphBuilder()
                //.withScheduler(new HybridScheduler())
                //.withScheduler(new TrampolineScheduler())
                .withScheduler(new ExecutorScheduler())
                .build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                final long previous = System.currentTimeMillis();
                Tasks.newTask().loopPar("0", "999",
                        Tasks.newTask().loop("0", "999",
                                Tasks.newTask().thenDo(new ActionFunction() {
                                    @Override
                                    public void eval(TaskContext ctx) {
                                        Random random = new Random();
                                        for (int i = 0; i < 100; i++) {
                                            random.nextFloat();
                                        }
                                        ctx.continueTask();
                                    }
                                })
                        )
                ).thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        System.out.println("End " + (System.currentTimeMillis() - previous) + " ms");
                        g.disconnect(null);
                    }
                }).execute(g, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        System.out.println();
                    }
                });
            }
        });
        /*
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }

}
