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

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.mwg.plugin.Storage;
import org.mwg.struct.Buffer;

import java.util.concurrent.TimeUnit;

public class NatsClient implements Storage {

    private final ConnectionFactory cf;
    private Connection nc;

    public NatsClient(String... servers) {
        cf = new ConnectionFactory();
        cf.setServers(servers);
    }

    @Override
    public void connect(Graph graph, Callback<Boolean> callback) {
        try {
            nc = cf.createConnection();
            callback.on(true);
        } catch (Exception e) {
            e.printStackTrace();
            callback.on(false);
        }
    }

    @Override
    public void get(Buffer keys, Callback<Buffer> callback) {
        /*
        nc.request("","", 4, TimeUnit.DAYS, new MessageHandler(){

            @Override
            public void onMessage(Message message) {

            }
        });*/
    }

    @Override
    public void put(Buffer stream, Callback<Boolean> callback) {

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
