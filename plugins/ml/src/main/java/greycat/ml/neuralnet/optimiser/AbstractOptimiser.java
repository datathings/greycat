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
package greycat.ml.neuralnet.optimiser;

import greycat.Type;
import greycat.ml.neuralnet.layer.Layer;
import greycat.struct.EStruct;


// The abstract class here manages all three cases between full online learning,
// mini-batch, and full batch in an elegant way. All that is needed to be done is
// to implement the update method and take into account the steps that
// have passed since the last update

abstract class AbstractOptimiser implements Optimiser {

    protected static final String LEARNING_RATE = "learningrate";
    protected static final String REGULARIZATION_RATE = "regularizationrate";
    private static final double LEARNING_RATE_DEF = 0.001;
    private static final double REGULARIZATION_RATE_DEF = 0.000001;
    private static final String STEPS = "steps";
    private static final String MAX_STEPS = "max_steps";
    private static final String BATCH_SIZE = "batch_size";
    protected double learningRate;
    protected double regularization;
    protected EStruct _backend;
    int steps;
    private int batchSize;
    private int maxSteps;


    AbstractOptimiser(EStruct backend) {
        this._backend = backend;
        learningRate = backend.getWithDefault(LEARNING_RATE, LEARNING_RATE_DEF);
        regularization = backend.getWithDefault(REGULARIZATION_RATE, REGULARIZATION_RATE_DEF);
        steps = backend.getWithDefault(STEPS, 0);
        maxSteps = backend.getWithDefault(MAX_STEPS, 1);
        batchSize = backend.getWithDefault(BATCH_SIZE, 1);
    }


    @Override
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        _backend.set(BATCH_SIZE, Type.INT, batchSize);
    }


    @Override
    public void setFrequency(int maxSteps) {
        this.maxSteps = maxSteps;
        _backend.set(MAX_STEPS, Type.INT, maxSteps);
    }


    @Override
    public void stepUpdate(Layer[] layers) {
        steps += batchSize;
        if (maxSteps > 0 && steps >= maxSteps) {
            update(layers);
            steps = 0;
        }
        _backend.set(STEPS, Type.INT, steps);
    }

    @Override
    public void finalUpdate(Layer[] layers) {
        if (maxSteps <= 0 || steps > 0) {
            update(layers);
            steps = 0;
            _backend.set(STEPS, Type.INT, steps);
        }
    }

    @Override
    public abstract void setParams(double[] params);

    protected abstract void update(Layer[] layers);

}
