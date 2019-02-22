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
package greycat.memory;

import greycat.Graph;
import greycat.GraphBuilder;
import greycat.chunk.ChunkSpace;
import greycat.chunk.ChunkType;
import greycat.chunk.StateChunk;
import greycatTest.internal.chunk.AbstractStateChunkTest;
import greycat.plugin.Plugin;
import greycat.scheduler.NoopScheduler;
import greycat.struct.Buffer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class OffHeapStateChunkTest extends AbstractStateChunkTest {

    public OffHeapStateChunkTest() {
        super(new OffHeapMemoryFactory());
    }

    @After
    public void tearDown() throws Exception {
        if (OffHeapConstants.DEBUG_MODE) {
            Assert.assertEquals(OffHeapConstants.SEGMENTS.size(), 0);
        }
    }

    @Test
    public void loadTest() {
        Graph g = GraphBuilder.newBuilder().withPlugin(new Plugin() {
            @Override
            public void start(Graph graph) {
                graph.setMemoryFactory(factory);
            }

            @Override
            public void stop() {

            }
        }).withScheduler(new NoopScheduler()).build();
        g.connect(null);

        ChunkSpace space = g.space();

        StateChunk chunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 42);
        Buffer buffer = factory.newBuffer();
        buffer.writeAll("K|I|iYxfq|C|M|CJHb/f|E:wFGg:wGGg|M|CJHcG7|E:QFGg:QGGg|i|Cbi4/v|i$M%G%A%BQ%G%E%g%G%C%C%M%G%E:wFGg:wGGg%M%I%E:QFGg:QGGg%k%m%C$K%G%C%C%M%G%E:AAA:AAA%M%I%E:QFGg:QGGg%G%A%BQ%k%k%E$K%G%C%C%M%G%E:QEGg:AAA%M%I%E:QFGg:QFGg%G%A%BQ%k%g%G$K%G%C%C%M%G%E:QEGg:AAA%M%I%E:QFA4:QEGg%G%A%BQ%k%g%I$K%G%C%C%M%G%E:QEGg:AAA%M%I%E:QEMI:QDGg%G%A%BQ%k%g%K$K%G%C%C%M%G%E:QEGg:AAA%M%I%E:QEJU:QCGg%G%A%BQ%k%i%M$K%G%C%C%M%G%E:QEGg:QBGg%M%I%E:QEH6:QCGg%G%A%BQ%k%k%O$K%G%C%C%M%G%E:QEHN:QBGg%M%I%E:QEH6:QCA4%G%A%BQ%k%m%Q$K%G%C%C%M%G%E:QEHjg:QBMI%M%I%E:QEH6:QCA4%G%A%BQ%k%k%S$K%G%C%C%M%G%E:QEHuw:QBMI%M%I%E:QEH6:QBO8%G%A%BQ%k%k%U$K%G%C%C%M%G%E:QEH0Y:QBMI%M%I%E:QEH6:QBNi%G%A%BQ%k%m%W$K%G%C%C%M%G%E:QEH3M:QBM1%M%I%E:QEH6:QBNi%G%A%BQ%k%i%Y$Q%G%C%I%M%G%E:QEH3M:QBNLg%M%I%E:QEH4m:QBNi%G%A%BQ%k%k%e%k%g%g%k%i%a%k%m%c$M%G%C%A%M%G%E:QEH3M:QBNWw%M%I%E:QEH35:QBNi%G%A%I%e%K%m:QAA:QCA:QBA:QEH3we/5+H8:QBNXl0YCQtB:QEH33ukDpBI:QBNXZtBkNG6:QEH30KWJ++9:QBNW+7UT1jF:QEH3xxt1zl2:QBNY7Nu6+lS:AAA:AAA:AAA:AAA:AAA:AAA:AAA:AAA%g%M%W:C:Q:I:EAAAEt2:EAAAEuE:EAAAExk:EAAAEy4:A:A:A:A$M%G%C%A%M%G%E:QEH35:QBNWw%M%I%E:QEH4m:QBNi%G%A%a%e%K%BG:QAA:QDA:QCK:QEH35kMcZft:QBNX9dtl7ES:QEH38ixpCTI:QBNX1bXHI7L:QEH3+53+23O:QBNXWz7pZES:QEH4ADqPzNP:QBNZQwUU/jS:QEH3+j5LL6T:QBNYrR1spE0:QEH4E+gUUO/:QBNXyy2QXAN:QEH357w8W9E:QBNWw6hg3Ln:QEH4HPMoSSR:QBNZNN2MDZ5:QEH4NGDj5HS:QBNZFK/miDu:QEH4EhHxrt1:QBNaMLvGVYZ:QEH4GL+17F1:QBNa6jXj8Vx:QEH4VTnK9TP:QBNZ/ukR88u:QEH4Fg1nInt:QBNYsYcZRh2:AAA:AAA:AAA:AAA:AAA:AAA%g%M%m:C:g:a:EAAAEuS:EAAAEug:EAAAEuu:EAAAEwC:EAAAEwQ:EAAAEws:EAAAExW:EAAAEyA:EAAAEzw:EAAAEz+:EAAAE0M:EAAAE0a:EAAAE1S:A:A:A$M%G%C%A%M%G%E:QEH35:QBNLg%M%I%E:QEH4m:QBNWw%G%A%Q%e%K%m:QAA:QCA:QCA:QEH4AJDPow+:QBNWtfhGROF:QEH3+yz9K/t:QBNV9Z0930v:QEH3515IRWd:QBNVAhktvq9:QEH4CjbrIOa:QBNUap/BrBt:QEH4E+6nJkJ:QBNWXwSsnZw:QEH4COBftqq:QBNVZBRlIbY:QEH4NNffHTQ:QBNWAaa/QYQ:QEH4UMu4PPk:QBNVDyA23c1%g%M%W:C:Q:Q:EAAAEu8:EAAAEvK:EAAAEvY:EAAAEwe:EAAAEw6:EAAAExI:EAAAEzi:EAAAE1g$M%G%C%A%M%G%E:QEH3M:QBNLg%M%I%E:QEH35:QBNWw%G%A%e%e%K%BG:QAA:QDA:QCO:QEH31saLaq0:QBNT62CJUxn:QEH30F/0Ns3:QBNV8i6dLvb:QEH3z5zNFL:QBNVX1/kik6:QEH3p1hMtP4:QBNVIJTCZSj:QEH3wbdadMC:QBNU4o7V8Tj:QEH3izljA+j:QBNVEXye1Dw:QEH3sszmEWi:QBNR/1H+KMJ:QEH3yMfamID:QBNS7WbJU62:QEH3dnXWjDT:QBNQc7d69CM:QEH3oFOaeGL:QBNRQW33in/:QEH3jLxABa6:QBNRpyEd9us:QEH3yRVax9B:QBNQxD2gQie:QEH32WIbFYS:QBNRiGNdgFZ:QEH3zwpt3xL:QBNPwgkA3HI:QEH340ETjDa:QBNO9XAwETo:AAA:AAA%g%M%m:C:g:e:EAAAEvm:EAAAEv0:EAAAExy:EAAAEyO:EAAAEyc:EAAAEyq:EAAAEzG:EAAAEzU:EAAAE0o:EAAAE02:EAAAE1E:EAAAE1u:EAAAE18:EAAAE2K:EAAAE2Y:A|I|DVH0bm|A".getBytes());
        chunk.load(buffer);
        space.free(chunk);
        buffer.free();

        g.disconnect(null);
        
    }


}
