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
package greycat.internal.heap;

import greycat.Graph;
import greycat.chunk.ChunkSpace;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;

public class HeapMemoryFactory implements MemoryFactory {

    @Override
    public final ChunkSpace newSpace(final long memorySize, final long batchSize, final Graph graph, final boolean deepWorld) {
        return new HeapChunkSpace((int) memorySize, (int) batchSize, graph, deepWorld);
    }

    @Override
    public final Buffer newBuffer() {
        return new HeapBuffer();
    }
}
