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
package greycatSearchTest;

import greycat.*;
import greycat.search.SearchEngine;
import greycat.search.WaveSearch;

import static greycat.Tasks.newTask;

public class App {

    public static void main(String[] args) {

        Graph g = GraphBuilder.newBuilder().build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //create a grid of 5x5 nodes
                Task initGrid = newTask();
                initGrid.loop("0", "9", newTask().createNode().setAttribute("name", Type.STRING, "room_{{i}}").addToGlobalIndex("rooms", "name"));
                initGrid.execute(g, new Callback<TaskResult>() {
                    @Override
                    public void on(TaskResult result) {
                        result.free();
                        SearchEngine engine = new WaveSearch();
                        //engine.addAction(newTask().glo);
                        //System.out.println("");
                    }
                });


            }
        });

    }

}
