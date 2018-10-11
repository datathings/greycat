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
package org.mwg;

import org.mwg.task.Task;

import java.io.File;
import java.io.IOException;

public class StorageTest {

    public static void main(String[] args) {
        try {
            test(new GraphBuilder().withStorage(new RiakStorage("localhost:32775,localhost:32773,localhost:32771,localhost:32769,localhost:8087")).withMemorySize(2000000).build());
            //  Thread.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // @Test
    public void test() throws IOException {
        try {
            test(new GraphBuilder().withStorage(new RiakStorage("127.0.0.1")).withMemorySize(2000000).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    final static int valuesToInsert = 1000000;
    final static long timeOrigin = 1000;

    private static void test(final Graph graph) throws IOException {
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                System.out.println("Connected");
                Task t = newTask();
                t.thenDo(context -> {
                    context.setGlobalVariable("beginning", System.currentTimeMillis());
                    context.continueTask();
                });
                t.loop("0", "100",
                        ifThen(cond("i % 20 == 0"), then(save()).thenDo(context -> {
                            System.out.println(System.currentTimeMillis());
                            context.continueTask();
                        }))
                                .then(createNode())
                );
                t.thenDo(context -> {
                    long before = (long) context.variable("beginning").get(0);
                    context.setGlobalVariable("execTime", ((System.currentTimeMillis() - before) / 1000));
                    context.continueTask();
                });
                t.then(println("{{execTime}} seconds"));
                t.execute(graph, result1 -> {
                    graph.disconnect(result2 -> {
                        File data = new File("data");
                        if (data.exists()) {
                            try {
                                delete(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                });



                /*
                new Thread(() -> {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
                */


                /*
                final long before = System.currentTimeMillis();

                final Node node = graph.newNode(0, 0);
                final DeferCounter counter = graph.newCounter(valuesToInsert);
                for (long i = 0; i < valuesToInsert; i++) {
                    if (i % 1 == 0) {
                        System.out.println("<insert til " + i + " in " + (System.currentTimeMillis() - before) / 1000 + "s");
                        graph.save(null);
                    }
                    final double value = i * 0.3;
                    final long time = timeOrigin + i;
                    graph.lookup(0, time, node.id(), new Callback<Node>() {
                        @Override
                        public void on(Node timedNode) {
                            timedNode.set("value", Type.DOUBLE, value);
                            counter.count();
                            timedNode.free();//free the node, for cache management
                        }
                    });
                }
                node.free();

                counter.then(new Job() {
                    @Override
                    public void run() {

                        long beforeRead = System.currentTimeMillis();

                        //System.out.println("<end insert phase>" + " " + (System.currentTimeMillis() - before) / 1000 + "s");
                        //System.out.println(name + " result: " + (valuesToInsert / ((System.currentTimeMillis() - before) / 1000) / 1000) + "kv/s");

                        graph.disconnect(new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                //System.out.println("Graph disconnected");
                            }
                        });
                    }

                });
                */

            }
        });

    }

    private static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            //directory is empty, then delete it
            if (file.list().length == 0) {
                file.delete();
            } else {
                //list all the directory contents
                String files[] = file.list();
                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }
                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                }
            }

        } else {
            //if file, then delete it
            file.delete();
        }
    }

}
