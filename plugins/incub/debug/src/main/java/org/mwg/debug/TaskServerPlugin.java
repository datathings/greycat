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
package org.mwg.debug;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.mwg.plugin.AbstractPlugin;
import org.mwg.task.TaskHook;
import org.mwg.task.TaskHookFactory;

import java.util.Set;

public class TaskServerPlugin extends AbstractPlugin implements HttpHandler {

    private int port;
    private Undertow server;
    private TaskTraceRegistry registry;

    public TaskServerPlugin(int p_port) {
        this.registry = new TaskTraceRegistry();
        this.port = p_port;
        declareTaskHookFactory(new TaskHookFactory() {
            @Override
            public TaskHook newHook() {
                return new JsonHook(registry);
            }
        });
        server = Undertow.builder().addHttpListener(port, "0.0.0.0").setHandler(this).build();
        server.start();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        httpServerExchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, "application/json")
                .put(new HttpString("Access-Control-Allow-Origin"), "*");

        String path = httpServerExchange.getRequestPath();
        if (path.startsWith("/task/")) {
            try {
                int id = Integer.parseInt(path.substring(6));
                String result = registry.tasks.get(id);
                if (result != null) {
                    httpServerExchange.getResponseSender().send(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (path.startsWith("/context/")) {
            try {
                int id = Integer.parseInt(path.substring(9));
                String result = registry.contexts.get(id);
                if (result != null) {
                    httpServerExchange.getResponseSender().send(result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (path.equals("/task")) {
            StringBuilder jsonOut = new StringBuilder();
            jsonOut.append("[");
            Set<Integer> keys = registry.tasks.keySet();
            boolean isFirst = true;
            for (Integer i : keys) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    jsonOut.append(",");
                }
                jsonOut.append(i);
            }
            jsonOut.append("]");
            httpServerExchange.getResponseSender().send(jsonOut.toString());
        } else {

        }
        httpServerExchange.endExchange();
    }
}
