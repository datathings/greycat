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

import com.netflix.dyno.jedis.DynoJedisClient;
import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;

public class DynoClient implements Storage {

    private DynoJedisClient jClient;
    private final String name;
    private final String clusterName;

    public DynoClient(String name, String clusterName) {
        this.name = name;
        this.clusterName = clusterName;
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        /*
        DynoJedisClient jClient = new DynoJedisClient.Builder()
                .withApplicationName(name)
                .withDynomiteClusterName(clusterName)
                .withDiscoveryClient(discoveryClient)
                .build();
                */
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {

    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {
        //jClient.set();
    }

    @Override
    public void remove(Buffer keys, Callback<Boolean> callback) {

    }


    @Override
    public void lock(Callback<Buffer> callback) {

    }

    @Override
    public void unlock(Buffer previousLock, Callback<Boolean> callback) {

    }

    @Override
    public void disconnect(Callback<Boolean> callback) {

    }
}
