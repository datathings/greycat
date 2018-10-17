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
import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.EStruct;

class RMSProp extends AbstractOptimiser {


    public final static String GRADIENT_CLIP_RATE = "gradientclip";
    public final static double GRADIENT_CLIP_DEF = 5;
    static final String SMOOTH_EPSILON = "smoothepsilon";
    static final double DECAY_RATE_DEF = 0.9999;
    static final double SMOOTH_EPSILON_DEF = 1e-8;
    static final String DECAY_RATE = "decayrate";

    private double smoothEpsilon;
    private double decayRate;
    private double gradientClip;


    public RMSProp(EStruct backend) {
        super(backend);
        smoothEpsilon = backend.getWithDefault(SMOOTH_EPSILON, SMOOTH_EPSILON_DEF);
        decayRate = backend.getWithDefault(DECAY_RATE, DECAY_RATE_DEF);
        gradientClip = backend.getWithDefault(GRADIENT_CLIP_RATE, GRADIENT_CLIP_DEF);
    }


    @Override
    protected void update(Layer[] layers) {
        DMatrix w;
        DMatrix dw;
        DMatrix sc;
        int len;
        double dwi;
        double reg = 1 - learningRate * regularization / steps;

        for (int k = 0; k < layers.length; k++) {
            ExMatrix[] weights = layers[k].getLayerParameters();
            for (int j = 0; j < weights.length; j++) {
                w = weights[j].getW();
                dw = weights[j].getDw();
                sc = weights[j].getStepCache();
                if (sc.length() == 0) {
                    sc.init(w.rows(), w.columns());
                }
                len = w.length();

                for (int i = 0; i < len; i++) {

                    // rmsprop adaptive learning rate
                    dwi = dw.unsafeGet(i) / steps;
                    sc.unsafeSet(i, sc.unsafeGet(i) * decayRate + (1 - decayRate) * dwi * dwi);

                    // gradient clip
                    if (dwi > gradientClip) {
                        dwi = gradientClip;
                    }
                    if (dwi < -gradientClip) {
                        dwi = -gradientClip;
                    }

                    // update (and regularize)
                    w.unsafeSet(i, w.unsafeGet(i) * reg - learningRate * dwi / Math.sqrt(sc.unsafeGet(i) + smoothEpsilon));
                }
                dw.fill(0);
            }
        }
    }

    //param[0] => learning rate
    //param[1] => regularization rate
    //param[2] => smooth Epsilon
    //param[3] => decay Rate
    //param[4] => gradient Clip
    @Override
    public void setParams(double[] params) {
        if (params.length != 5) {
            throw new RuntimeException("Gradient descent needs 5 params: {learning rate, regularization rate, smooth Epsilon, decay Rate, gradient Clip}");
        }
        learningRate = params[0];
        regularization = params[1];
        smoothEpsilon = params[2];
        decayRate = params[3];
        gradientClip = params[4];

        _backend.set(LEARNING_RATE, Type.DOUBLE, learningRate);
        _backend.set(REGULARIZATION_RATE, Type.DOUBLE, regularization);
        _backend.set(SMOOTH_EPSILON, Type.DOUBLE, smoothEpsilon);
        _backend.set(DECAY_RATE, Type.DOUBLE, decayRate);
        _backend.set(GRADIENT_CLIP_RATE, Type.DOUBLE, gradientClip);
    }
}
