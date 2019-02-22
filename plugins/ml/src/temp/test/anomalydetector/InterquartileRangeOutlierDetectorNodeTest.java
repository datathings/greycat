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
package greycat.ml.anomalydetector;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.Type;
import greycat.ml.AnomalyDetectionNode;
import greycat.ml.BaseMLNode;
import greycat.ml.MLPlugin;
import greycat.ml.temp.anomalydetector.InterquartileRangeOutlierDetectorNode;
import greycat.scheduler.NoopScheduler;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by andrey.boytsov on 16/06/16.
 */
public class InterquartileRangeOutlierDetectorNodeTest {
    public static final String FEATURE = "f1";

    //rnorm(100) in R
    double testSet[] = new double[]{-0.817744260, -1.365458814, -0.620009256, -0.520956857, -0.894466780, -0.051088197, -2.619288683, 0.102225992, -0.003299866, 0.396176585,
            0.691630235, -0.436487886, -0.422471504, 1.547227526, -0.736721625, -1.617654584, 0.852180599, 0.675784632, 0.563363958, 0.637482377,
            -1.251430880, -0.398667035, 1.037897926, -0.038595241, -0.736625439, 0.329014229, 0.761588891, -0.223292633, 0.915047432, -1.504710509,
            -0.190125099, -1.702326685, 0.533617811, 1.116993521, -0.416579564, -0.481328011, -1.261906265, 0.193957422, -0.827032987, 0.213658406,
            -0.158251389, 0.486154460, 0.129855738, 0.211725282, -1.130834696, -0.275163694, -0.817134794, -0.239001259, -0.635657135, -0.529427114,
            -1.162418356, -0.586342078, 0.247159807, -0.885436897, -1.859038793, 0.846346388, 0.905734039, -0.978588115, -1.225701486, -0.039783977,
            0.203154136, -0.269153810, 1.222489616, -1.055618739, -0.701104263, 1.214092428, -0.453533758, 0.757476789, 0.826918527, 1.412081584,
            1.172735382, 0.479048711, 0.782471190, -0.180828008, -0.316079403, -0.353000141, 0.330479896, 0.927594610, -0.432594687, 0.742635362,
            -0.865745872, -0.546697967, 0.448811623, -0.189598775, 0.868184331, 0.476605718, -0.553966352, -0.586257513, -0.202730776, 0.862270127,
            0.156878572, -0.744383825, -0.216351202, 1.510074024, -0.139686834, 1.414843729, 0.017254308, -1.420384951, 0.060592004, -2.153703322};

    protected AnomalyDetectionJumpCallback runThroughDummyDataset(AnomalyDetectionNode classfierNode) {
        final AnomalyDetectionJumpCallback cjc = new AnomalyDetectionJumpCallback(new String[]{FEATURE});

        Callback<AnomalyDetectionNode> cad = new Callback<AnomalyDetectionNode>() {
            @Override
            public void on(AnomalyDetectionNode result) {
                cjc.on(result);
            }
        };

        for (int i = 0; i < testSet.length; i++) {
            cjc.value = new double[]{testSet[i]};
            classfierNode.travelInTime(i, cad);
        }

        return cjc;
    }

    protected AnomalyDetectionJumpCallback runThroughDummyDataset2D(AnomalyDetectionNode classfierNode) {
        final AnomalyDetectionJumpCallback cjc = new AnomalyDetectionJumpCallback(new String[]{"f1", "f2"});

        Callback<AnomalyDetectionNode> cad = new Callback<AnomalyDetectionNode>() {
            @Override
            public void on(AnomalyDetectionNode result) {
                cjc.on(result);
            }
        };

        for (int i = 0; i < testSet.length; i++) {
            cjc.value = new double[]{testSet[i], -testSet[i]};
            classfierNode.travelInTime(i, cad);
        }

        return cjc;
    }

    @Test
    public void test1D() {
        //This test fails if there are too many errors
        final Graph graph = new GraphBuilder().withPlugin(new MLPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                InterquartileRangeOutlierDetectorNode iqadNode = (InterquartileRangeOutlierDetectorNode) graph.newTypedNode(0, 0, InterquartileRangeOutlierDetectorNode.NAME);

                iqadNode.set(InterquartileRangeOutlierDetectorNode.BUFFER_SIZE_KEY, Type.INT, testSet.length);
                iqadNode.set(BaseMLNode.FROM, Type.STRING, FEATURE);

                final AnomalyDetectionJumpCallback adjc = runThroughDummyDataset(iqadNode);
                final AnomalyClassifyCallback acc = new AnomalyClassifyCallback(new String[]{FEATURE});
                int index = testSet.length;

                Callback<AnomalyDetectionNode> accCB = new Callback<AnomalyDetectionNode>() {
                    @Override
                    public void on(AnomalyDetectionNode result) {
                        acc.on(result);
                    }
                };

                acc.value = new double[]{-2.44};
                acc.expectedOutlier = true;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{-2.43};
                acc.expectedOutlier = false;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{2.36};
                acc.expectedOutlier = false;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{2.37};
                acc.expectedOutlier = true;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                iqadNode.free();
                graph.disconnect(null);
            }
        });
    }

    @Test
    public void test2D() {
        //This test fails if there are too many errors
        final Graph graph = new GraphBuilder().withPlugin(new MLPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                InterquartileRangeOutlierDetectorNode iqadNode = (InterquartileRangeOutlierDetectorNode) graph.newTypedNode(0, 0, InterquartileRangeOutlierDetectorNode.NAME);

                iqadNode.set(InterquartileRangeOutlierDetectorNode.BUFFER_SIZE_KEY, Type.INT, testSet.length);
                iqadNode.set(BaseMLNode.FROM, Type.STRING, "f1;f2");

                final AnomalyDetectionJumpCallback adjc = runThroughDummyDataset2D(iqadNode);
                final AnomalyClassifyCallback acc = new AnomalyClassifyCallback(new String[]{"f1", "f2"});
                int index = testSet.length;

                Callback<AnomalyDetectionNode> accCB = new Callback<AnomalyDetectionNode>() {
                    @Override
                    public void on(AnomalyDetectionNode result) {
                        acc.on(result);
                    }
                };

                acc.value = new double[]{-2.44, 0};
                acc.expectedOutlier = true;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{0, 2.56};
                acc.expectedOutlier = true;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{-2.43, 2.43};
                acc.expectedOutlier = false;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{2.36, -2.36};
                acc.expectedOutlier = false;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                acc.value = new double[]{2.37, 0};
                acc.expectedOutlier = true;
                iqadNode.travelInTime(index, accCB);
                assertTrue(acc.called);
                index++;

                iqadNode.free();
                graph.disconnect(null);
            }
        });
    }

    protected static class AnomalyDetectionJumpCallback {

        final String features[];
        public double value[] = null;
        public boolean expectedOutlier = true;
        Callback<Boolean> cb = new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //Nothing so far
            }
        };

        public AnomalyDetectionJumpCallback(String featureNames[]) {
            this.features = featureNames;
        }

        public void on(AnomalyDetectionNode result) {
            for (int i = 0; i < features.length; i++) {
                result.set(features[i], Type.DOUBLE, value[i]);
            }
            result.learn(cb);
            result.free();
        }
    }

    protected static class AnomalyClassifyCallback {

        final String features[];
        public double value[] = null;
        public boolean expectedOutlier = true;
        public boolean called = false;
        Callback<Boolean> cb = new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                //Nothing so far
                called = true;
                assertTrue(expectedOutlier == result);
            }
        };

        public AnomalyClassifyCallback(String featureNames[]) {
            this.features = featureNames;
        }

        public void on(AnomalyDetectionNode result) {
            for (int i = 0; i < features.length; i++) {
                result.set(features[i], Type.DOUBLE, value[i]);
            }
            result.classify(cb);
            result.free();
        }
    }
}
