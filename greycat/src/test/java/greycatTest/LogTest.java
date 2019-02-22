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
package greycatTest;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class LogTest {

    @Test
    public void consoleTest() {
        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.log().info("hello from {}, mode: {}", "GreyCat", "test");
        g.log().error("hello from {}, mode: {}", "GreyCat", "test");
        g.log().warn("hello from {}, mode: {}", "GreyCat", "test");
        g.log().debug("hello from {}, mode: {}", "GreyCat", "test");
    }

    /**
     * {@native ts
     * }
     */
    @Test
    public void simpleFileTest() {
        Graph g = GraphBuilder.newBuilder()
                .withScheduler(new NoopScheduler())
                .build();
        g.logDirectory("log_out", "10KB");

        g.log().info("=> Say Hi: ", "GreyCat");
        for (int i = 0; i < 100; i++) {
            g.log().info("hello from {}, mode: {}", "GreyCat", "test");
            g.log().error("hello from {}, mode: {}", "GreyCat", "test");
            g.log().warn("hello from {}, mode: {}", "GreyCat", "test");
            g.log().debug("hello from {}, mode: {}", "GreyCat", "test");
        }


        File out_dir = new File("log_out");
        File[] children = out_dir.listFiles();

        Assert.assertNotNull(children);
        Assert.assertEquals(2, children.length);
        for (int i = 0; i < children.length; i++) {
            children[i].delete();
        }
        out_dir.delete();

    }


}
