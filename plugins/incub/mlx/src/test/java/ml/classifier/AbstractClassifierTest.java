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
package ml.classifier;

import org.mwg.Callback;
import org.mwg.Node;
import org.mwg.Type;
import org.mwg.ml.AbstractMLNode;
import org.mwg.mlx.algorithm.AbstractAnySlidingWindowManagingNode;
import org.mwg.mlx.algorithm.AbstractClassifierSlidingWindowManagingNode;

/**
 * Created by andre on 5/9/2016.
 */
public class AbstractClassifierTest {

    public static final String FEATURE = "f1";
    protected double dummyDataset1[][] = new double[][]{{18.1875, 0}, {18.1875, 0}, {18.1875, 0}, {18.1875, 0}, {18.1875, 0}, {18.1875, 0}, {21.4493, 1}, {21.4493, 1}, {21.3747, 0}, {21.276, 1}, {21.1987, 1},
            {21.0933, 1}, {21.0, 0}, {20.9053, 0}, {20.816, 0}, {20.5253, 0}, {20.5253, 0}, {20.43, 1}, {20.0053, 0}, {19.5727, 0}, {16.282, 2}, {16.1087, 2}, {15.99, 2}, {15.99, 2}, {15.96, 2}, {22.156, 1},
            {22.044, 1}, {21.82, 1}, {21.6973, 1}, {21.5627, 1}, {21.5627, 0}, {21.5627, 1}, {21.4453, 1}, {21.4453, 1}, {21.304, 1}, {21.18, 1}, {21.04, 0}, {20.8987, 0}, {20.7747, 0}, {20.264, 0}, {18.0233, 0},
            {17.956, 0}, {17.8847, 0}, {17.83, 0}, {17.83, 2}, {17.7513, 2}, {17.708, 2}, {21.3613, 1}, {20.62, 0}, {20.512, 0}, {20.3933, 0}, {20.3933, 0}, {20.278, 0}, {20.278, 0}, {19.9253, 0}, {19.46, 0},
            {19.342, 0}, {19.342, 0}, {18.7487, 0}, {18.4027, 0}, {15.392, 2}, {15.294, 2}, {20.43, 0}, {19.9993, 0}, {19.9007, 0}, {19.9007, 0}, {19.9007, 0}, {19.8167, 0}, {19.732, 0}, {19.6387, 0}, {19.554, 0},
            {19.4633, 0}, {19.4633, 0}, {19.4633, 0}, {19.4633, 0}, {19.2593, 0}, {19.162, 0}, {19.162, 0}, {18.954, 0}, {18.954, 0}, {15.3673, 2}, {15.4533, 2}, {16.1507, 2}, {17.0047, 2}, {17.3027, 2},
            {18.2773, 0}, {18.9313, 0}, {19.9507, 0}, {22.6267, 1}, {23.088, 1}, {23.088, 1}, {23.2853, 1}, {23.2853, 1}, {23.168, 1}, {23.0693, 1}, {23.0693, 1}, {23.008, 1}, {22.9413, 1}, {22.8147, 1},
            {21.828, 1}, {18.3867, 0}, {18.518, 0}, {18.9927, 0}, {19.196, 0}, {19.668, 0}, {19.668, 0}, {19.9133, 0}, {20.7053, 1}, {22.284, 1}, {22.952, 1}, {23.1547, 1}, {23.3453, 1}, {24.184, 1}, {24.128, 1},
            {23.8427, 1}, {23.8427, 1}, {23.284, 1}, {22.992, 1}, {22.6093, 1}, {21.7493, 1}, {17.9547, 0}, {17.5847, 0}, {17.5847, 2}, {17.5847, 2}, {17.4973, 2}, {20.8373, 0}, {20.624, 0}, {20.05, 0}, {20.05, 0},
            {19.8027, 0}, {19.8027, 0}, {19.6587, 0}, {19.6587, 0}, {19.6587, 0}, {19.5333, 0}, {19.3947, 0}, {19.2573, 0}, {18.9867, 0}, {18.7667, 0}, {18.3987, 0}, {14.9493, 2}, {14.9493, 2}, {14.7967, 2},
            {14.7393, 2}, {14.6693, 2}, {14.6693, 2}, {14.56, 2}, {14.56, 2}, {13.02, 2}, {12.9473, 2}, {12.892, 2}, {12.892, 2}, {12.7767, 2}, {12.7767, 2}, {12.6993, 2}, {12.6993, 2}, {12.6, 2}, {12.6, 2},
            {12.4807, 2}, {12.3013, 2}, {12.0, 2}, {11.9767, 2}, {11.9767, 2}, {11.97, 2}, {11.97, 2}, {11.954, 2}, {11.954, 2}, {11.922, 2}, {17.0987, 2}, {16.67, 2}, {16.5313, 2}, {16.4187, 2}, {16.2967, 2},
            {16.0707, 2}, {15.904, 2}, {15.7933, 2}, {15.68, 2}, {15.4493, 2}, {15.4493, 2}, {14.3, 2}, {11.3867, 2}, {11.3867, 2}, {11.352, 2}, {11.352, 2}, {11.3553, 2}, {11.4333, 2}, {18.5233, 0}, {18.2787, 0},
            {18.19, 2}, {18.1127, 0}, {18.1127, 0}, {18.0147, 0}, {17.8267, 0}, {17.7527, 2}, {17.54, 0}, {17.3727, 2}, {17.272, 2}, {17.0427, 2}, {17.0427, 2}, {16.666, 2}, {14.0913, 2}, {13.916, 2}, {13.8427, 2},
            {13.81, 2}, {13.7767, 2}, {13.77, 2}, {19.89, 0}, {19.734, 0}, {19.5793, 0}, {19.5793, 0}, {19.4047, 0}, {19.4047, 0}, {19.3147, 0}, {19.148, 0}, {19.148, 0}, {19.06, 0}, {19.06, 0}, {18.8967, 0},
            {18.8167, 0}, {18.8167, 0}, {15.292, 2}, {15.2127, 2}, {15.9647, 2}, {15.9647, 2}, {17.7833, 2}, {18.5573, 2}, {19.3287, 0}, {20.0233, 0}, {20.0267, 0}, {20.0127, 0}, {19.982, 0}, {19.95, 0}, {19.9, 0},
            {19.8493, 0}, {19.486, 0}, {19.408, 0}, {19.11, 0}, {18.8, 0}, {18.4453, 0}, {18.1667, 0}, {15.182, 2}, {16.822, 2}, {17.6733, 2}, {17.6733, 0}, {18.5493, 0}, {18.5493, 0}, {18.8327, 0}, {18.8327, 0},
            {19.8727, 0}, {21.3, 1}, {21.0027, 1}, {20.8093, 0}, {20.8093, 0}, {20.8093, 0}, {20.4233, 0}, {19.7547, 0}, {19.506, 0}, {19.2467, 0}, {19.2467, 0}, {19.118, 0}, {15.8907, 2}, {15.7867, 2}, {15.7867, 2},
            {15.5613, 2}, {15.554, 2}, {23.0787, 1}, {22.472, 1}, {21.9133, 1}, {21.9133, 1}, {21.8173, 1}, {21.8173, 1}, {21.5787, 1}, {21.5787, 1}, {21.4613, 1}, {21.3533, 1}, {21.3533, 1}, {21.3533, 0},
            {20.984, 0}, {20.8533, 0}, {20.2313, 0}, {16.8433, 2}, {16.8433, 2}, {16.8433, 2}, {16.7447, 2}, {16.7447, 2}, {16.5927, 2}, {16.55, 2}, {20.2707, 0}, {20.2707, 0}, {19.6267, 0}, {19.6267, 0},
            {19.4767, 0}, {19.4767, 0}, {19.186, 0}, {19.0833, 0}, {18.7867, 2}, {18.7867, 0}, {18.7867, 0}, {18.524, 0}, {18.3647, 2}, {14.7353, 2}, {14.65, 2}, {14.61, 2}, {14.61, 2}, {14.65, 2}, {14.8073, 2},
            {23.056, 1}, {22.696, 1}, {22.3813, 1}, {22.1787, 1}, {22.0613, 1}, {21.964, 1}, {21.7507, 1}, {21.628, 1}, {21.5147, 1}, {21.5147, 1}, {21.4187, 1}, {21.1707, 0}, {21.0427, 1}, {21.0427, 0}, {16.9627, 2},
            {16.908, 2}, {16.8433, 2}, {16.776, 2}, {16.768, 2}, {16.8, 2}, {22.992, 1}, {22.9053, 1}, {22.7347, 1}, {22.7347, 1}, {22.6467, 1}, {22.6467, 1}, {22.5413, 1}, {22.5413, 1}, {22.432, 1}, {22.104, 1},
            {22.104, 1}, {21.9733, 1}, {21.4453, 1}, {21.1533, 0}, {17.384, 2}, {17.384, 2}, {17.3167, 2}, {17.3167, 2}, {17.3547, 2}, {25.3, 1}, {25.2173, 1}, {25.1427, 1}, {25.1427, 1}, {24.992, 1}, {24.916, 1},
            {24.6493, 1}, {24.6493, 1}, {24.6493, 1}, {24.4853, 1}, {24.38, 1}, {24.38, 1}, {24.1707, 1}, {23.8613, 1}, {23.8613, 1}, {19.76, 0}, {20.1653, 0}, {20.338, 0}, {23.852, 1}, {24.008, 1}, {24.0613, 1},
            {24.0773, 1}, {24.0987, 1}, {23.9787, 1}, {23.648, 1}, {23.364, 1}, {23.2053, 1}, {23.2053, 1}, {23.2053, 1}, {23.0107, 1}, {22.62, 1}, {22.1667, 1}, {22.0573, 1}, {21.832, 1}, {21.576, 1}, {19.364, 0},
            {20.652, 0}, {20.652, 1}, {21.7173, 1}, {21.9627, 1}, {21.9627, 1}, {22.1787, 1}, {22.6347, 1}, {23.5947, 1}, {24.152, 1}, {24.2733, 1}, {24.2733, 1}, {24.2507, 1}, {24.1973, 1}, {23.7467, 1},
            {23.3347, 1}, {22.8213, 1}, {22.5933, 1}, {22.4933, 1}, {22.2533, 1}, {19.6467, 0}, {19.476, 0}, {19.4147, 0}, {19.4147, 0}, {19.3933, 0}, {19.3633, 0}, {19.3633, 0}, {21.348, 1}, {21.268, 0},
            {21.192, 0}, {20.8867, 1}, {20.832, 0}, {20.832, 0}, {20.832, 0}, {20.832, 1}, {20.7173, 0}, {20.7173, 0}, {20.624, 0}, {20.5667, 0}, {20.5147, 0}, {18.496, 0}, {18.4687, 0}, {18.4653, 0}, {18.5033, 0},
            {18.5033, 0}, {21.872, 1}, {21.84, 1}, {21.64, 1}, {21.488, 0}, {21.488, 1}, {21.4147, 1}, {21.4147, 1}, {21.24, 0}, {21.24, 0}, {21.152, 0}, {20.988, 1}, {20.988, 1}, {20.8973, 0}, {20.7507, 1},
            {20.692, 0}, {18.6307, 0}, {18.6307, 2}, {20.4847, 0}, {20.21, 0}, {20.21, 0}, {19.9807, 0}, {19.9807, 0}, {19.89, 0}, {19.752, 0}, {19.6787, 0}, {19.606, 0}, {19.606, 0}, {19.606, 0}, {19.606, 0},
            {19.5467, 0}, {19.4073, 0}, {19.3467, 0}, {19.3467, 0}, {19.3467, 0}, {19.06, 0}, {17.8167, 0}, {17.782, 2}, {17.782, 2}, {17.782, 2}, {17.806, 2}, {21.8747, 1}, {21.408, 1}, {21.0787, 1}, {20.8853, 0},
            {20.8853, 1}, {20.8853, 0}, {20.7747, 0}, {20.688, 0}, {20.688, 0}, {20.47, 0}, {20.3067, 0}, {20.3067, 0}, {20.3067, 0}, {19.8867, 0}, {19.7693, 0}, {16.9593, 2}, {16.924, 2}, {16.874, 2}, {16.84, 2},
            {16.84, 2}, {16.7107, 2}, {19.8527, 0}, {19.8527, 0}, {19.7653, 0}, {19.598, 0}, {19.5, 0}, {19.5, 0}, {19.4193, 0}, {19.2493, 0}, {19.1473, 0}, {19.1473, 0}, {19.04, 0}, {18.4487, 0}, {18.336, 0},
            {18.336, 0}, {15.3433, 2}, {15.7053, 2}, {15.8513, 2}, {15.8513, 2}, {17.5413, 2}, {19.6227, 0}, {19.6227, 0}, {19.788, 0}, {20.3227, 0}, {20.458, 0}, {20.1593, 0}, {19.7867, 0}, {19.4447, 0}, {19.29, 0},
            {19.2167, 0}, {19.122, 0}, {18.958, 0}, {18.958, 0}, {18.8027, 0}, {18.36, 0}, {16.0673, 2}, {16.5987, 2}, {17.8233, 2}, {18.1267, 0}, {18.1267, 0}, {18.4453, 0}, {18.4453, 2}, {20.76, 1}, {21.5067, 1},
            {22.1107, 1}, {22.468, 1}, {23.0467, 1}, {23.008, 1}, {22.96, 1}, {22.8867, 1}, {21.6013, 1}, {20.7973, 0}, {20.5813, 0}, {20.5813, 0}, {20.4913, 0}, {17.7287, 2}, {17.65, 2}, {17.608, 2}, {17.568, 2},
            {17.568, 2}, {17.6427, 0}, {22.82, 1}, {22.82, 1}, {22.5853, 1}, {22.484, 1}, {22.484, 1}, {22.2293, 1}, {22.2293, 1}, {22.124, 1}, {22.008, 1}, {21.784, 1}, {21.784, 1}, {21.5347, 1}, {21.0613, 0},
            {20.5627, 0}, {18.7133, 0}, {18.5613, 0}, {18.5267, 0}, {18.5267, 0}, {18.5067, 0}, {18.5067, 0}, {18.5133, 0}, {24.892, 1}, {24.756, 1}, {24.6173, 1}, {24.6173, 1}, {24.6173, 1}, {24.3693, 1},
            {24.3693, 1}, {24.3, 1}, {24.3, 1}, {24.144, 1}, {24.0573, 1}, {23.716, 1}, {23.716, 1}, {20.7627, 1}, {20.7627, 0}, {20.7627, 0}, {20.7627, 1}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0},
            {20.7627, 0}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0}, {20.7627, 0}};

    protected boolean bootstraps1[] = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,};

    protected ClassificationJumpCallback runThroughDummyDataset(AbstractClassifierSlidingWindowManagingNode classfierNode){
        ClassificationJumpCallback cjc = new ClassificationJumpCallback(new String[]{FEATURE});

        for (int i = 0; i < dummyDataset1.length; i++) {
            cjc.value = new double[]{dummyDataset1[i][0]};
            cjc.expectedClass = (int) dummyDataset1[i][1];
            cjc.expectedBootstrap = bootstraps1[i];
            classfierNode.jump(i, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    cjc.on((AbstractClassifierSlidingWindowManagingNode)result);
                }
            });
        }

        return cjc;
    }

    protected void standardSettings(AbstractMLNode node){
        node.setProperty(AbstractAnySlidingWindowManagingNode.BUFFER_SIZE_KEY, Type.INT, 60);
        node.setProperty(AbstractAnySlidingWindowManagingNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 0.2);
        node.setProperty(AbstractAnySlidingWindowManagingNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 0.3);
        node.set(AbstractMLNode.FROM, FEATURE);
    }

    /**
     * Created by andre on 5/9/2016.
     */
    protected static class ClassificationJumpCallback {

        final String features[];

        public ClassificationJumpCallback(String featureNames[]){
            this.features = featureNames;
        }

        public int errors = 0;
        public double value[] = null;
        public boolean expectedBootstrap = true;
        public int expectedClass = -10000;

        Callback<Boolean> cb = new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //Nothing so far
            }
        };

        public void on(AbstractClassifierSlidingWindowManagingNode result) {
            for (int i=0;i<features.length;i++){
                result.set(features[i], value[i]);
            }
            result.learn(expectedClass, cb);
            Boolean bootstrapObj = (Boolean)result.get(AbstractClassifierSlidingWindowManagingNode.BOOTSTRAP_MODE_KEY);
            boolean curBootstrap = (bootstrapObj==null)?true:bootstrapObj;
            if (curBootstrap != expectedBootstrap) {
                errors++;
            }
            result.free();
        }
    }
}
