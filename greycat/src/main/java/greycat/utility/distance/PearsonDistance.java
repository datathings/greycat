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
package greycat.utility.distance;

public class PearsonDistance implements Distance {

    private static PearsonDistance static_instance = null;

    public static PearsonDistance instance() {
        if (static_instance == null) {
            static_instance = new PearsonDistance();
        }
        return static_instance;
    }

    private PearsonDistance() {
    }

    @Override
    public final double measure(double[] a, double[] b) {
        double xy = 0, x = 0, x2 = 0, y = 0, y2 = 0;
        for (int i = 0; i < a.length; i++) {
            xy += a[i] * b[i];
            x += a[i];
            y += b[i];
            x2 += a[i] * a[i];
            y2 += b[i] * b[i];
        }
        int n = a.length;
        return (xy - (x * y) / n) / Math.sqrt((x2 - (x * x) / n) * (y2 - (y * y) / n));
    }

    @Override
    public final boolean compare(double x, double y) {
        return Math.abs(x) > Math.abs(y);
    }

    @Override
    public final double getMinValue() {
        return 1;
    }

    @Override
    public final double getMaxValue() {
        return 0;
    }

}
