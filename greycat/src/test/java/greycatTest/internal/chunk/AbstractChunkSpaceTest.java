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
package greycatTest.internal.chunk;

import greycat.chunk.*;
import greycat.plugin.MemoryFactory;
import org.junit.Test;

public abstract class AbstractChunkSpaceTest {

    private MemoryFactory factory;

    public AbstractChunkSpaceTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void globalTest() {
        
        ChunkSpace space = factory.newSpace(100,-1, null, false);

        StateChunk stateChunk = (StateChunk) space.createAndMark(ChunkType.STATE_CHUNK, 0, 0, 0);

        WorldOrderChunk worldOrderChunk = (WorldOrderChunk) space.createAndMark(ChunkType.WORLD_ORDER_CHUNK, 0, 0, 1);

        TimeTreeChunk timeTreeChunk = (TimeTreeChunk) space.createAndMark(ChunkType.TIME_TREE_CHUNK, 0, 0, 2);

        GenChunk genChunk = (GenChunk) space.createAndMark(ChunkType.GEN_CHUNK, 1, 1, 1);

        space.free(stateChunk);
        space.free(worldOrderChunk);
        space.free(timeTreeChunk);
        space.free(genChunk);

        space.freeAll();

    }

}
