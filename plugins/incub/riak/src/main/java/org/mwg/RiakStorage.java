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

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.api.cap.Quorum;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.MultiFetch;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.RiakFuture;
import com.basho.riak.client.core.RiakFutureListener;
import com.basho.riak.client.core.query.Location;
import com.basho.riak.client.core.query.Namespace;
import com.basho.riak.client.core.query.RiakObject;
import com.basho.riak.client.core.util.BinaryValue;
import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;
import org.mwg.struct.BufferIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class RiakStorage implements Storage {

    private String[] _urls;
    private RiakClient _client;
    private Namespace _ns;
    private Graph _graph;

    public RiakStorage(String nameSpace, String... urls) {
        this._urls = urls;
        _ns = new Namespace("default", nameSpace);
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        try {
            _graph = graph;
            _client = RiakClient.newClient("localhost:32773", "localhost:32769", "localhost:32771");
            callback.on(true);
        } catch (Exception e) {
            e.printStackTrace();
            callback.on(false);
        }
    }

    @Override
    public void disconnect(Callback<Boolean> callback) {
        _client.shutdown();
        // _client.cleanup();
    }


    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        List<Location> locations = new ArrayList<Location>();
        BufferIterator it = keys.iterator();
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Location location = new Location(_ns, new String(keyView.data()));
            locations.add(location);
        }
        MultiFetch multifetch = new MultiFetch.Builder().addLocations(locations).build();
        try {
            MultiFetch.Response response = _client.execute(multifetch);
            java.util.Map<Location, byte[]> alignedResults = new HashMap<Location, byte[]>();
            List<RiakFuture<FetchValue.Response, Location>> l = response.getResponses();
            for (int i = 0; i < l.size(); i++) {
                FetchValue.Response fetchResponse = l.get(i).get();
                List<RiakObject> objs = fetchResponse.getValues();
                if (!objs.isEmpty()) {
                    alignedResults.put(fetchResponse.getLocation(), objs.get(0).getValue().getValue());
                }
            }
            final Buffer result = _graph.newBuffer();
            for (int i = 0; i < locations.size(); i++) {
                if (i != 0) {
                    result.write(Constants.BUFFER_SEP);
                }
                byte[] resolved = alignedResults.get(locations.get(i));
                if (resolved != null) {
                    result.writeAll(resolved);

                }
            }
            callback.on(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        _client.executeAsync(multifetch).addListener(new RiakFutureListener<MultiFetch.Response, List<Location>>() {
            @Override
            public void handle(RiakFuture<MultiFetch.Response, List<Location>> f) {
                try {

                    System.out.println("BEFORE GET");

                    MultiFetch.Response resp = f.get();
                    java.util.Map<Location, byte[]> alignedResults = new HashMap<Location, byte[]>();
                    List<RiakFuture<FetchValue.Response, Location>> l = resp.getResponses();
                    for (int i = 0; i < l.size(); i++) {
                        FetchValue.Response response = l.get(i).get();
                        List<RiakObject> objs = response.getValues();
                        if (!objs.isEmpty()) {
                            alignedResults.put(response.getLocation(), objs.get(0).getValue().getValue());
                        }
                    }
                    final Buffer result = _graph.newBuffer();
                    for (int i = 0; i < locations.size(); i++) {
                        if (i != 0) {
                            result.write(Constants.BUFFER_SEP);
                        }
                        byte[] resolved = alignedResults.get(locations.get(i));
                        if (resolved != null) {
                            result.writeAll(resolved);

                        }
                    }

                    System.out.println("AFTER GET");

                    callback.on(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.on(null);
                }
            }
        });*/
    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        BufferIterator it = stream.iterator();
        List<StoreValue> all = new ArrayList<StoreValue>();
        while (it.hasNext()) {
            Buffer keyView = it.next();
            Buffer valueView = it.next();
            if (valueView != null) {

                try {
                    Location location = new Location(_ns, new String(keyView.data()));
                    RiakObject riakObject = new RiakObject();
                    riakObject.setValue(BinaryValue.create(valueView.data()));
                    StoreValue store = new StoreValue.Builder(riakObject)
                            .withLocation(location)
                            .withOption(StoreValue.Option.W, new Quorum(3))
                            .build();

                    all.add(store);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        //CountDownLatch latch = new CountDownLatch(all.size());
        final DeferCounter counter = _graph.newCounter(all.size());
        counter.then(() -> {
            callback.on(true);
        });


        for (int i = 0; i < all.size(); i++) {

            /*
            try {
                _client.execute(all.get(i));
                counter.count();
            } catch (Exception e) {
                e.printStackTrace();
            }*/


            _client.executeAsync(all.get(i)).addListener(new RiakFutureListener<StoreValue.Response, Location>() {
                @Override
                public void handle(RiakFuture<StoreValue.Response, Location> f) {
                    //latch.countDown();
                    counter.count();
                }
            });

        }

        /*
        try {
            latch.await();
            callback.on(true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/



    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {
        throw new RuntimeException("Not implemented yet!");
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
            Buffer newBuf = _graph.newBuffer();
            org.mwg.utility.Base64.encodeIntToBuffer(currentPrefix, newBuf);
            callback.on(newBuf);
        }
    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {
        callback.on(true);
    }

}
