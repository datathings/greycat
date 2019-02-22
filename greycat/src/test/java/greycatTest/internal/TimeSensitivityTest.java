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

import greycat.*;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

public class TimeSensitivityTest {

    private Graph graph;

    public TimeSensitivityTest() {
        graph = new GraphBuilder().withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
            }
        });
    }

    @Test
    public void test() {
        Node node = graph.newNode(0, 0);
        node.setTimeSensitivity(10, 0);
        node.set("name", Type.STRING, "myName");

        //traditional resolution
        node.travelInTime(20, new Callback<Node>() {
            @Override
            public void on(Node nodeTime20) {
                Assert.assertEquals(nodeTime20.get("name").toString(), "myName");
            }
        });

        node.travelInTime(5, new Callback<Node>() {
            @Override
            public void on(Node nodeTime5) {
                nodeTime5.set("name", Type.STRING, "pastMutated");
            }
        });

        node.travelInTime(0, new Callback<Node>() {
            @Override
            public void on(Node nodeTime0) {
                Assert.assertEquals(nodeTime0.get("name").toString(), "pastMutated");
            }
        });

        node.travelInTime(20, new Callback<Node>() {
            @Override
            public void on(Node nodeTime20) {
                Assert.assertEquals(nodeTime20.get("name").toString(), "pastMutated");
            }
        });

        node.travelInTime(15, new Callback<Node>() {
            @Override
            public void on(Node nodeTime15) {
                nodeTime15.set("name", Type.STRING, "shouldBe10");
            }
        });

        //chunk 10 should have been created
        node.travelInTime(10, new Callback<Node>() {
            @Override
            public void on(Node nodeTime10) {
                Assert.assertEquals(nodeTime10.get("name").toString(), "shouldBe10");
            }
        });

        //past 0 should not be modified
        node.travelInTime(0, new Callback<Node>() {
            @Override
            public void on(Node nodeTime0) {
                Assert.assertEquals(nodeTime0.get("name").toString(), "pastMutated");
            }
        });

        node.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
            @Override
            public void on(long[] result) {
                Assert.assertEquals(2, result.length);
                Assert.assertEquals(10, result[0]);
                Assert.assertEquals(0, result[1]);
            }
        });

        node.setTimeSensitivity(10, 2);

        node.travelInTime(25, new Callback<Node>() {
            @Override
            public void on(Node nodeTime25) {
                nodeTime25.set("name", Type.STRING, "shouldBe22");
            }
        });

        node.travelInTime(50, new Callback<Node>() {
            @Override
            public void on(Node nodeTime50) {
                Assert.assertEquals(nodeTime50.get("name").toString(), "shouldBe22");
            }
        });

        node.timepoints(Constants.BEGINNING_OF_TIME, Constants.END_OF_TIME, new Callback<long[]>() {
            @Override
            public void on(long[] result) {
                Assert.assertEquals(3, result.length);
                Assert.assertEquals(22, result[0]);
                Assert.assertEquals(10, result[1]);
                Assert.assertEquals(0, result[2]);
            }
        });

    }


}
