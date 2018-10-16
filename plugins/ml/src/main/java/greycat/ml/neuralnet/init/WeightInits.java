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
package greycat.ml.neuralnet.init;

public class WeightInits {
//   Follow  https://deeplearning4j.org/api/latest/org/deeplearning4j/nn/weights/WeightInit.html for doc

    public final static int DISTRIBUTION = 0;
    public final static int ZERO = 1;
    public final static int ONE = 2;
    public final static int SIGMOID_UNIFORM = 3;
    public final static int NORMAL = 4;
    public final static int LECUN_UNIFORM = 5;
    public final static int UNIFORM = 6;
    public final static int XAVIER = 7;
    public final static int XAVIER_UNIFORM = 8;
    public final static int RELU = 9;
    public final static int RELU_UNIFORM = 10;
    public final static int IDENTITY = 11;

    public final static int NORMAL_IN = 12;
    public final static int NORMAL_OUT = 13;
    public final static int NORMAL_AVG = 14;

    public final static int UNIFORM_IN = 15;
    public final static int UNIFORM_OUT = 16;
    public final static int UNIFORM_AVG = 17;

}
