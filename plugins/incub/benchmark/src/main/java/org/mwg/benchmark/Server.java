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
package org.mwg.benchmark;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

import java.io.File;
import java.io.FileReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server implements HttpHandler {

    public static void main(String[] args) {
        File dataDir;
        if (System.getProperty("data") != null) {
            dataDir = new File(System.getProperty("data"));
        } else {
            dataDir = new File("/Users/duke/dev/mwDB/plugins/benchmark/data");
        }
        final Server srv = new Server();
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    JsonArray runs = new JsonArray();
                    File[] files = dataDir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        try {
                            FileReader read = new FileReader(files[i]);
                            JsonObject run = (JsonObject) Json.parse(read);
                            runs.add(run);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    srv.root.set("runs", runs);
                    JsonObject charts = new JsonObject();
                    for (int i = 0; i < runs.size(); i++) {
                        JsonObject run = (JsonObject) runs.get(i);
                        JsonArray benchmarks = (JsonArray) run.get("benchmarks");
                        for (int j = 0; j < benchmarks.size(); j++) {
                            JsonObject benchmark = (JsonObject) benchmarks.get(j);
                            String benchmarkName = benchmark.getString("benchmark", "defaultBenchmark");
                            JsonObject chart = (JsonObject) charts.get(benchmarkName);
                            if (chart == null) {
                                chart = new JsonObject();
                                charts.set(benchmarkName, chart);
                            }
                            JsonArray metrics = (JsonArray) benchmark.get("metrics");
                            for (int k = 0; k < metrics.size(); k++) {
                                JsonObject metric = (JsonObject) metrics.get(k);
                                String metricName = metric.getString("name", "defaultMetric");
                                JsonArray chartLine = (JsonArray) chart.get(metricName);
                                if (chartLine == null) {
                                    chartLine = new JsonArray();
                                    chart.set(metricName, chartLine);
                                }
                                chartLine.add(new JsonObject().set("time", run.get("time")).set("value", metric.get("value")));
                            }
                        }
                    }
                    srv.root.set("charts", charts);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    final int port = 9077;
    final JsonObject root = new JsonObject();

    public Server() {
        Undertow server = Undertow.builder().addHttpListener(port, "0.0.0.0",
                Handlers.path()
                        .addPrefixPath("rpc", this)
                        .addPrefixPath("/", new ResourceHandler(new ClassPathResourceManager(Server.class.getClassLoader(), "static")).addWelcomeFiles("index.html").setDirectoryListingEnabled(false))
        ).build();
        server.start();
        System.out.println("Server running at : 9077");
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        if (httpServerExchange.getRequestPath().equals("/rpc/charts")) {
            httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
            httpServerExchange.setStatusCode(StatusCodes.OK);
            httpServerExchange.getResponseSender().send(root.get("charts").toString());
        }
    }

}
