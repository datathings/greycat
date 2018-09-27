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
package greycat.ml.neuralnet.layer;

import greycat.Type;
import greycat.struct.EStruct;

public class Layers {
    public final static String TYPE = "type";

    public final static int FEED_FORWARD_LAYER = 0;
    public final static int LINEAR_LAYER = 1;
    public final static int GRU_LAYER = 2;
    public final static int LSTM_LAYER = 3;
    public final static int RNN_LAYER = 4;
    public final static int SOFTMAX_LAYER = 5;
    public final static int LINEAR_SOFTMAX_LAYER = 6;
    public final static int DROPOUT_LAYER = 7;

    public static Layer loadLayer(EStruct node) {
        switch ((int) node.get(TYPE)) {
            case FEED_FORWARD_LAYER:
                return new FeedForward(node);
            case LINEAR_LAYER:
                return new Linear(node);
            case GRU_LAYER:
                return new GRU(node);
            case LSTM_LAYER:
                return new LSTM(node);
            case RNN_LAYER:
                return new RNN(node);
            case SOFTMAX_LAYER:
                return new SoftMax(node);
            case LINEAR_SOFTMAX_LAYER:
                return new LinearSoftMax(node);
            case DROPOUT_LAYER:
                return new Dropout(node);
        }
        throw new RuntimeException("Layer type unknown!");
    }


    public static Layer createLayer(EStruct node, int type) {
        node.set(Layers.TYPE, Type.INT, type);
        return loadLayer(node);
    }
}
