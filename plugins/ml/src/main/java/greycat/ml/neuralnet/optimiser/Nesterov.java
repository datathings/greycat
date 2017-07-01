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
package greycat.ml.neuralnet.optimiser;

import greycat.Type;
import greycat.ml.neuralnet.layer.Layer;
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;
import greycat.struct.matrix.MatrixOps;

class Nesterov extends AbstractOptimiser {

    static final String DECAY_RATE = "decayrate";
    static final double DECAY_RATE_DEF = 0.9;
    double decayRate;
    Nesterov(EStruct backend) {
        super(backend);
        decayRate = backend.getWithDefault(DECAY_RATE, DECAY_RATE_DEF);
    }

    //param[0] => learning rate
    //param[1] => regularization rate
    //param[3] => decay Rate
    @Override
    public void setParams(double[] params) {
        if (params.length != 3) {
            throw new RuntimeException("Gradient descent needs 3 params: {learning rate, regularization rate, decay Rate}");
        }
        learningRate = params[0];
        regularization = params[1];
        decayRate = params[2];

        _backend.set(LEARNING_RATE, Type.DOUBLE, learningRate);
        _backend.set(REGULARIZATION_RATE, Type.DOUBLE, regularization);
        _backend.set(DECAY_RATE, Type.DOUBLE, decayRate);
    }

    //w= reg * w + decay*decay*sc -(1+ decay)*learning * dw
    //sc = sc*decay -learning *dw
    @Override
    protected void update(Layer[] layers) {
        DMatrix w;
        DMatrix dw;
        DMatrix sc;

        double reg = 1 - learningRate * regularization / steps;
        double stepsize = learningRate / steps;
        double rate1 = decayRate * decayRate;
        double rate2 = -(1 + decayRate) * stepsize;

        for (int i = 0; i < layers.length; i++) {
            ExMatrix[] weights = layers[i].getLayerParameters();
            for (int j = 0; j < weights.length; j++) {
                w = weights[j].getW();
                dw = weights[j].getDw();
                sc = weights[j].getStepCache();
                MatrixOps.addInPlace(w, reg, sc, rate1);
                MatrixOps.addInPlace(w, 1, dw, rate2);
                MatrixOps.addInPlace(sc, decayRate, dw, -stepsize);
                //w= (1- learningRate * regularization / samples ) * w - learningRate * dw / samples ;
                //Ref: https://www.coursera.org/learn/machine-learning/lecture/QrMXd/regularized-linear-regression
                dw.fill(0);
            }
        }
    }
}
