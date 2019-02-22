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
package org.kevoree.modeling.addons.rest;

import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.mwg.Constants;
import org.mwg.Graph;
import org.mwg.task.Action;
import org.mwg.task.TaskContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class RestGateway implements HttpHandler {

    private int port;
    private Undertow server;
    private Graph graph;

    public RestGateway(Graph p_graph, int p_port) {
        this.port = p_port;
        this.graph = p_graph;
    }

    @Override
    public void handleRequest(final HttpServerExchange httpServerExchange) throws Exception {
        String rawPath = httpServerExchange.getRelativePath();

        long world = 0;
        long time = System.currentTimeMillis();

        Map<String, Deque<String>> params = httpServerExchange.getQueryParameters();
        if (params.containsKey("world")) {
            try {
                world = Long.parseLong(params.get("world").getFirst());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (params.containsKey("time")) {
            try {
                time = Long.parseLong(params.get("time").getFirst());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        String[] parts = rawPath.split("/");
        try {
            StringBuilder concatQuery = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (concatQuery.length() > 0) {
                    concatQuery.append(".");
                }
                concatQuery.append(parts[i]);
            }
            if (httpServerExchange.getRequestMethod().equalToString("GET")) {
                httpServerExchange.dispatch();
                setWorld(world+"")
                        .setTime(time+"")
                        .parse(concatQuery.toString())
                        .then(new Action() {
                            @Override
                            public void eval(TaskContext context) {
                                Object result = context.result();
                                httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                                Sender sender = httpServerExchange.getResponseSender();
                                if (result != null) {
                                    if (result instanceof Object[]) {
                                        Object[] results = (Object[]) result;
                                        StringBuilder builder = new StringBuilder();
                                        builder.append("[\n");
                                        for (int i = 0; i < results.length; i++) {
                                            if (i != 0) {
                                                builder.append(",\n");
                                            }
                                            builder.append((results[i]).toString());
                                        }
                                        builder.append("\n]\n");
                                        sender.send(builder.toString());
                                        httpServerExchange.endExchange();
                                    } else {
                                        sender.send("[{" + result.toString() + "}]");
                                    }
                                } else {
                                    sender.send("[]");
                                }
                                httpServerExchange.endExchange();
                            }
                        })
                        .execute(graph,null);
            } else {
                //avoid blocking the main HTTP server thread
                if (httpServerExchange.isInIoThread()) {
                    httpServerExchange.dispatch(this);
                    return;
                }
                //read the post payload
                BufferedReader reader = null;
                StringBuilder builder = new StringBuilder();
                try {
                    httpServerExchange.startBlocking();
                    reader = new BufferedReader(new InputStreamReader(httpServerExchange.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                final String body = builder.toString();
                //free the HTTP server
                httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "test/plain");
                httpServerExchange.getResponseSender().send(body.length() + "");
                httpServerExchange.endExchange();

                final Map<String, String> postParam = new HashMap<String, String>();
                int cursor = 0;
                int lastStart = 0;
                while (cursor < body.length()) {
                    if (body.charAt(cursor) == Constants.TASK_SEP) {
                        String p = body.substring(lastStart, cursor).trim();
                        if (!p.equals("")) {
                            String[] pArray = p.split(Constants.QUERY_KV_SEP + "");
                            if (pArray.length > 1) {
                                postParam.put(pArray[0].trim(), pArray[1].trim());
                            }
                        }
                        lastStart = cursor + 1;
                    }
                    cursor++;
                }
                String p = body.substring(lastStart, cursor).trim();
                if (!p.equals("")) {
                    String[] pArray = p.split(Constants.QUERY_KV_SEP + "");
                    if (pArray.length > 1) {
                        postParam.put(pArray[0].trim(), pArray[1].trim());
                    }
                }

                setWorld(world+"")
                        .setTime(time+"")
                        .parse(concatQuery.toString())
                        .then(new Action() {
                            @Override
                            public void eval(TaskContext context) {
                                Object result = context.result();
                                if (result != null) {
                                    if (result instanceof Object[]) {
                                        Object[] castedArray = (Object[]) result;
                                        for (int i = 0; i < castedArray.length; i++) {
                                            if (castedArray[i] instanceof AbstractNode) {
                                                AbstractNode castedNodeLoop = (AbstractNode) castedArray[i];
                                                for (String kk : postParam.keySet()) {
                                                    castedNodeLoop.set(kk, postParam.get(kk));
                                                }
                                            }
                                        }
                                    } else if (result instanceof AbstractNode) {
                                        AbstractNode castedNode = (AbstractNode) result;
                                        for (String kk : postParam.keySet()) {
                                            castedNode.set(kk, postParam.get(kk));
                                        }
                                    }
                                }
                            }
                        })
                        .execute(graph,null);
            }

        } catch (Exception e) {
            e.printStackTrace();
            httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            httpServerExchange.getResponseSender().send("Bad API usage: " + e.getMessage());
        }
    }

    public static RestGateway expose(Graph p_graph, int port) {
        RestGateway newgateway = new RestGateway(p_graph, port);
        return newgateway;
    }

    public void start() {
        server = Undertow.builder().addHttpListener(port, "0.0.0.0").setHandler(this).build();
        server.start();
    }

    public void stop() {
        server.stop();
        server = null;
    }

}
