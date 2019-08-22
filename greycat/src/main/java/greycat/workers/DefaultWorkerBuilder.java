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

/**
 * @ignore ts
 */
public class DefaultWorkerBuilder implements WorkerBuilder {

    protected GraphBuilder graphBuilder;
    protected String name;
    protected byte workerKind;

    public static DefaultWorkerBuilder newBuilder() {
        return new DefaultWorkerBuilder();
    }

    @Override
    public GraphWorker build() {

        SlaveWorkerStorage workerStorage = new SlaveWorkerStorage();
        GraphBuilder localGraphBuilder = graphBuilder.clone().withStorage(workerStorage);

        GraphWorker worker = new GraphWorker(localGraphBuilder, workerKind == WorkerAffinity.GENERAL_PURPOSE_WORKER);

        workerStorage.setWorkerMailboxId(worker.mailboxId, worker.callbacksRegistry);

        worker.setName(this.name);
        if (workerKind == WorkerAffinity.TASK_WORKER) {
            worker.setTaskWorker();
        }
        return worker;
    }

    public WorkerBuilder withGraphBuilder(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
        return this;
    }

    @Override
    public WorkerBuilder withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public WorkerBuilder withWorkerKind(byte workerKind) {
        this.workerKind = workerKind;
        return this;
    }
}
