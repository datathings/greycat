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
package greycat.internal.heap;

import greycat.Container;
import greycat.Index;
import greycat.Type;
import greycat.chunk.StateChunk;
import greycat.plugin.NodeStateCallback;
import greycat.struct.*;

public class MockNodeStateDValue implements StateChunk {

    private HeapTimeTreeDValueChunk chunk;
    private int offset;

    MockNodeStateDValue(HeapTimeTreeDValueChunk p_chunk, int p_offset) {
        chunk = p_chunk;
        offset = p_offset;
    }

    @Override
    public final long world() {
        return chunk.world();
    }

    @Override
    public final long time() {
        return chunk._k[offset];
    }

    @Override
    public long id() {
        return chunk.id();
    }

    @Override
    public byte chunkType() {
        return -1;
    }

    @Override
    public long index() {
        return -1;
    }

    @Override
    public void save(Buffer buffer) {

    }

    @Override
    public void saveDiff(Buffer buffer) {

    }

    @Override
    public void load(Buffer buffer) {

    }

    @Override
    public void loadDiff(Buffer buffer) {

    }

    @Override
    public long hash() {
        return 0;
    }

    @Override
    public boolean inSync() {
        return false;
    }

    @Override
    public boolean sync(long remoteHash) {
        return false;
    }

    @Override
    public final void each(NodeStateCallback callBack) {
        callBack.on(offset, Type.DOUBLE, chunk._values[offset]);
    }

    @Override
    public final Object get(String name) {
        return chunk._values[offset];
    }

    @Override
    public final Relation getRelation(String name) {
        return null;
    }

    @Override
    public final Index getIndex(String name) {
        return null;
    }

    @Override
    public final DMatrix getDMatrix(String name) {
        return null;
    }

    @Override
    public final LMatrix getLMatrix(String name) {
        return null;
    }

    @Override
    public final EGraph getEGraph(String name) {
        return null;
    }

    @Override
    public final LongArray getLongArray(String name) {
        return null;
    }

    @Override
    public final IntArray getIntArray(String name) {
        return null;
    }

    @Override
    public final DoubleArray getDoubleArray(String name) {
        return null;
    }

    @Override
    public final StringArray getStringArray(String name) {
        return null;
    }

    @Override
    public final StringIntMap getStringIntMap(String name) {
        return null;
    }

    @Override
    public final LongLongMap getLongLongMap(String name) {
        return null;
    }

    @Override
    public final IntIntMap getIntIntMap(String name) {
        return null;
    }

    @Override
    public final IntStringMap getIntStringMap(String name) {
        return null;
    }

    @Override
    public final LongLongArrayMap getLongLongArrayMap(String name) {
        return null;
    }

    @Override
    public LongSet getLongSet(String name) {
        return null;
    }

    @Override
    public IntSet getIntSet(String name) {
        return null;
    }

    @Override
    public final Object getAt(int index) {
        return chunk._values[offset];
    }

    @Override
    public final Object getRawAt(int index) {
        return chunk._values[offset];
    }

    @Override
    public final Object getTypedRawAt(int index, int type) {
        return chunk._values[offset];
    }

    @Override
    public final int type(String name) {
        return Type.DOUBLE;
    }

    @Override
    public final int typeAt(int index) {
        return Type.DOUBLE;
    }

    @Override
    public final Container set(String name, int type, Object value) {
        chunk._values[offset] = (double) value;
        return this;
    }

    @Override
    public final Container setAt(int index, int type, Object value) {
        chunk._values[offset] = (double) value;
        return this;
    }

    @Override
    public final Container remove(String name) {
        return this;
    }

    @Override
    public final Container removeAt(int index) {
        return this;
    }

    @Override
    public final Object getOrCreate(String name, int type) {
        return null;
    }

    @Override
    public final Object getOrCreateAt(int index, int type) {
        return null;
    }

    @Override
    public final Container rephase() {
        return this;
    }

    @Override
    public <A> A getWithDefault(String key, A defaultValue) {
        return null;
    }

    @Override
    public <A> A getAtWithDefault(int key, A defaultValue) {
        return null;
    }

    @Override
    public void loadFrom(StateChunk origin) {

    }
}
