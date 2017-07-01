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

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.Type;
import greycat.scheduler.NoopScheduler;
import greycat.struct.EStructArray;
import greycat.struct.EStruct;
import greycat.struct.Relation;
import org.junit.Assert;
import org.junit.Test;

public class ProxyTest {

    @Test
    public void relationTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(result -> {
            Node n = g.newNode(0, 0);
            Relation rel = (Relation) n.getOrCreate("myRel", Type.RELATION);
            rel.add(42);
            Assert.assertEquals(42, rel.get(0));
            Assert.assertEquals(1, rel.size());
            n.travelInTime(10, n_t10 -> {
                Relation rel_10 = (Relation) n_t10.get("myRel");
                rel_10.remove(42);//mutable operation, here we expect an automatic clone
                Assert.assertEquals(0, rel_10.size());
                Assert.assertEquals(1, rel.size());
            });
        });
    }

    @Test
    public void eGraphTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(result -> {
            Node n = g.newNode(0, 0);
            EStructArray eg = (EStructArray) n.getOrCreate("eg", Type.ESTRUCT_ARRAY);
            eg.newEStruct();
            Assert.assertEquals(1, eg.size());
            n.travelInTime(10, n_t10 -> {
                EStructArray eg_10 = (EStructArray) n_t10.get("eg");
                eg_10.newEStruct();//mutable operation, here we expect an automatic clone
                Assert.assertEquals(1, eg.size());
                Assert.assertEquals(2, eg_10.size());
            });
        });
    }

    @Test
    public void eGraphRelationTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(result -> {
            Node n = g.newNode(0, 0);
            EStructArray eg = (EStructArray) n.getOrCreate("eg", Type.ESTRUCT_ARRAY);
            EStruct egn = eg.newEStruct();
            Relation egn_r = (Relation) egn.getOrCreate("myrel", Type.RELATION);
            egn_r.add(42);
            n.travelInTime(10, n_t10 -> {
                EStructArray eg10 = (EStructArray) n_t10.get("eg");
                EStruct eg10n = eg10.estruct(0);
                Relation eg10n_r = (Relation) eg10n.get("myrel");
                eg10n_r.add(43);
                Assert.assertEquals(1, egn_r.size());
                Assert.assertEquals(2, eg10n_r.size());
            });

            /*
            Assert.assertEquals(1, eg.size());
            n.travelInTime(10, n_t10 -> {
                EStructArray eg_10 = (EStructArray) n_t10.get("eg");
                eg_10.newEStruct();//mutable operation, here we expect an automatic clone
                Assert.assertEquals(1, eg.size());
                Assert.assertEquals(2, eg_10.size());
            });
            */
        });
    }


}
