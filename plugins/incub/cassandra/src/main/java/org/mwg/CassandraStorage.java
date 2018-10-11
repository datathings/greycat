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
package org.mwg;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;
import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;
import org.mwg.struct.BufferIterator;

import java.util.*;

public class CassandraStorage implements Storage {

    private static ColumnFamily<byte[], Integer> MWG = ColumnFamily.newColumnFamily("mwg_cf", BytesArraySerializer.get(), IntegerSerializer.get());

    private AstyanaxContext<Keyspace> context;
    private Keyspace keyspace;
    private Graph graph;

    public CassandraStorage(String keySpace) {
        context = new AstyanaxContext.Builder()
                .forKeyspace(keySpace)
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                )
                .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
                        .setMaxConnsPerHost(1)
                        .setSeeds("127.0.0.1:9160")
                )
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                .buildKeyspace(ThriftFamilyFactory.getInstance());
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        this.graph = graph;
        context.start();
        keyspace = context.getClient();
       /*
        try {
            keyspace.createColumnFamily(MWG, null);
        } catch (ConnectionException e) {
            e.printStackTrace();
        }*/
        callback.on(true);
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        context.shutdown();
        callback.on(true);
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        try {
            BufferIterator it = keys.iterator();
            final List<byte[]> all_keys = new ArrayList<byte[]>();
            final Map<byte[], byte[]> results = new HashMap<byte[], byte[]>();
            while (it.hasNext()) {
                Buffer keyView = it.next();
                if (keyView != null) {
                    all_keys.add(keyView.data());
                }
            }
            Rows<byte[], Integer> rows = keyspace.prepareQuery(MWG).getKeySlice(all_keys).execute().getResult();
            for (int i = 0; i < rows.size(); i++) {
                Row<byte[], Integer> row = rows.getRowByIndex(i);
                if(row != null){
                    Column col = row.getColumns().getColumnByName(0);
                    if(col != null){
                        results.put(row.getKey(), col.getByteArrayValue());
                    }
                }
            }
            Buffer result = graph.newBuffer();
            for (int i = 0; i < all_keys.size(); i++) {
                if (i != 0) {
                    result.write(Constants.BUFFER_SEP);
                }
                byte[] resolved = results.get(all_keys.get(i));
                if(resolved != null){
                    result.writeAll(resolved);

                }
            }
            callback.on(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        MutationBatch m = keyspace.prepareMutationBatch();
        BufferIterator it = stream.iterator();
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Buffer valueView = it.next();
            if (valueView != null) {
                m.withRow(MWG, keyView.data()).putColumn(0, valueView.data());
            }
        }
        try {
            @SuppressWarnings("unused")
            OperationResult<Void> result = m.execute();
            callback.on(true);
        } catch (ConnectionException e) {
            callback.on(false);
        }
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {

    }

    @Override
    public void lock(Callback<Buffer> callback) {
        byte[] current = null;//db.get(prefixKey);
        if (current == null) {
            current = new String("0").getBytes();
        }
        Short currentPrefix = Short.parseShort(new String(current));
        // db.put(prefixKey, ((currentPrefix + 1) + "").getBytes());
        if (callback != null) {
            Buffer newBuf = graph.newBuffer();
            org.mwg.utility.Base64.encodeIntToBuffer(currentPrefix, newBuf);
            callback.on(newBuf);
        }
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        callback.on(true);
    }


}
