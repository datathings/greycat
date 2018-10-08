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
package greycatTest.internal;

import greycat.*;
import greycat.chunk.ChunkSpace;
import greycat.internal.CoreQuery;
import greycat.internal.heap.HeapMemoryFactory;
import greycat.plugin.*;
import greycat.struct.Buffer;
import greycat.TaskHook;
import greycat.utility.HashHelper;
import greycat.utility.Tuple;
import org.junit.Assert;
import org.junit.Test;


public class QueryTest implements Resolver, Graph {

    @Test
    public void test() {


        Query query = this.newQuery();
        query.add("name", "Hello");
        //64 bits version/
        Assert.assertEquals(query.hash(), 8429737982204714L);
        //Assert.assertEquals(query.hash(), 3074775135214424L);

        Query query2 = this.newQuery();
        query2.add("id", "Hello");
        Assert.assertEquals(query2.hash(), -4949475811985026L);

        Query query3 = this.newQuery();
        query3.add("id", "Hello2");
        Assert.assertEquals(query3.hash(), 5227124363167243L);

        Assert.assertTrue(query3.hash() != query.hash());
        Assert.assertTrue(query2.hash() != query.hash());
        Assert.assertTrue(query3.hash() != query2.hash());

        Query query4 = this.newQuery();
        query4.add("id", "Hello2");
        Assert.assertEquals(query4.hash(), 5227124363167243L);
        Assert.assertEquals(query3.hash(), query4.hash());

    }


    @Override
    public void init() {

    }

    @Override
    public void free() {

    }

    @Override
    public void initNode(Node node, long typeCode) {

    }

    @Override
    public void initWorld(long parentWorld, long childWorld) {

    }

    @Override
    public void freeNode(Node node) {

    }

    @Override
    public int typeCode(Node node) {
        return 0;
    }

    @Override
    public void end(Node node) {

    }

    @Override
    public Graph setProperty(String key, Object value) {
        return null;
    }

    @Override
    public Object getProperty(String key) {
        return null;
    }

    @Override
    public Node newNode(long world, long time) {
        return null;
    }

    @Override
    public Node newTypedNode(long world, long time, String nodeType) {
        return null;
    }

    @Override
    public Node newTypedNodeFrom(long world, long time, int nodeType) {
        return null;
    }

    /**
     * @ignore ts
     */
    @Override
    public <A extends Node> A newTypedNode(long world, long time, String nodeType, Class<A> type) {
        return null;
    }

    @Override
    public Node cloneNode(Node origin) {
        return null;
    }

    @Override
    public <A extends Node> void lookup(long world, long time, long id, Callback<A> callback) {

    }

    @Override
    public void lookupBatch(long[] worlds, long[] times, long[] ids, Callback<Node[]> callback) {

    }

    @Override
    public void lookupPTimes(long world, long[] times, long id, Callback<Node[]> callback) {

    }


    @Override
    public void lookupAll(long world, long time, long[] ids, Callback<Node[]> callback) {

    }

    @Override
    public void lookupTimes(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback) {

    }

    @Override
    public void lookupTimesSparse(long world, long rfrom, long rto, long id, int limit, Callback<Node[]> callback) {

    }

    @Override
    public long fork(long world) {
        return 0;
    }

    @Override
    public void savePartial(Callback<Boolean> callback) {

    }

    @Override
    public void save(Callback<Boolean> callback) {

    }

    @Override
    public void saveSilent(Callback<Buffer> callback) {

    }

    @Override
    public void savePartialSilent(Callback<Buffer> callback) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void connect(Callback<Boolean> callback) {

    }

    @Override
    public void disconnect(Callback<Boolean> callback) {

    }

    @Override
    public void declareIndex(long world, String name, Callback<NodeIndex> callback, String... indexedAttributes) {

    }

    @Override
    public void declareTimedIndex(long world, long time, String name, Callback<NodeIndex> callback, String... indexedAttributes) {

    }

    @Override
    public void index(long world, long time, String name, Callback<NodeIndex> callback) {

    }

    @Override
    public void indexNames(long world, long time, Callback<String[]> callback) {

    }

    @Override
    public DeferCounter newCounter(int expectedEventsCount) {
        return null;
    }

    @Override
    public DeferCounterSync newSyncCounter(int expectedEventsCount) {
        return null;
    }

    @Override
    public Resolver resolver() {
        return null;
    }

    @Override
    public Scheduler scheduler() {
        return null;
    }

    @Override
    public ChunkSpace space() {
        return null;
    }

    @Override
    public Storage storage() {
        return null;
    }

    private HeapMemoryFactory factory = new HeapMemoryFactory();

    @Override
    public Buffer newBuffer() {
        return factory.newBuffer();
    }

    @Override
    public Query newQuery() {
        return new CoreQuery(this, this);
    }

    @Override
    public void freeNodes(Node[] nodes) {

    }

    @Override
    public TaskHook[] taskHooks() {
        return new TaskHook[0];
    }

    @Override
    public ActionRegistry actionRegistry() {
        return null;
    }

    @Override
    public NodeRegistry nodeRegistry() {
        return null;
    }

    @Override
    public TypeRegistry typeRegistry() {
        return null;
    }

    @Override
    public TaskContextRegistry taskContextRegistry() {
        return null;
    }

    @Override
    public Graph setMemoryFactory(MemoryFactory factory) {
        return null;
    }

    @Override
    public Graph addGlobalTaskHook(TaskHook taskHook) {
        return null;
    }

    @Override
    public Graph addConnectHook(Callback<Callback<Boolean>> onConnect) {
        return null;
    }

    @Override
    public void remoteNotify(Buffer buffer) {
        //noop
    }

    @Override
    public Log log() {
        return null;
    }

    @Override
    public NodeState resolveState(Node node) {
        return null;
    }

    @Override
    public NodeState alignState(Node node) {
        return null;
    }

    @Override
    public NodeState newState(Node node, long world, long time) {
        return null;
    }

    @Override
    public void resolveTimepoints(Node node, long beginningOfSearch, long endOfSearch, Callback<long[]> callback) {

    }

    @Override
    public void countTimepoints(Node node, long beginningOfSearch, long endOfSearch, Callback<Long> callback) {

    }

    @Override
    public int stringToHash(String name, boolean insertIfNotExists) {
        return HashHelper.hash(name);
    }

    @Override
    public String hashToString(int key) {
        return null;
    }

    @Override
    public void externalLock(Node node) {

    }

    @Override
    public void externalUnlock(Node node) {

    }

    @Override
    public void setTimeSensitivity(Node node, long deltaTime, long delta) {

    }

    @Override
    public Tuple<Long, Long> getTimeSensitivity(Node node) {
        return new Tuple<Long, Long>(0L, 0L);
    }

    @Override
    public void drop(Node target, Callback callback) {

    }

    @Override
    public void batchInsert(Node target, long[] times, double[] values) {

    }

}
