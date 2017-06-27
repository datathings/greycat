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
package greycat.internal.custom;

// Hyper-Rectangle class supporting KDTreeOld class
public class HRect {

    public double[] min;
    public double[] max;

    public HRect(double[] vmin, double[] vmax) {
        min = new double[vmin.length];
        max = new double[vmax.length];
        System.arraycopy(vmin, 0, min, 0, vmin.length);
        System.arraycopy(vmax, 0, max, 0, vmax.length);
    }

    public final Object clone() {
        return new HRect(min, max);
    }

    // from Moore's eqn. 6.6
    public double[] closest(double[] t) {

        double[] p = new double[t.length];

        for (int i = 0; i < t.length; ++i) {
            if (t[i] <= min[i]) {
                p[i] = min[i];
            } else if (t[i] >= max[i]) {
                p[i] = max[i];
            } else {
                p[i] = t[i];
            }
        }

        return p;
    }

    // used in initial conditions of KDTreeOld.nearest()
    public static HRect infiniteHRect(int d) {
        double[] vmin = new double[d];
        double[] vmax = new double[d];
        for (int i = 0; i < d; ++i) {
            vmin[i] = Double.NEGATIVE_INFINITY;
            vmax[i] = Double.POSITIVE_INFINITY;
        }
        return new HRect(vmin, vmax);
    }

    @Override
    public String toString() {
        return min + "\n" + max + "\n";
    }
}

