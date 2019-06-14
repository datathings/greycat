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
package greycat.websocket;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.websocket.WSClient;

/**
 * Created by Gregory NAIN on 2019-06-14.
 */
public class ConnectTest {


    public static void main(String[] args) {


        GraphBuilder builder = GraphBuilder
                .newBuilder()
                .withMemorySize(1000000)
                .withStorage(new WSClient("wss://rainsat.datathings.com/ws", "rainsat", "rainsat2019"));

        Graph graph = builder.build();

        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean aBoolean) {

                System.out.println();


            }
        });

    }


}
