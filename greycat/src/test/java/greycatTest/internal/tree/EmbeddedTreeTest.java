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
package greycatTest.internal.tree;

import greycat.*;
import greycat.internal.custom.NDTree;
import greycat.scheduler.NoopScheduler;
import greycat.struct.TreeResult;
import greycat.utility.HashHelper;
import org.junit.Assert;
import org.junit.Test;

public class EmbeddedTreeTest {

    @Test
    public void NDTest() {
        final Graph g = GraphBuilder.newBuilder()
                .withScheduler(new NoopScheduler())
                .withMemorySize(100)
                .build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                Node node = g.newNode(0, 0);
                NDTree profile = (NDTree) node.getOrCreate("mindex", HashHelper.hash(NDTree.NAME));
                //configure the profile
                profile.setMinBound(new double[]{0, 0});
                profile.setMaxBound(new double[]{23, 1000});
                profile.setResolution(new double[]{1, 100});
                //profile 550w/h at noon
                profile.insert(new double[]{12, 550},1);
                //profile 450w/h at noon
                profile.insert(new double[]{12, 450},1);
                //profile 800w/h at 4.PM
                profile.insert(new double[]{16, 800},1);

                TreeResult treeResult = profile.queryAround(new double[]{12, 520}, 1);

                Assert.assertEquals(1, treeResult.size());
                double[] retrieveKeys = treeResult.keys(0);
                Assert.assertTrue(12 == retrieveKeys[0]);
                Assert.assertTrue(550 == retrieveKeys[1]);
                treeResult.free();
                
                g.disconnect(null);

            }
        });
    }

}
