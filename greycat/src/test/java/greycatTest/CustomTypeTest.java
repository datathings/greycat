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
package greycatTest;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.plugin.TypeFactory;
import greycat.scheduler.NoopScheduler;
import greycat.struct.EStructArray;
import greycat.utility.HashHelper;
import greycatTest.internal.MockStorage;
import greycatTest.utility.GPSPosition;
import org.junit.Assert;
import org.junit.Test;

public class CustomTypeTest {

    @Test
    public void initTest() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withStorage(storage).withScheduler(new NoopScheduler()).build();
        g.typeRegistry().getOrCreateDeclaration("GPSPosition").setFactory(new TypeFactory() {
            @Override
            public Object wrap(final EStructArray backend) {
                return new GPSPosition(backend);
            }
        });
        g.connect(null);

        Node n = g.newNode(0, 0);
        GPSPosition position = (GPSPosition) n.getOrCreate("complexAtt", HashHelper.hash("GPSPosition"));
        Assert.assertEquals("position(1.5,1.5)", position.toString());
        GPSPosition position1 = (GPSPosition) n.getOrCreate("complexAtt", HashHelper.hash("GPSPosition"));
        Assert.assertEquals("position(1.5,1.5)", position1.toString());
    }

    @Test
    public void autoProxyTest() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withStorage(storage).withScheduler(new NoopScheduler()).build();
        g.typeRegistry().getOrCreateDeclaration("GPSPosition").setFactory(new TypeFactory() {
            @Override
            public Object wrap(final EStructArray backend) {
                return new GPSPosition(backend);
            }
        });
        g.connect(null);

        Node n = g.newNode(0, 0);
        GPSPosition position = (GPSPosition) n.getOrCreate("complexAtt", HashHelper.hash("GPSPosition"));
        position.setPosition(42.5d, 84.5d);

        n.travelInTime(100, new Callback<Node>() {
            @Override
            public void on(Node n_100) {
                GPSPosition position_n100 = (GPSPosition) n_100.get("complexAtt");
                position_n100.setPosition(50.5, 90.5);
                Assert.assertEquals("position(50.5,90.5)", position_n100.toString());
                Assert.assertEquals("position(42.5,84.5)", position.toString());
                Assert.assertEquals(position.get("lat") + "", "42.5");
                Assert.assertEquals(position.lat() + "", "42.5");
                Assert.assertEquals(position.get("lng") + "", "84.5");
            }
        });
    }

    @Test
    public void saveLoadTest() {
        MockStorage storage = new MockStorage();
        Graph g = GraphBuilder.newBuilder().withStorage(storage).withScheduler(new NoopScheduler()).build();
        g.typeRegistry().getOrCreateDeclaration("GPSPosition").setFactory(new TypeFactory() {
            @Override
            public Object wrap(EStructArray backend) {
                return new GPSPosition(backend);
            }
        });
        g.connect(null);

        Node n = g.newNode(0, 0);
        GPSPosition position = (GPSPosition) n.getOrCreate("complexAtt", HashHelper.hash("GPSPosition"));
        Assert.assertEquals("position(1.5,1.5)", position.toString());
        position.setPosition(42.5d, 84.5d);
        Assert.assertEquals("position(42.5,84.5)", position.toString());

        n.free();
        g.save(null);

        //Now try to load again
        Graph g2 = GraphBuilder.newBuilder().withStorage(storage).withScheduler(new NoopScheduler()).build();
        g2.typeRegistry().getOrCreateDeclaration("GPSPosition").setFactory(new TypeFactory() {
            @Override
            public Object wrap(EStructArray backend) {
                return new GPSPosition(backend);
            }
        });
        g2.connect(null);
        g2.lookup(0, 0, n.id(), new Callback<Node>() {
            @Override
            public void on(Node g2_n) {
                GPSPosition position2 = (GPSPosition) g2_n.get("complexAtt");
                Assert.assertEquals("position(42.5,84.5)", position2.toString());
            }
        });
    }

}
