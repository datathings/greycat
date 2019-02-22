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
package greycatTest;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.scheduler.NoopScheduler;
import org.junit.Assert;
import org.junit.Test;

public class ConnectHookTest {

    @Test
    public final void test() {

        final int[] onStartCall = new int[1];
        onStartCall[0] = 0;

        Graph g = GraphBuilder.newBuilder().withScheduler(new NoopScheduler()).build();
        g.addConnectHook(new Callback<Callback<Boolean>>() {
            @Override
            public void on(Callback<Boolean> result) {
                onStartCall[0]++;
                result.on(true);
            }
        });
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                onStartCall[0]++;
            }
        });
        Assert.assertEquals(onStartCall[0],2);
    }

}
