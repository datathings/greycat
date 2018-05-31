/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat.ml;

import greycat.Callback;
import greycat.Graph;
import greycat.Type;

public class NoopRegressionNode extends BaseMLNode implements RegressionNode {

    public static final String NAME = "NoopRegressionNode";

    public NoopRegressionNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    @Override
    public void learn(double output, final Callback<Boolean> callback) {
        extractFeatures(new Callback<double[]>() {
            @Override
            public void on(double[] result) {
                set("extracted", Type.DOUBLE_ARRAY, result);
                if (callback != null) {
                    callback.on(true);
                }
            }
        });


    }

    @Override
    public void extrapolate(Callback<Double> callback) {

    }

}
