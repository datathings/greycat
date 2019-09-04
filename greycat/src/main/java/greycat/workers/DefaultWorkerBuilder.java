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
package greycat.workers;

import greycat.GraphBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @ignore ts
 */
public class DefaultWorkerBuilder implements WorkerBuilder {

    protected GraphBuilder graphBuilder;
    protected String name;
    protected byte kind;
    protected Map<String, String> properties;

    protected String logPath, logMaxSize;

    public static DefaultWorkerBuilder newBuilder() {
        return new DefaultWorkerBuilder();
    }

    @Override
    public GraphWorker build() {

        SlaveWorkerStorage workerStorage = new SlaveWorkerStorage();
        GraphBuilder localGraphBuilder = graphBuilder.clone().withStorage(workerStorage);

        GraphWorker worker = new GraphWorker(localGraphBuilder, kind == WorkerAffinity.GENERAL_PURPOSE_WORKER);

        workerStorage.setWorkerMailboxId(worker.mailboxId, worker.callbacksRegistry);

        worker.setName(this.name);
        if (kind == WorkerAffinity.TASK_WORKER) {
            worker.setTaskWorker();
        }

        if(this.logPath != null) {
            worker.withLogDirectory(this.logPath, this.logMaxSize);
        }

        worker.buildGraph();
        setGraphProperties(worker);

        return worker;
    }

    protected void setGraphProperties(GraphWorker worker) {
        if(properties != null) {
            properties.forEach((key, value)->{
                worker.workingGraphInstance.setProperty(key, value);
            });
        }
    }

    public WorkerBuilder withGraphBuilder(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
        return this;
    }

    public WorkerBuilder withLogDirectory(String path, String maxSize) {
        this.logPath = path;
        this.logMaxSize = maxSize;
        return this;
    }

    @Override
    public WorkerBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public WorkerBuilder withKind(byte kind) {
        this.kind = kind;
        return this;
    }

    @Override
    public WorkerBuilder withProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }
}
