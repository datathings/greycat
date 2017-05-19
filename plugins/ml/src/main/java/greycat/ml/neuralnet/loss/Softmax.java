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
package greycat.ml.neuralnet.loss;

import greycat.ml.neuralnet.process.ExMatrix;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.VolatileDMatrix;

//todo need fix on forward and backward
class Softmax implements Loss {


    private static Softmax static_unit= null;

    public static Softmax instance() {
        if (static_unit == null) {
            static_unit = new Softmax();
        }
        return static_unit;
    }

    @Override
    public void backward(ExMatrix logprobs, ExMatrix targetOutput) {
        int targetIndex = getTargetIndex(targetOutput);
        VolatileDMatrix probs = getSoftmaxProbs(logprobs, 1.0);
        int wlen = probs.length();
        for (int i = 0; i < wlen; i++) {
            logprobs.getDw().unsafeSet(i, probs.unsafeGet(i));
        }
        logprobs.getDw().unsafeSet(targetIndex, logprobs.getDw().unsafeGet(targetIndex) - 1);   //logprobs.dw[targetIndex] -= 1;
    }

    @Override
    public DMatrix forward(ExMatrix logprobs, ExMatrix targetOutput) {
        MatrixOps.testDim(logprobs,targetOutput);
        int targetIndex = getTargetIndex(targetOutput);
        VolatileDMatrix probs = getSoftmaxProbs(logprobs, 1.0);
        double cost = -Math.log(probs.unsafeGet(targetIndex)); //cost = -Math.log(probs.w[targetIndex]);
        return null;
    }


    public static VolatileDMatrix getSoftmaxProbs(ExMatrix logprobs, double temperature) {
        VolatileDMatrix probs = VolatileDMatrix.empty(logprobs.rows(), logprobs.columns());
        int loglen = logprobs.length();
        if (temperature != 1.0) {
            for (int i = 0; i < loglen; i++) {
                logprobs.unsafeSet(i, logprobs.unsafeGet(i) / temperature); // logprobs.w[i] /= temperature;
            }
        }
        double maxval = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < loglen; i++) {
            if (logprobs.unsafeGet(i) > maxval) {
                maxval = logprobs.unsafeGet(i);
            }
        }
        double sum = 0;
        for (int i = 0; i < loglen; i++) {
            probs.unsafeSet(i, Math.exp(logprobs.unsafeGet(i) - maxval));  //probs.w[i] = Math.exp(logprobs.w[i] - maxval); //all inputDimensions to exp() are non-positive
            sum += probs.unsafeGet(i); //sum += probs.w[i];
        }
        for (int i = 0; i < loglen; i++) {
            probs.unsafeSet(i, probs.unsafeGet(i) / sum); //probs.w[i] /= sum;
        }
        return probs;
    }

    private static int getTargetIndex(ExMatrix targetOutput) {
        int len = targetOutput.length();
        for (int i = 0; i < len; i++) {
            if (targetOutput.unsafeGet(i) == 1.0) {
                return i;
            }
        }
        throw new RuntimeException("no target index selected");
    }
}
