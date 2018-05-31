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
import greycat.Node;

/**
 * Outlier detector it predicts true or false (anomaly or not, respectively).
 * Like a clusterer, it accepts only a list of values for learning. No label (unlike classfier).
 */
public interface AnomalyDetectionNode extends Node {
    /**
     * Main training function to learn from the the expected output,
     * The input features are defined through features extractions.
     *
     * @param callback Called when the learning is completed with the status of learning true/false
     */
    void learn(Callback<Boolean> callback);

    /**
     * Main infer function to classify the current example
     * The input features are defined through features extractions.
     *
     * @param callback Called when the classification is completed with the integer as the result of the classification
     */
    void classify(Callback<Boolean> callback);
}
