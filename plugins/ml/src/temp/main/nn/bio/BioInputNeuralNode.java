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
package greycat.ml.neuralnet.bio;

import greycat.Callback;
import greycat.Graph;
import greycat.Task;
import greycat.TaskContext;
import greycat.base.BaseNode;
import greycat.plugin.Job;
import greycat.plugin.SchedulerAffinity;
import greycat.struct.LongLongArrayMap;
import greycat.struct.LongLongMap;
import greycat.utility.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static greycat.Tasks.newTask;
import static greycat.internal.task.CoreActions.lookup;
import static greycat.internal.task.CoreActions.readVar;

class BioInputNeuralNode extends BaseNode {

    static String NAME = "BioInputNeuralNode";

    public BioInputNeuralNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    public void learn2(final double value, final int spikeLimit, final double threshold, final Callback callback) {
        final Task[] job = new Task[1];
        job[0] = newTask().then(readVar("target")).forEach(
                newTask()
                        .then(lookup("{{result}}"))
                        .thenDo(context -> {
                            BioNeuralNode neural = (BioNeuralNode) context.result().get(0);
                            LongLongMap outputs = (LongLongMap) neural.get(BioNeuralNetwork.RELATION_OUTPUTS);
                            outputs.each((key, value1) -> context.addToVariable("target", key));
                            double signal = (double) context.variable("signal").get(0);
                            long sender = (long) context.variable("sender").get(0);
                            double nextSignal = neural.learn(sender, signal, spikeLimit, threshold);
                            context.setVariable("signal", nextSignal);
                            context.setVariable("sender", neural.id());
                        })
                        .ifThen(context -> ((double) context.variable("signal").get(0)) > 0,
                                newTask().thenDo(context -> {
                                    TaskContext ctx = job[0].prepare(graph(), null, result -> {
                                        context.continueTask();
                                    });
                                    ctx.setVariable("signal", context.variable("signal"));
                                    ctx.setVariable("sender", context.variable("sender"));
                                    ctx.setVariable("target", context.variable("target"));
                                    job[0].executeUsing(ctx);
                                })));
        TaskContext ctx = job[0].prepare(graph(), null, callback);
        ctx.setVariable("signal", value);
        ctx.setVariable("sender", id());
        LongLongMap outputs = (LongLongMap) get(BioNeuralNetwork.RELATION_OUTPUTS);
        outputs.each((key, value1) -> {
            ctx.addToVariable("target", key);
        });
        job[0].executeUsing(ctx);
    }

    public void learn(final double value, final int spikeLimit, final double threshold, final Callback callback) {
        final Map<Long, List<Tuple<Long, Double>>> loop = new HashMap<Long, List<Tuple<Long, Double>>>();
        LongLongMap firstHiddenLayer = (LongLongMap) get(BioNeuralNetwork.RELATION_OUTPUTS);
        //fill with first layer
        firstHiddenLayer.each((key, value1) -> {
            List<Tuple<Long, Double>> previous = loop.get(key);
            if (previous == null) {
                previous = new ArrayList<Tuple<Long, Double>>();
                loop.put(key, previous);
            }
            previous.add(new Tuple<Long, Double>(this.id(), value));
        });
        final Job[] job = new Job[1];
        job[0] = new Job() {
            @Override
            public void run() {
                long[] keys = new long[loop.size()];
                //ugly !!!
                int index = 0;
                for (Long v : loop.keySet()) {
                    keys[index] = v;
                    index++;
                }
                graph().lookupAll(world(), time(), keys, nextLayers -> {
                    for (int i = 0; i < nextLayers.length; i++) {
                        if (nextLayers[i] instanceof BioOutputNeuralNode) {
                            BioOutputNeuralNode neural = (BioOutputNeuralNode) nextLayers[i];
                            long currentId = neural.id();
                            List<Tuple<Long, Double>> signals = loop.get(currentId);
                            loop.remove(currentId);
                            for (int j = 0; j < signals.size(); j++) {
                                double nextSignal = neural.learn(signals.get(j).left(), signals.get(j).right(), spikeLimit, threshold);
                                //TODO signal
                            }
                        } else {
                            BioNeuralNode neural = (BioNeuralNode) nextLayers[i];
                            long currentId = neural.id();
                            List<Tuple<Long, Double>> signals = loop.get(currentId);
                            loop.remove(currentId);
                            final LongLongArrayMap outputs = (LongLongArrayMap) neural.get(BioNeuralNetwork.RELATION_OUTPUTS);
                            for (int j = 0; j < signals.size(); j++) {
                                double nextSignal = neural.learn(signals.get(j).left(), signals.get(j).right(), spikeLimit, threshold);
                                if (nextSignal != 0) {
                                    outputs.each((key, value1) -> {
                                        List<Tuple<Long, Double>> previous = loop.get(key);
                                        if (previous == null) {
                                            previous = new ArrayList<Tuple<Long, Double>>();
                                            loop.put(key, previous);
                                        }
                                        previous.add(new Tuple<Long, Double>(currentId, nextSignal));
                                    });
                                    //stack
                                }
                            }
                        }
                        //in any case free
                        nextLayers[i].free();
                    }
                    if (loop.size() > 0) {
                        graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, job[0]);
                    }
                });
            }
        };
        graph().scheduler().dispatch(SchedulerAffinity.SAME_THREAD, job[0]);


    }

    /*
    private static Task processLayer =
            foreach(
                    defineVar("source")
                            .traverse(BioNeuralNetwork.RELATION_OUTPUTS)
                            .then(context -> {
                                TaskResult<Node> currentNode = context.resultAsNodes();
                                for (int i = 0; i < currentNode.size(); i++) {

                                }
                            })
            );


    private static Task forwardTask = doWhile(processLayer, context -> context.result().size() > 0);
    */

}
