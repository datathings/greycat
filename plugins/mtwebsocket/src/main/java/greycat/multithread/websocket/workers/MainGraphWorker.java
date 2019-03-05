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
package greycat.multithread.websocket.workers;

import greycat.*;
import greycat.chunk.Chunk;
import greycat.internal.CoreGraphLog;
import greycat.multithread.websocket.message.GraphExecutorMessage;
import greycat.multithread.websocket.message.GraphMessage;
import greycat.plugin.Job;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import greycat.utility.Base64;
import greycat.utility.KeyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import static greycat.multithread.websocket.Constants.*;

/**
 * The graph Executor is the thread handling the main graph and dealing with all basic request (get, put, remove, lock, unlock, log), no task should be execute on this graph.
 * this thread is using a message queue as input and a queue provided in the incoming mesage as output.
 */
public class MainGraphWorker extends Thread implements Callback<Buffer> {

    private final GraphBuilder graphBuilder;
    private final BlockingQueue<GraphExecutorMessage> graphInput;
    private final DeferCounterSync defercounter;
    private final BlockingQueue<GraphMessage> defaultoutput;
    private boolean running = true;

    /**
     *
     * @param graphBuilder default graph builder
     * @param graphInput message queue used as input
     * @param deferCounterSync counter reporting that the graph has been connected
     * @param defaultoutput message queue of the websocket server to use for broadcasting
     */
    public MainGraphWorker(GraphBuilder graphBuilder, BlockingQueue<GraphExecutorMessage> graphInput, DeferCounterSync deferCounterSync, BlockingQueue<GraphMessage> defaultoutput) {
        this.graphBuilder = graphBuilder;
        this.graphInput = graphInput;
        this.defercounter = deferCounterSync;
        this.defaultoutput = defaultoutput;
        if (graphBuilder.storage != null) {
            graphBuilder.storage.listen(this);
        } else if (graphBuilder.storageFactory != null) {
            graphBuilder.storageFactory.listen(this);
        }
        setDaemon(false);
    }

    @Override
    public void run() {
        final Graph graph = graphBuilder.build();
        graph.connect(on -> {
            defercounter.count();
            while (running) {
                GraphExecutorMessage graphExecutorMessage = null;
                try {
                    graphExecutorMessage = graphInput.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final BufferIterator it;
                if (graphExecutorMessage != null) {
                    switch (graphExecutorMessage.getOperationId()) {
                        case REQ_GET:
                            it = graphExecutorMessage.getContent().iterator();
                            final List<ChunkKey> keys = new ArrayList<>();
                            while (it.hasNext()) {
                                keys.add(ChunkKey.build(it.next()));
                            }
                            process_get(graph, keys.toArray(new ChunkKey[keys.size()]), graphExecutorMessage);
                            break;
                        case REQ_PUT:
                            it = graphExecutorMessage.getContent().iterator();
                            final List<ChunkKey> flatKeys = new ArrayList<ChunkKey>();
                            final List<Buffer> flatValues = new ArrayList<Buffer>();
                            while (it.hasNext()) {
                                final Buffer keyView = it.next();
                                final Buffer valueView = it.next();
                                if (valueView != null) {
                                    flatKeys.add(ChunkKey.build(keyView));
                                    flatValues.add(valueView);
                                }
                            }
                            final ChunkKey[] collectedKeys = flatKeys.toArray(new ChunkKey[flatKeys.size()]);
                            process_put(graph, collectedKeys, flatValues.toArray(new Buffer[flatValues.size()]), graphExecutorMessage);
                            break;
                        case REQ_REMOVE:
                            it = graphExecutorMessage.getContent().iterator();
                            final List<ChunkKey> rkeys = new ArrayList<ChunkKey>();
                            while (it.hasNext()) {
                                rkeys.add(ChunkKey.build(it.next()));
                            }
                            process_remove(graph, rkeys.toArray(new ChunkKey[rkeys.size()]), graphExecutorMessage);
                            break;
                        case REQ_LOCK:
                            process_lock(graph, graphExecutorMessage);
                            break;
                        case REQ_UNLOCK:
                            process_unlock(graph, graphExecutorMessage.getContent(), graphExecutorMessage);
                            break;
                        case REQ_LOG:
                            it = graphExecutorMessage.getContent().iterator();
                            if (it.hasNext()) {
                                Buffer b = it.next();
                                StringBuilder buf = new StringBuilder();
                                buf.append(Base64.decodeToStringWithBounds(b, 0, b.length()));
                                ((CoreGraphLog) graph.log()).writeMessage(buf);
                            }
                            graphExecutorMessage.getOutputQueue().add(new GraphMessage(RESP_LOG, graphExecutorMessage.getReturnID(), null, graphExecutorMessage.getOriginalCallBack()));
                            break;
                    }

                }
            }
            graph.disconnect(done -> {
                System.out.println("Main Graph Disconnected");
            });
        });
    }


    private void process_get(final Graph graph, ChunkKey[] keys, GraphExecutorMessage originalmessage) {
        final DeferCounter defer = graph.newCounter(keys.length);
        final Buffer[] buffers = new Buffer[keys.length];
        defer.then(new Job() {
            @Override
            public void run() {
                Buffer stream = graph.newBuffer();
                for (int i = 0; i < buffers.length; i++) {
                    if (i != 0) {
                        stream.write(greycat.Constants.BUFFER_SEP);
                    }
                    if (buffers[i] != null) {
                        stream.writeAll(buffers[i].data());
                        buffers[i].free();
                    }
                }

                try {
                    originalmessage.getOutputQueue().put(new GraphMessage(RESP_GET, originalmessage.getReturnID(), stream, originalmessage.getOriginalCallBack()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        for (int i = 0; i < keys.length; i++) {
            final int fixedI = i;
            ChunkKey tuple = keys[i];
            graph.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                @Override
                public void on(Chunk memoryChunk) {
                    if (memoryChunk != null) {
                        final Buffer toSaveBuffer = graph.newBuffer();
                        memoryChunk.save(toSaveBuffer);
                        graph.space().unmark(memoryChunk.index());
                        buffers[fixedI] = toSaveBuffer;
                    } else {
                        buffers[fixedI] = null;
                    }
                    defer.count();
                }
            });
        }

    }

    private void process_put(final Graph graph, final ChunkKey[] keys, final Buffer[] values, GraphExecutorMessage originalmessage) {
        if (keys.length == 0) {
            try {
                originalmessage.getOutputQueue().put(new GraphMessage(RESP_PUT, originalmessage.getReturnID(), null, originalmessage.getOriginalCallBack()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            final DeferCounter defer = graph.newCounter(keys.length);
            defer.then(() -> {
                graph.save(on -> {
                    try {
                        originalmessage.getOutputQueue().put(new GraphMessage(RESP_PUT, originalmessage.getReturnID(), null, originalmessage.getOriginalCallBack()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            });
            for (int i = 0; i < keys.length; i++) {
                final int finalI = i;
                ChunkKey tuple = keys[i];
                graph.space().getOrLoadAndMark(tuple.type, tuple.world, tuple.time, tuple.id, new Callback<Chunk>() {
                    @Override
                    public void on(Chunk memoryChunk) {
                        if (memoryChunk != null) {
                            memoryChunk.loadDiff(values[finalI]);
                            graph.space().unmark(memoryChunk.index());
                        } else {
                            Chunk newChunk = graph.space().createAndMark(tuple.type, tuple.world, tuple.time, tuple.id);
                            if (newChunk != null) {
                                newChunk.loadDiff(values[finalI]);
                                graph.space().unmark(newChunk.index());
                            }
                        }
                        defer.count();
                    }
                });
            }
        }
    }

    private void process_remove(final Graph graph, ChunkKey[] keys, GraphExecutorMessage originalmessage) {
        Buffer buffer = graph.newBuffer();
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) {
                buffer.write(greycat.Constants.BUFFER_SEP);
            }
            ChunkKey tuple = keys[i];
            KeyHelper.keyToBuffer(buffer, tuple.type, tuple.world, tuple.time, tuple.id);
            graph.space().delete(tuple.type, tuple.world, tuple.time, tuple.id);
        }
        graph.storage().remove(buffer, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                buffer.free();
                try {
                    originalmessage.getOutputQueue().put(new GraphMessage(RESP_REMOVE, originalmessage.getReturnID(), null, originalmessage.getOriginalCallBack()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void process_lock(Graph graph, GraphExecutorMessage originalmessage) {
        graph.storage().lock(new Callback<Buffer>() {
            @Override
            public void on(Buffer result) {
                try {
                    originalmessage.getOutputQueue().put(new GraphMessage(RESP_LOCK, originalmessage.getReturnID(), result, originalmessage.getOriginalCallBack()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void process_unlock(Graph graph, Buffer toUnlock, GraphExecutorMessage originalmessage) {
        graph.storage().unlock(toUnlock, new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                try {
                    originalmessage.getOutputQueue().put(new GraphMessage(RESP_UNLOCK, originalmessage.getReturnID(), null, originalmessage.getOriginalCallBack()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public void on(Buffer result) {
        try {
            defaultoutput.put(new GraphMessage(NOTIFY_UPDATE, 0, result, null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
