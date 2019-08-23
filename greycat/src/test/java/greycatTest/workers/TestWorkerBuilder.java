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
package greycatTest.workers;

import greycat.GraphBuilder;
import greycat.workers.*;

/**
 * @ignore ts
 */
public class TestWorkerBuilder extends DefaultWorkerBuilder {


    public static TestWorkerBuilder newBuilder() {
        return new TestWorkerBuilder();
    }

    @Override
    public GraphWorker build() {

        SlaveWorkerStorage workerStorage = new SlaveWorkerStorage();
        GraphBuilder localGraphBuilder = graphBuilder.clone().withStorage(workerStorage);

        TestGraphWorker worker = new TestGraphWorker(localGraphBuilder, kind == WorkerAffinity.GENERAL_PURPOSE_WORKER);
        workerStorage.setWorkerMailboxId(worker.getMailboxId(), worker.getCallbackRegistry());

        if (kind == WorkerAffinity.TASK_WORKER) {
            worker.setTaskWorker();
        }

        worker.buildGraph();
        setGraphProperties(worker);

        return worker;
    }

}
