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
package greycatTest.internal;

import greycat.*;
import greycat.base.BaseNode;
import greycat.chunk.SuperTimeTreeChunk;
import greycat.chunk.TimeTreeChunk;
import greycat.internal.CoreConstants;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("Duplicates")
public class ResolverTest {

    @Test
    public void endTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                Node n = g.newNode(0, 0);
                Assert.assertEquals("{\"world\":0,\"time\":0,\"id\":1,\"group\":0}", n.toString());

                g.lookup(0, 10, n.id(), new Callback<Node>() {
                    @Override
                    public void on(final Node unPhased) {
                        Assert.assertEquals("{\"world\":0,\"time\":10,\"id\":1,\"group\":0}", unPhased.toString());
                        unPhased.end();
                        g.lookup(0, 15, n.id(), new Callback<Node>() {
                            @Override
                            public void on(final Node dead) {
                                Assert.assertTrue(dead == null);
                            }
                        });
                    }
                });

            }
        });
    }

    @Test
    public void timeTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                BaseNode n = (BaseNode) g.newNode(0, 0);
                for (long i = 0; i < CoreConstants.TREE_SCALES[0]; i++) {
                    final long finalI = i;
                    g.lookup(0, i, n.id(), new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("time", Type.LONG, finalI);
                        }
                    });
                }
                SuperTimeTreeChunk stt = (SuperTimeTreeChunk) g.space().get(n._index_superTimeTree);
                TimeTreeChunk tt = (TimeTreeChunk) g.space().get(n._index_timeTree);
                Assert.assertTrue(stt.size() == 1);
                Assert.assertTrue(tt.size() == CoreConstants.TREE_SCALES[0]);
                for (long i = CoreConstants.TREE_SCALES[0]; i < CoreConstants.TREE_SCALES[1]; i++) {
                    final long finalI = i;
                    g.lookup(0, i, n.id(), new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("time", Type.LONG, finalI);
                        }
                    });
                }
                stt = (SuperTimeTreeChunk) g.space().get(n._index_superTimeTree);
                Assert.assertTrue(stt.size() == 10);
            }
        });
    }

    @Test
    public void pastInsertTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                BaseNode n = (BaseNode) g.newNode(0, 0);

                for (long i = 0; i < CoreConstants.TREE_SCALES[0]; i++) {
                    final long finalI = i;
                    g.lookup(0, i, n.id(), new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("time", Type.LONG, finalI);
                        }
                    });
                }
                g.lookup(0, CoreConstants.TREE_SCALES[0] * 2, n.id(), new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        result.set("time", Type.LONG, CoreConstants.TREE_SCALES[0]);
                    }
                });
                g.lookup(0, CoreConstants.TREE_SCALES[0] + 100, n.id(), new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        result.set("time", Type.LONG, CoreConstants.TREE_SCALES[0] + 100);
                    }
                });
                SuperTimeTreeChunk stt = (SuperTimeTreeChunk) g.space().get(n._index_superTimeTree);
                Assert.assertTrue(stt.size() == 2);
            }
        });
    }

    @Test
    public void dephasingTest() {
        long today = 1000;
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);
        BaseNode n = (BaseNode) g.newNode(0, today);
        n.set("name", Type.STRING, "myName");
        n.travelInTime(today + 100, new Callback<Node>() {
            @Override
            public void on(Node result) {
                Assert.assertEquals("{\"world\":0,\"time\":1100,\"id\":1,\"group\":0,\"name\":\"myName\"}", result.toString());
                Assert.assertEquals(100, result.timeDephasing());
            }
        });
    }

    @Test
    public void lookupTimesSparseTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                long availableBefore = g.space().available();

                BaseNode n = (BaseNode) g.newNode(0, 0);
                for (long i = 1000; i < 2000; i++) {
                    final long finalI = i;
                    g.lookup(0, i, n.id(), new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("time", Type.LONG, finalI);
                            result.free();
                        }
                    });
                }

                int limit = 50;
                g.lookupTimesSparse(0, 100, 10000, n.id(), limit, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(limit, result.length);
                        g.freeNodes(result);
                    }
                });
                n.free();
                g.save(null);

                long availableAfter = g.space().available();
                Assert.assertEquals(availableBefore, availableAfter);
            }
        });
    }

    @Test
    public void lookupTimesTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                long availableBefore = g.space().available();
                BaseNode n = (BaseNode) g.newNode(0, 0);
                for (long i = 1000; i < 2000; i++) {
                    final long finalI = i;
                    g.lookup(0, i, n.id(), new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            result.set("time", Type.LONG, finalI);
                            result.free();
                        }
                    });
                }
                //From, to, not reversed, complete
                g.lookupTimes(0, 1000, 2000, n.id(), -1, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(1000, result.length);
                        //check order
                        for (long i = 0; i < result.length; i++) {
                            Assert.assertEquals(result[(int) i].get("time"), i + 1000);
                        }
                        g.freeNodes(result);
                    }
                });
                //From, to, not reversed, not complete
                g.lookupTimes(0, 1000, 2000, n.id(), 500, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(500, result.length);
                        //check order
                        for (long i = 0; i < result.length; i++) {
                            Assert.assertEquals(result[(int) i].get("time"), i + 1000);
                        }
                        g.freeNodes(result);
                    }
                });
                //To, from, reversed, complete
                g.lookupTimes(0, 2000, 1000, n.id(), 1000, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(1000, result.length);
                        //check order
                        for (long i = 0; i < result.length; i++) {
                            Assert.assertEquals(result[(int) i].get("time"), 2000 - i - 1);
                        }
                        g.freeNodes(result);
                    }
                });
                //To, from, reversed, not complete
                g.lookupTimes(0, 2000, 1000, n.id(), 500, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(500, result.length);
                        //check order
                        for (long i = 0; i < result.length; i++) {
                            Assert.assertEquals(result[(int) i].get("time"), 2000 - i - 1);
                        }
                        g.freeNodes(result);
                    }
                });
                //BEGINNING, END, reversed, not complete
                g.lookupTimes(0, Constants.END_OF_TIME, Constants.BEGINNING_OF_TIME, n.id(), 10, new Callback<Node[]>() {
                    @Override
                    public void on(Node[] result) {
                        //check size
                        Assert.assertEquals(10, result.length);
                        //check order
                        for (long i = 0; i < result.length; i++) {
                            Assert.assertEquals(result[(int) i].get("time"), 2000 - i - 1);
                        }
                        g.freeNodes(result);
                    }
                });
                n.free();
                g.save(null);
                long availableAfter = g.space().available();
                Assert.assertEquals(availableBefore, availableAfter);
            }
        });
    }


}
