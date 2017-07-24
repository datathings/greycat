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
package greycatTest.bench;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.scheduler.NoopScheduler;

import java.util.ArrayList;
import java.util.List;

public class CloneSpeed {

    public static void main(String[] args) {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);

        Node n = g.newNode(0,0);
        int round = 1000000;
        long before = System.currentTimeMillis();
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(n);
        for(int i=0;i<round;i++){
            nodes.add(g.cloneNode(n));
        }
        for(int i=0;i<round;i++){
            nodes.get(i).free();
        }

        long after = System.currentTimeMillis();
        double time = (after - before) / 1000.0;

        double throughput = round / time;
        System.out.println(time+"/"+throughput);
    }

}
