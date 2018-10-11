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
package org.mwg.mlx.algorithm;

import org.mwg.Graph;
import org.mwg.ml.RegressionNode;
import org.mwg.plugin.NodeState;
import org.mwg.utility.Enforcer;

public abstract class AbstractLinearRegressionNode extends AbstractAnySlidingWindowManagingRegressionNode implements RegressionNode {

    /**
     * Regression coefficients
     */
    public static final String COEFFICIENTS_KEY = "regressionCoefficients";
    /**
     * Regression coefficients - default
     */
    public static final double[] COEFFICIENTS_DEF = new double[0];
    /**
     * Regression intercept
     */
    public static final String INTERCEPT_KEY = "regressionIntercept";
    /**
     * Regression intercept - default
     */
    public static final double INTERCEPT_DEF = 0.0;

    /**
     * L2 regularization coefficient
     */
    public static final String L2_COEF_KEY = "L2Coefficient";
    /**
     * L2 regularization coefficient - default
     */
    public static final double L2_COEF_DEF = 0.0;

    public AbstractLinearRegressionNode(long p_world, long p_time, long p_id, Graph p_graph) {
        super(p_world, p_time, p_id, p_graph);
    }

    private static final Enforcer alrEnforcer = new Enforcer()
            .asNonNegativeDouble(L2_COEF_KEY);

    @Override
    public void setProperty(String propertyName, byte propertyType, Object propertyValue) {
        if (COEFFICIENTS_KEY.equals(propertyName) || INTERCEPT_KEY.equals(propertyName)) {
            //Nothing. Those cannot be set.
        }else{
            alrEnforcer.check(propertyName, propertyType, propertyValue);
            super.setProperty(propertyName, propertyType, propertyValue);
        }
    }

    @Override
    protected void setBootstrapModeHook(NodeState state) {
        //What should we do when bootstrap mode is approaching?
        //TODO Nothing?
    }

    @Override
    public double predictValue(NodeState state, double curValue[]){
        return predictValueInternal(curValue, state.getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF), state.getFromKeyWithDefault(INTERCEPT_KEY, INTERCEPT_DEF));
    }

    protected double predictValueInternal(double curValue[], double coefs[], double intercept){
        double response = 0;
        for (int i=0;i<curValue.length;i++){
            response += coefs[i]*curValue[i];
        }
        response += intercept;
        return response;
    }

    public double debugGetBufferError(){
        NodeState state = unphasedState();
        return getBufferError(state, (double[])state.getFromKey(INTERNAL_VALUE_BUFFER_KEY), (double[])state.getFromKey(INTERNAL_RESULTS_BUFFER_KEY));
    }

    @Override
    public double getBufferError(NodeState state, double valueBuffer[], double results[]) {
        //For each value in value buffer
        int startIndex = 0;
        final int dims = valueBuffer.length / results.length;

        final int numValues = valueBuffer.length / dims;//TODO What if there are not enough values?
        if (numValues == 0) {
            return 0;
        }

        double coefficients[] = state.getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF);
        int index = 0;
        double intercept = state.getFromKeyWithDefault(INTERCEPT_KEY, INTERCEPT_DEF);
        double sqrResidualSum = 0;
        while (startIndex + dims <= valueBuffer.length) { //For each value
            double curValue[] = new double[dims];
            System.arraycopy(valueBuffer, startIndex, curValue, 0, dims);
            double response = predictValueInternal(curValue,coefficients,intercept);

            sqrResidualSum += (response - results[index])*(response - results[index]);

            //Continue the loop
            startIndex += dims;
            index++;
        }
        return sqrResidualSum / numValues;
    }

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        double coefs[] = unphasedState().getFromKeyWithDefault(COEFFICIENTS_KEY, COEFFICIENTS_DEF);
        result.append("Coefficients: ");
        for (int j = 0; j < coefs.length; j++) {
            result.append(coefs[j] + ", ");
        }
        return result.toString();
    }
}
