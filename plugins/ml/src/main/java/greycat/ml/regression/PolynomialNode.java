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
package greycat.ml.regression;

import greycat.*;
import greycat.ml.BaseMLNode;
import greycat.ml.RegressionNode;
import greycat.ml.math.PolynomialFit;
import greycat.plugin.NodeState;
import greycat.struct.DoubleArray;
import greycat.utility.Enforcer;

public class PolynomialNode extends BaseMLNode implements RegressionNode {

    /**
     * Tolerated error that can be configure per node to drive the learning process
     */
    public static final String PRECISION = "precision";
    public static final double PRECISION_DEF = 1;
    public static final String VALUE = "value";
    public static final String DERIVATE = "derivate";
    public static final String FUTURE_PREDICTION = "futurePrediction";
    public static final boolean FUTURE_PREDICTION_DEF = false;

    /**
     * Name of the algorithm to be used in the meta model
     */
    public final static String NAME = "PolynomialNode";

    //Internal state variables private and starts with _
    public static final String INTERNAL_WEIGHT_KEY = "weight";
    public static final String INTERNAL_STEP_KEY = "step";
    //Other default parameters that should not be changed externally:
    public static final String MAX_DEGREE = "maxdegree";
    public static final int MAX_DEGREE_DEF = 20; // maximum polynomial degree
    private static final String INTERNAL_TIME_BUFFER = "times";
    private static final String INTERNAL_VALUES_BUFFER = "values";
    private static final String INTERNAL_NB_PAST_KEY = "nb";
    private static final String INTERNAL_LAST_TIME_KEY = "lastTime";
    private static final String INTERNAL_TIME_ORIGIN = "timeorigin";
    private final static String NOT_MANAGED_ATT_ERROR = "Polynomial node can only handle value attribute, please use a super node to store other data";
    private static final Enforcer enforcer = new Enforcer().asPositiveDouble(PRECISION);
    private static final Enforcer degenforcer = new Enforcer().asPositiveInt(MAX_DEGREE);
    private static final Enforcer futurePredictionEnforcer = new Enforcer().asBool(FUTURE_PREDICTION);


    public PolynomialNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private static double[] updateBuffer(NodeState state, double t, int maxdeg, String key) {
        DoubleArray tsa = (DoubleArray) state.get(key);
        double[] ts = null;
        if (tsa != null) {
            ts = tsa.extract();
        }
        if (ts == null) {
            ts = new double[1];
            ts[0] = t;
            tsa = (DoubleArray) state.getOrCreate(key, Type.DOUBLE_ARRAY);
            tsa.initWith(ts);
            return ts;
        } else if (ts.length < maxdeg * 4) {
            double[] nts = new double[ts.length + 1];
            System.arraycopy(ts, 0, nts, 0, ts.length);
            nts[ts.length] = t;
            tsa = (DoubleArray) state.getOrCreate(key, Type.DOUBLE_ARRAY);
            tsa.initWith(nts);
            return nts;
        } else {
            double[] nts = new double[ts.length];
            System.arraycopy(ts, 1, nts, 0, ts.length - 1);
            nts[ts.length - 1] = t;
            tsa = (DoubleArray) state.getOrCreate(key, Type.DOUBLE_ARRAY);
            tsa.initWith(nts);
            return nts;
        }
    }

    //Override default Abstract node default setters and getters
    @Override
    public final Node set(String propertyName, int propertyType, Object propertyValue) {
        if (propertyName.equals(VALUE)) {
            learn(Double.parseDouble(propertyValue.toString()), null);
        } else if (propertyName.equals(PRECISION)) {
            enforcer.check(propertyName, propertyType, propertyValue);
            super.set(propertyName, Type.DOUBLE, propertyValue);
        } else if (propertyName.equals(MAX_DEGREE)) {
            degenforcer.check(propertyName, propertyType, propertyValue);
            super.set(propertyName, Type.INT, propertyValue);
        } else if (propertyName.equals(FUTURE_PREDICTION)) {
            futurePredictionEnforcer.check(propertyName, propertyType, propertyValue);
            super.set(propertyName, Type.BOOL, propertyValue);
        } else {
            throw new RuntimeException(NOT_MANAGED_ATT_ERROR);
        }
        return this;
    }

    @Override
    public final Object get(String propertyName) {
        if (propertyName.equals(VALUE)) {
            final Double[] res = {null};
            //ToDo fix callback - return
            extrapolate(new Callback<Double>() {
                @Override
                public void on(Double result) {
                    res[0] = result;
                }
            });
            return res[0];
        } else if (propertyName.equals(DERIVATE)) {
            final Double[] res = {null};
            //ToDo fix callback - return
            derivate(new Callback<Double>() {
                @Override
                public void on(Double result) {
                    res[0] = result;
                }
            });
            return res[0];
        } else {
            return super.get(propertyName);
        }
    }

    @Override
    public final void learn(double value, Callback<Boolean> callback) {
        NodeState previousState = unphasedState(); //past state, not cloned

        //final long dephasing = timeDephasing();
        long timeOrigin = previousState.time();

        if (timeOrigin == Constants.BEGINNING_OF_TIME) {
            if (previousState.getWithDefault(INTERNAL_TIME_ORIGIN, null) == null) {
                previousState.set(INTERNAL_TIME_ORIGIN, Type.LONG, this.time());
            } else {
                timeOrigin = (long) previousState.get(INTERNAL_TIME_ORIGIN);
            }
        }

        long nodeTime = time();


        double precision = previousState.getWithDefault(PRECISION, PRECISION_DEF);
        DoubleArray weight = (DoubleArray) previousState.get(INTERNAL_WEIGHT_KEY);

        //Initial feed for the very first time, the weight is set directly with the first value that arrives
        if (weight == null) {
            weight = (DoubleArray) previousState.getOrCreate(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY);
            weight.init(1);
            weight.set(0, value);
            previousState.set(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY, weight);
            previousState.set(INTERNAL_NB_PAST_KEY, Type.INT, 1);
            previousState.set(INTERNAL_STEP_KEY, Type.LONG, 0l);
            previousState.set(INTERNAL_LAST_TIME_KEY, Type.LONG, 0l);
            DoubleArray timebuffer = ((DoubleArray) previousState.getOrCreate(INTERNAL_TIME_BUFFER, Type.DOUBLE_ARRAY));
            timebuffer.init(1);
            timebuffer.set(0, 0);
            DoubleArray valuebuffer = (DoubleArray) previousState.getOrCreate(INTERNAL_VALUES_BUFFER, Type.DOUBLE_ARRAY);
            valuebuffer.init(1);
            valuebuffer.set(0, value);
            if (callback != null) {
                callback.on(true);
            }
            return;
        }
        //Check if we are inserting in the past:
        long previousTime = timeOrigin + (Long) previousState.get(INTERNAL_LAST_TIME_KEY);
        if (nodeTime > previousTime) {
            // For the second time point, test and check for the step in time
            Long stp = (Long) previousState.get(INTERNAL_STEP_KEY);
            long lastTime = nodeTime - timeOrigin;
            if (stp == null || stp == 0) {
                if (lastTime == 0) {
                    weight = (DoubleArray) previousState.getOrCreate(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY);
                    weight.init(1);
                    weight.set(0, value);
                    previousState.set(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY, weight);
                    DoubleArray timebuffer = ((DoubleArray) previousState.getOrCreate(INTERNAL_TIME_BUFFER, Type.DOUBLE_ARRAY));
                    timebuffer.init(1);
                    timebuffer.set(0, 0);
                    DoubleArray valuebuffer = (DoubleArray) previousState.getOrCreate(INTERNAL_VALUES_BUFFER, Type.DOUBLE_ARRAY);
                    valuebuffer.init(1);
                    valuebuffer.set(0, value);
                    if (callback != null) {
                        callback.on(true);
                    }
                    return;
                } else {
                    stp = lastTime;
                    previousState.set(INTERNAL_STEP_KEY, Type.LONG, stp);
                }
            }

            //Then, first step, check if the current model already fits the new value:
            int deg = weight.size() - 1;
            Integer num = (Integer) previousState.get(INTERNAL_NB_PAST_KEY);

            double t = timeDephasing();
            ;
            t = t / stp;
            double maxError = maxErr(precision, deg);

            int maxd = previousState.getWithDefault(MAX_DEGREE, MAX_DEGREE_DEF);

            double[] times = updateBuffer(previousState, t, maxd, INTERNAL_TIME_BUFFER);
            double[] values = updateBuffer(previousState, value, maxd, INTERNAL_VALUES_BUFFER);


            //If yes, update some states parameters and return
            if (Math.abs(PolynomialFit.extrapolate(t, weight.extract()) - value) <= maxError) {
                previousState.set(INTERNAL_NB_PAST_KEY, Type.INT, num + 1);
                previousState.set(INTERNAL_LAST_TIME_KEY, Type.LONG, lastTime);
                if (callback != null) {
                    callback.on(true);
                }
                return;
            }

            //If not increase polynomial degrees
            int newdeg = Math.min(times.length, maxd);
            while (deg < newdeg && times.length < maxd * 4) {
                maxError = maxErr(precision, deg);
                PolynomialFit pf = new PolynomialFit(deg);
                pf.fit(times, values);
                if (tempError(pf.getCoef(), times, values) <= maxError) {
                    weight.initWith(pf.getCoef());
                    previousState.set(INTERNAL_NB_PAST_KEY, Type.INT, num + 1);
                    previousState.set(INTERNAL_LAST_TIME_KEY, Type.LONG, lastTime);
                    if (callback != null) {
                        callback.on(true);
                    }
                    return;
                }
                deg++;
            }


            //It does not fit, create a new state and split the polynomial, different splits if we are dealing with the future or with the past

            long newstep = nodeTime - previousTime;
            NodeState phasedState = newState(previousTime); //force clone
            double[] nvalues = new double[2];
            double[] ntimes = new double[2];

            ntimes[0] = 0;
            ntimes[1] = 1;
            nvalues[0] = values[values.length - 2];
            nvalues[1] = value;

            //Test if the newly created polynomial is of degree 0 or 1.
            maxError = maxErr(precision, 0);
            if (Math.abs(nvalues[1] - nvalues[0]) <= maxError) {
                // Here it's a degree 0
                weight = (DoubleArray) phasedState.getOrCreate(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY);
                weight.init(1);
                weight.set(0, nvalues[0]);
            } else {
                //Here it's a degree 1
                weight = (DoubleArray) phasedState.getOrCreate(INTERNAL_WEIGHT_KEY, Type.DOUBLE_ARRAY);
                weight.init(2);
                weight.set(0, nvalues[0]);
                weight.set(1, nvalues[1] - nvalues[0]);
            }

            previousState.set(INTERNAL_TIME_BUFFER, Type.DOUBLE_ARRAY, null);
            previousState.set(INTERNAL_VALUES_BUFFER, Type.DOUBLE_ARRAY, null);
            //create and set the phase set
            DoubleArray timeBuffer = (DoubleArray) phasedState.getOrCreate(INTERNAL_TIME_BUFFER, Type.DOUBLE_ARRAY);
            timeBuffer.initWith(ntimes);
            DoubleArray valueBuffer = (DoubleArray) phasedState.getOrCreate(INTERNAL_VALUES_BUFFER, Type.DOUBLE_ARRAY);
            valueBuffer.initWith(nvalues);


            phasedState.set(PRECISION, Type.DOUBLE, precision);
            phasedState.set(MAX_DEGREE, Type.INT, maxd);
            phasedState.set(INTERNAL_NB_PAST_KEY, Type.INT, 2);
            phasedState.set(INTERNAL_STEP_KEY, Type.LONG, newstep);
            phasedState.set(INTERNAL_LAST_TIME_KEY, Type.LONG, newstep);
            if (callback != null) {
                callback.on(true);
            }
            return;
        } else {
            // 2 phased states need to be created
            //TODO Insert in past.
        }
        if (callback != null) {
            callback.on(false);
        }
    }

    @Override
    public final void extrapolate(Callback<Double> callback) {
        //long time = time();
        NodeState state = unphasedState();
        //long timeOrigin = state.time();
        DoubleArray dw = (DoubleArray) state.get(INTERNAL_WEIGHT_KEY);
        double[] weight = null;
        if (dw != null) {
            weight = dw.extract();
        }
        if (weight == null) {
            if (callback != null) {
                callback.on(0.0);
            }
            return;
        }
        Long inferSTEP = (Long) state.get(INTERNAL_STEP_KEY);
        if (inferSTEP == null || inferSTEP == 0) {
            if (callback != null) {
                callback.on(weight[0]);
            }
            return;
        }
        double t = timeDephasing();// (time - timeOrigin);
        Long lastTime = (Long) state.get(INTERNAL_LAST_TIME_KEY);
        boolean futurePrediction = state.getWithDefault(FUTURE_PREDICTION, FUTURE_PREDICTION_DEF);
        if (!futurePrediction && t > lastTime) {
            t = (double) lastTime;
        }
        t = t / inferSTEP;
        if (callback != null) {
            callback.on(PolynomialFit.extrapolate(t, weight));
        }
    }

    public final void derivate(Callback<Double> callback) {
        //long time = time();
        NodeState state = unphasedState();
        //long timeOrigin = state.time();
        DoubleArray dw = (DoubleArray) state.get(INTERNAL_WEIGHT_KEY);
        double[] oldweight = null;
        if (dw != null) {
            oldweight = dw.extract();
        }
        if (oldweight == null) {
            if (callback != null) {
                callback.on(0.0);
            }
            return;
        }
        double[] weight = calculateDerivative(oldweight);

        Long inferSTEP = (Long) state.get(INTERNAL_STEP_KEY);
        if (inferSTEP == null || inferSTEP == 0) {
            if (callback != null) {
                callback.on(weight[0]);
            }
            return;
        }
        double t = timeDephasing();// (time - timeOrigin);
        Long lastTime = (Long) state.get(INTERNAL_LAST_TIME_KEY);
        boolean futurePrediction = state.getWithDefault(FUTURE_PREDICTION, FUTURE_PREDICTION_DEF);
        if (!futurePrediction && t > lastTime) {
            t = (double) lastTime;
        }
        t = t / inferSTEP;
        if (callback != null) {
            callback.on(PolynomialFit.extrapolate(t, weight));
        }
    }

    private double[] calculateDerivative(double[] oldweight) {
        if (oldweight.length == 1 || oldweight.length == 0) {
            return new double[]{0.0};
        } else {
            double[] res = new double[oldweight.length - 1];
            for (int i = 1; i < oldweight.length; i++) {
                res[i - 1] = oldweight[i] * i;
            }
            return res;
        }
    }

    private double maxErr(double precision, int degree) {
        //double tol = precision;
    /*    if (_prioritization == Prioritization.HIGHDEGREES) {
            tol = precision / Math.pow(2, _maxDegree - degree);
        } else if (_prioritization == Prioritization.LOWDEGREES) {*/
        //double tol = precision / Math.pow(2, degree + 0.5);
       /* } else if (_prioritization == Prioritization.SAMEPRIORITY) {
            tol = precision * degree * 2 / (2 * _maxDegree);
        }*/
        return precision / Math.pow(2, degree + 1);
    }

    private double tempError(double[] computedWeights, double[] times, double[] values) {
        double maxErr = 0;
        double temp;
        for (int i = 0; i < times.length; i++) {
            temp = Math.abs(values[i] - PolynomialFit.extrapolate(times[i], computedWeights));
            if (temp > maxErr) {
                maxErr = temp;
            }
        }
        return maxErr;
    }

    @Override
    public final String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"world\":");
        builder.append(world());
        builder.append(",\"time\":");
        builder.append(time());
        builder.append(",\"id\":");
        builder.append(id());
        final NodeState state = unphasedState();
        long timeOrigin = state.time();
        long step = state.getWithDefault(INTERNAL_STEP_KEY, 1l);
        double[] weight = state.getDoubleArray(INTERNAL_WEIGHT_KEY).extract();
        if (weight != null) {
            builder.append("\",polynomial\":\"");
            for (int i = 0; i < weight.length; i++) {
                if (i != 0) {
                    builder.append("+(");
                }
                builder.append(weight[i]);
                if (i == 1) {
                    if (timeOrigin == 0) {
                        if (step == 1) {
                            builder.append("*t");
                        } else {
                            builder.append("*t/").append(step);
                        }
                    } else {
                        if (step == 1) {
                            builder.append("*(t-").append(timeOrigin).append(")");
                        } else {
                            builder.append("*(t-").append(timeOrigin).append(")/").append(step);
                        }
                    }
                } else if (i > 1) {
                    if (timeOrigin == 0) {
                        if (step == 1) {
                            builder.append("*t^");
                            builder.append(i);
                        } else {
                            builder.append("*(t/").append(step).append(")^");
                            builder.append(i);
                        }
                    } else {
                        if (step == 1) {
                            builder.append("*(t-").append(timeOrigin).append(")^");
                            builder.append(i);
                        } else {
                            builder.append("*((t-").append(timeOrigin).append(")/").append(step).append(")^");
                            builder.append(i);
                        }
                    }
                }
                if (i != 0) {
                    builder.append(")");
                }
            }
            builder.append("\"");
        }
        builder.append("}");
        return builder.toString();
    }

}
