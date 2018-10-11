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
package greycatTest.internal;


import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import org.junit.Assert;
import org.junit.Test;

public class GraphRobustness {

    private Graph _graph;

    public GraphRobustness() {
        _graph = new GraphBuilder().build();
        _graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
            }
        });
    }

    @Test
    public void robustnessNewTypedNode() {
        boolean exceptionCaught = false;
        try {
            Node n = _graph.newTypedNode(0, 0, null);
            n.free();
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertEquals(true, exceptionCaught);
    }

    @Test
    public void robustnessCloneNode() {
        boolean exceptionCaught = false;
        try {
            Node n = _graph.cloneNode(null);
            n.free();
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertEquals(true, exceptionCaught);
    }

    @Test
    public void robustnessLookup() {
        boolean exceptionCaught = false;
        try {
            Node n1 = _graph.newNode(0, 0);
            _graph.lookup(0, 0, n1.id(), null);
            n1.free();
        } catch (Exception e) {
            exceptionCaught = true;
        }
        Assert.assertEquals(false, exceptionCaught);
    }

    @Test
    public void robustnessSave() {
        boolean exceptionCaught = false;
        try {
            Node n1 = _graph.newNode(0, 0);
            _graph.save(null);
            n1.free();
        } catch (Exception e) {
            exceptionCaught = true;
        }
        Assert.assertEquals(false, exceptionCaught);
    }

    @Test
    public void robustnessConnect() {
        final boolean[] exceptionCaught = new boolean[1];
        exceptionCaught[0] = false;
        _graph.disconnect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                try {
                    _graph.connect(null);
                } catch (Exception e) {
                    exceptionCaught[0] = true;
                }
            }
        });

        Assert.assertEquals(false, exceptionCaught[0]);
    }

    @Test
    public void robustnessDisconnect() {
        boolean exceptionCaught = false;
        try {
            _graph.disconnect(null);
            _graph.connect(new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {
                }
            });

        } catch (Exception e) {
            exceptionCaught = true;
        }

        Assert.assertEquals(false, exceptionCaught);
    }


    /*
    @Test
    public void robustnessIndex() {
        Node node = _graph.newNode(0, 0);
        node.set("name", "n1");

        boolean exceptionCaught = false;

        //indexName null
        try {
            _graph.index(null, node, "name", new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        Assert.assertEquals(true, exceptionCaught);

        exceptionCaught = false;
        //nodeToIndex null
        try {
            _graph.index("indexName", null, "name", new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown: ");
        }
        Assert.assertEquals(true, exceptionCaught);

        //flatKeyAttributes null
        exceptionCaught = false;
        try {
            _graph.index("name", node, null, new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown: ");
        }
        Assert.assertEquals(true, exceptionCaught);

        //callback null
        try {
            _graph.index("name", node, "name", null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        node.free();
    }

    @Test
    public void robustnessUnindex() {
        Node node = _graph.newNode(0, 0);
        node.set("name", "n1");

        boolean exceptionCaught = false;

        //indexName null
        try {
            _graph.unindex(null, node, "name", new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertEquals(true, exceptionCaught);

        exceptionCaught = false;
        //nodeToIndex null
        try {
            _graph.unindex("indexName", null, "name", new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);

        //flatKeyAttributes null
        exceptionCaught = false;
        try {
            _graph.unindex("name", node, null, new Callback<Boolean>() {
                @Override
                public void on(Boolean result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);

        //callback null
        try {
            _graph.unindex("name", node, "name", null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        node.free();
    }

    @Test
    public void robustnessFind() {
        Node node = _graph.newNode(0, 0);
        node.set("name", "root");
        _graph.index("indexName", node, "name=root", null);

        //indexName
        boolean exceptionCaught = false;
        try {
            _graph.find(0, 0, null, "name=root", new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown: ");
        }
        Assert.assertTrue(exceptionCaught);


        //query
        exceptionCaught = false;
        try {
            _graph.find(0, 0, "indexName", null, new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);

        //callback
        try {
            _graph.find(0, 0, "indexName", "name=root", null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        node.free();
    }

    @Test
    public void robustnessFindQuery() {
        boolean exceptionCaught = false;
        try {
            _graph.findByQuery(null, new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {

                }
            });
        } catch (RuntimeException npe) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);


        try {
            Query q = _graph.newQuery();
            q.setWorld(0);
            q.setTime(0);
            q.setIndexName("indexName");
            _graph.findByQuery(q, null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
    }

    @Test
    public void robustnessAll() {
        Node node = _graph.newNode(0, 0);
        node.set("name", "root");
        _graph.index("indexName", node, "name=root", null);

        //indexName
        boolean exceptionCaught = false;
        try {
            _graph.findAll(0, 0, null, new Callback<Node[]>() {
                @Override
                public void on(Node[] result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);


        //callback
        try {
            _graph.findAll(0, 0, "indexName", null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        node.free();
    }

    @Test
    public void robustnessNamedIndex() {
        Node node = _graph.newNode(0, 0);
        node.set("name", "root");
        _graph.index("indexName", node, "name=root", null);

        //indexName
        boolean exceptionCaught = false;
        try {
            _graph.getIndexNode(0, 0, null, new Callback<Node>() {
                @Override
                public void on(Node result) {

                }
            });
        } catch (RuntimeException e) {
            exceptionCaught = true;
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }
        Assert.assertTrue(exceptionCaught);


        //callback
        try {
            _graph.getIndexNode(0, 0, "indexName", null);
        } catch (Exception e) {
            Assert.fail("Unexpected exception thrown");
        }

        node.free();
    }*/


}
