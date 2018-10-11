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

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Node;
import greycat.chunk.Chunk;
import greycat.chunk.Interceptor;
import greycat.scheduler.NoopScheduler;
import org.junit.Test;

public class InterceptorTest {

    //@Test
    public void intercept_chunk_read() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.connect(null);

        g.space().addInterceptorFirst(new Interceptor() {
            @Override
            public boolean preChunkRead(byte type, long world, long time, long id) {
                System.out.println("READ="+type+"|"+world+"|"+time+"|"+id);
                return true;
            }

            @Override
            public boolean preChunkCreate(byte type, long world, long time, long id) {
                System.out.println("CREATED="+type+"|"+world+"|"+time+"|"+id);
                return true;
            }

            @Override
            public boolean preAttSet(Chunk chunk, int index) {
                return true;
            }

            @Override
            public boolean postAttSet(Chunk chunk, int index) {
                return true;
            }
        });

        Node n = g.newNode(0, 0);


    }

}
