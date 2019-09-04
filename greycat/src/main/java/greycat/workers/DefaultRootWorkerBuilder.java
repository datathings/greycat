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


/**
 * @ignore ts
 */
public class DefaultRootWorkerBuilder extends DefaultWorkerBuilder {

    public static DefaultRootWorkerBuilder newBuilder() {
        return new DefaultRootWorkerBuilder();
    }

    @Override
    public GraphWorker build() {
        GraphWorker worker = new GraphWorker(graphBuilder, false);
        worker.buildGraph();
        worker.workingGraphInstance.storage().listen(worker.notifyGraphUpdate);
        worker.setName(this.name);

        setGraphProperties(worker);

        return worker;
    }
}
