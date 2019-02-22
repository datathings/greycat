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
package greycat.ml.neuralnet;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.ml.MLPlugin;

public class FlatNeuralNodeTest {

    public static void main(String[] arg) {
        Graph g = new GraphBuilder().withPlugin(new MLPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //Node root = g.newNode(0, 0);
                FlatNeuralNode nn = (FlatNeuralNode) g.newTypedNode(0, 0, FlatNeuralNode.NAME);
                nn.configure(4, 2, 2, 3, 0.01);
                double[] inputs = {0.01, 0.003, -0.5, 0.3};
                double[] outputs = nn.predict(inputs);
                nn.learn(inputs, outputs);
                for (int i = 0; i < outputs.length; i++) {
                    System.out.println(outputs[i]);
                }
            }
        });
    }
}
