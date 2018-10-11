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
package greycat.chunk;

import greycat.Graph;
import greycat.struct.Buffer;
import greycat.struct.EStructArray;
import greycat.Callback;
import greycat.utility.LMap;

public interface ChunkSpace {

    /**
     * Creates a chunk, out of the space, not marked, used as a factory
     *
     * @param type  the type of chunk {@link ChunkType}
     * @param world the world id of the chunk
     * @param time  the time of the chunk
     * @param id    the id of the chunk
     * @return the created chunk
     */
    Chunk createAndMark(byte type, long world, long time, long id);

    /**
     * Gets and marks a chunk for the given values/keys
     *
     * @param type  the type of chunk {@link ChunkType}
     * @param world the world id of the chunk
     * @param time  the time of the chunk
     * @param id    the id of the chunk
     * @return the retrieved chunk
     */
    Chunk getAndMark(byte type, long world, long time, long id);

    /**
     * Gets and marks a chunk for the given values/keys, if not present in cache, loads it from configured storage
     *
     * @param type     the type of chunk {@link ChunkType}
     * @param world    the world id of the chunk
     * @param time     the time of the chunk
     * @param id       the id of the chunk
     * @param callback method to be called once the chunk is ready for use (retrieved or loaded).
     */
    void getOrLoadAndMark(byte type, long world, long time, long id, Callback<Chunk> callback);

    void getOrLoadAndMarkAll(long[] keys, Callback<Chunk[]> callback);

    /**
     * Retrieves the chunk for the given index
     *
     * @param index index of the chunk
     * @return the chunk
     */
    Chunk get(long index);

    /**
     * Unmarks the chunk at the given index
     *
     * @param index the index of the chunk to unmark
     */
    void unmark(final long index);

    void delete(byte type, long world, long time, long id);

    /**
     * Marks the chunk at the given index
     *
     * @param index the index of the chunk to mark
     * @return the number of marks at this index
     */
    long mark(final long index);

    /**
     * Frees the chunk given in parameter from the memory
     *
     * @param chunk the chunk to be freed
     */
    void free(final Chunk chunk);

    /**
     * Notifies that the chunk at this index has been updated
     *
     * @param index the index of the chunk updated
     */
    void notifyUpdate(long index);

    /**
     * Get current working graph
     *
     * @return current graph
     */
    Graph graph();

    /*
     * Saves the ChunkSpace
     *
     * @param callback the method to be called when save is completed. Parameter indicates status of the save.
     */
    //void save(Callback<Boolean> callback);
    /*
     * Saves the ChunkSpace without notifying storage directly
     *
     * @param callback the method to be called when save is completed. Parameter indicates status of the save.
     */
    //void saveSilent(Callback<Buffer> callback);

    //void savePartial(Callback<Boolean> callback);

    /**
     * Saves the ChunkSpace
     *
     * @param silent   specifies if notification has to be broad-casted or not
     * @param partial  specifies if the batch size configuration should be used
     * @param callback the method to be called when save is completed. Parameter indicates status of the save.
     */
    void save(boolean silent, boolean partial, LMap filter, Callback<Buffer> callback);

    void clear();

    void freeAll();

    long available();

    long dirties();

    /**
     * Create a temporary EStructArray object, not related to the main Graph and without the purpose to be serialized. This object has to be free at end.
     *
     * @return the newly created EStructArray.
     */
    EStructArray newVolatileGraph();

    Interceptor[] interceptors();

    void addInterceptorFirst(Interceptor it);

    void addInterceptorLast(Interceptor it);

    void printMarked();

}
