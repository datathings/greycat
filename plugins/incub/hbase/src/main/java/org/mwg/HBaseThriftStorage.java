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
package org.mwg;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.thrift.generated.Hbase;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;
import org.mwg.struct.BufferIterator;
import org.mwg.utility.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class HBaseThriftStorage implements Storage {
    private static final String _connectedError = "PLEASE CONNECT YOUR DATABASE FIRST";
    private static final byte[] prefixKey = "prefix".getBytes();

    private static final String COLUMN_FAMILY = "data";
    private static final String COLUMN_NAME = "value";

    private String tablename;

    private Connection connection;
    private Table table;
    private boolean isConnected;
    private Graph graph;

    public HBaseThriftStorage(String tablename) {
        this.tablename = tablename;
    }


    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        final Buffer result = graph.newBuffer();
        final BufferIterator it = keys.iterator();

        boolean isFirst = true;
        final List<Get> gets = new ArrayList<>();
        while (it.hasNext()) {
            final Buffer view = it.next();
            if (!isFirst) {
                result.write(Constants.BUFFER_SEP);
            } else {
                isFirst = false;
            }
            final Get get = new Get(view.data());
            gets.add(get);
        }

        try {
            Result[] res = table.get(gets);
            if (res != null) {
                for (int i = 0; i < res.length; i++) {
                    byte[] r = res[i].getValue(Bytes.toBytes(COLUMN_FAMILY), Bytes.toBytes(COLUMN_NAME));
                    if (r != null) {
                        result.writeAll(r);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (callback != null) {
            callback.on(result);
        }
    }


    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        try {
            final List<Put> puts = new ArrayList<>();

            final BufferIterator it = stream.iterator();
            while (it.hasNext()) {
                final Buffer keyView = it.next();
                final Buffer valueView = it.next();
                if (valueView != null) {
                    final Put put = new Put(keyView.data());
                    put.addColumn(Bytes.toBytes(COLUMN_FAMILY), Bytes.toBytes(COLUMN_NAME), valueView.data());
                    puts.add(put);
                }
            }
            table.put(puts);
            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {

    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        if (isConnected) {
            if (callback != null) {
                callback.on(null);
            }
            return;
        }
        this.graph = graph;

        // Create a connection to the cluster.
        try {
            TTransport transport = new TSocket("192.168.25.60", 2280);
            TProtocol protocol = new TBinaryProtocol(transport, true, true);

            Hbase.Client client = new Hbase.Client(protocol);

            transport.open();

            final Li
            new HColumnDescriptor(COLUMN_FAMILY);
            byte[] tablename = Bytes.toBytes(this.tablename);

            client.createTable(ByteBuffer.wrap(tablename), );

//            Configuration conf = HBaseConfiguration.create();
//            conf.set("hbase.zookeeper.quorum", "192.168.25.60");
//            conf.set("hbase.zookeeper.quorum", "localhost");
//            conf.set("hbase.zookeeper.property.clientPort", "2280");
//            conf.set("hbase.zookeeper.property.clientPort", "2181");
//            conf.set("zookeeper.znode.parent", "/hbase");

//            connection = ConnectionFactory.createConnection(conf);
//            HBaseAdmin.checkHBaseAvailable(conf);
//
//            Admin admin = connection.getAdmin();


//            if (!admin.tableExists(TableName.valueOf(tablename))) {
//                HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tablename));
//                tableDescriptor.addFamily(new HColumnDescriptor(COLUMN_FAMILY));
//                admin.createTable(tableDescriptor);
//            }
//            table = connection.getTable(TableName.valueOf(tablename));

            isConnected = true;
            if (callback != null) {
                callback.on(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(null);
            }
        }


    }

    @Override
    public void lock(Callback<Buffer> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }

        try {
            final Result result = table.get(new Get(prefixKey));
            byte[] current = result.getValue(Bytes.toBytes(COLUMN_FAMILY), Bytes.toBytes(COLUMN_NAME));
            if (current == null) {
                current = new String("0").getBytes();
            }
            final Short currentPrefix = Short.parseShort(new String(current));
            final Put put = new Put(prefixKey);
            put.addColumn(Bytes.toBytes(COLUMN_FAMILY), Bytes.toBytes(COLUMN_NAME), ((currentPrefix + 1) + "").getBytes());
            table.put(put);

            if (callback != null) {
                Buffer newBuf = graph.newBuffer();
                Base64.encodeIntToBuffer(currentPrefix, newBuf);
                callback.on(newBuf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        if (!isConnected) {
            throw new RuntimeException(_connectedError);
        }
        callback.on(true);
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        try {
            connection.close();
            connection = null;
            isConnected = false;

            if (callback != null) {
                callback.on(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.on(false);
            }
        }
    }
}
