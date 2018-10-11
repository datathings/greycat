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
package ml.regression;

import org.junit.Test;
import org.mwg.*;
import org.mwg.internal.scheduler.NoopScheduler;
import org.mwg.ml.AbstractMLNode;
import org.mwg.mlx.MLXPlugin;
import org.mwg.mlx.algorithm.AbstractLinearRegressionNode;
import org.mwg.mlx.algorithm.regression.LinearRegressionWithPeriodicityNode;

import static org.junit.Assert.assertTrue;

public class LinearRegressionPeriodicNodeTest extends AbstractLinearRegressionTest {

    double consumptionLondon[] = new double[]{64, 78, 96, 93, 92, 124, 87, 44, 25, 48, 87, 119, 78, 58, 60, 124, 279, 625, 115, 83, 1018, 1382, 950, 1266, 950, 2022,
            1181, 779, 245, 354, 192, 168, 137, 147, 1123, 915, 270, 861, 889, 258, 195, 185, 206, 194, 287, 155, 169, 92, 87, 56, 76, 42, 20, 58, 90, 67, 83, 45, 17,
            25, 93, 80, 71, 180, 116, 486, 202, 153, 116, 69, 63, 74, 129, 265, 103, 183, 126, 126, 85, 122, 68, 35, 30, 240, 1304, 741, 283, 283, 270, 271, 250, 228, 327,
            208, 188, 116, 87, 95, 89, 38, 18, 29, 59, 110, 102, 39, 18, 27, 56, 88, 93, 38, 33, 233, 432, 188, 111, 71, 83, 60, 172, 128, 269, 61, 64, 95, 65, 90, 92,
            166, 160, 343, 1087, 868, 671, 211, 306, 398, 638, 289, 312, 695, 583, 167, 107, 19, 22, 57, 55, 93, 104, 44, 18, 47, 54, 83, 76, 41, 29, 61, 280, 562, 181,
            521, 303, 272, 503, 530, 188, 118, 186, 131, 133, 133, 105, 309, 206, 183, 156, 172, 1237, 613, 200, 229, 364, 368, 233, 231, 346, 195, 68, 34, 58, 55, 56,
            101, 83, 39, 44, 57, 54, 53, 104, 44, 24, 57, 79, 53, 150, 161, 734, 179, 163, 690, 565, 182, 166, 199, 193, 169, 176, 144, 99, 125, 288, 125, 161, 299, 1173,
            606, 187, 239, 261, 190, 163, 199, 341, 203, 120, 66, 24, 17, 46, 98, 89, 78, 28, 18, 44, 86, 85, 76, 18, 18, 71, 239, 207, 451, 103, 94, 176, 111, 91, 237,
            229, 237, 563, 273, 567, 116, 149, 164, 278, 164, 280, 156, 165, 140, 142, 149, 196, 164, 160, 104, 125, 144, 166, 162, 126, 36, 21, 49, 56, 92, 105, 56, 19,
            32, 57, 86, 91, 52, 19, 38, 70, 321, 277, 468, 282, 595, 329, 123, 223, 74, 151, 179, 88, 58, 77, 79, 56, 30, 141, 123, 787, 1311, 490, 211, 170, 193, 247,
            282, 346, 168, 109, 60};
    double timeLondon[] = new double[]{
            1733400.0, 1735200.0, 1737000.0, 1738800.0, 1740600.0, 1742400.0, 1744200.0, 1746000.0, 1747800.0, 1749600.0, 1751400.0, 1753200.0, 1755000.0,
            1756800.0, 1758600.0, 1760400.0, 1762200.0, 1764000.0, 1765800.0, 1767600.0, 1769400.0, 1771200.0, 1773000.0, 1774800.0, 1776600.0, 1778400.0,
            1780200.0, 1782000.0, 1783800.0, 1785600.0, 1787400.0, 1789200.0, 1791000.0, 1792800.0, 1794600.0, 1796400.0, 1798200.0, 1800000.0, 1801800.0,
            1803600.0, 1805400.0, 1807200.0, 1809000.0, 1810800.0, 1812600.0, 1814400.0, 1816200.0, 1818000.0, 1819800.0, 1821600.0, 1823400.0, 1825200.0,
            1827000.0, 1828800.0, 1830600.0, 1832400.0, 1834200.0, 1836000.0, 1837800.0, 1839600.0, 1841400.0, 1843200.0, 1845000.0, 1846800.0, 1848600.0,
            1850400.0, 1852200.0, 1854000.0, 1855800.0, 1857600.0, 1859400.0, 1861200.0, 1863000.0, 1864800.0, 1866600.0, 1868400.0, 1870200.0000000002,
            1872000.0000000002, 1873800.0000000002, 1875600.0000000002, 1877400.0000000002, 1879200.0000000002, 1881000.0000000002, 1882800.0000000002,
            1884600.0000000002, 1886400.0000000002, 1888200.0000000002, 1890000.0000000002, 1891800.0000000002, 1893600.0000000002, 1895400.0000000002,
            1897200.0000000002, 1899000.0000000002, 1900800.0000000002, 1902600.0000000002, 1904400.0000000002, 1906200.0000000002, 1908000.0000000002,
            1909800.0000000002, 1911600.0000000002, 1913400.0000000002, 1915200.0000000002, 1917000.0000000002, 1918800.0000000002, 1920600.0000000002,
            1922400.0000000002, 1924200.0000000002, 1926000.0000000002, 1927800.0000000002, 1929600.0000000002, 1931400.0000000002, 1933200.0000000002,
            1935000.0000000002, 1936800.0000000002, 1938600.0000000002, 1940400.0000000002, 1942200.0000000002, 1944000.0000000002, 1945800.0000000002,
            1947600.0000000002, 1949400.0000000002, 1951200.0000000002, 1953000.0000000002, 1954800.0000000002, 1956600.0000000002, 1958400.0000000002,
            1960200.0000000002, 1962000.0000000002, 1963800.0000000002, 1965600.0000000002, 1967400.0000000002, 1969200.0000000002, 1971000.0000000002,
            1972800.0000000002, 1974600.0000000002, 1976400.0000000002, 1978200.0000000002, 1980000.0000000002, 1981800.0000000002, 1983600.0000000002,
            1985400.0000000002, 1987200.0000000002, 1989000.0000000002, 1990800.0000000002, 1992600.0000000002, 1994400.0000000002, 1996200.0000000002,
            1998000.0000000002, 1999800.0000000002, 2001600.0000000002, 2003400.0000000002, 2005200.0000000002, 2007000.0000000002, 2008800.0000000002,
            2010600.0000000002, 2012400.0000000002, 2014200.0000000002, 2016000.0000000002, 2017800.0000000002, 2019600.0000000002, 2021400.0000000002,
            2023200.0000000002, 2025000.0000000002, 2026800.0000000002, 2028600.0000000002, 2030400.0000000002, 2032200.0000000002, 2034000.0000000002,
            2035800.0000000002, 2037600.0000000002, 2039400.0000000002, 2041200.0000000002, 2043000.0000000002, 2044800.0000000002, 2046600.0000000002,
            2048400.0000000002, 2050200.0000000002, 2052000.0000000002, 2053800.0000000002, 2055600.0000000002, 2057400.0000000002, 2059200.0000000002,
            2061000.0000000002, 2062800.0000000002, 2064600.0000000002, 2066400.0000000002, 2068200.0000000002, 2070000.0000000002, 2071800.0000000002,
            2073600.0000000002, 2075400.0000000002, 2077200.0000000002, 2079000.0000000002, 2080800.0000000002, 2082600.0000000002, 2084400.0000000002,
            2086200.0000000002, 2088000.0000000002, 2089800.0000000002, 2091600.0000000002, 2093400.0000000002, 2095200.0000000002, 2097000.0000000002,
            2098800.0, 2100600.0, 2102400.0, 2104200.0, 2106000.0, 2107800.0, 2109600.0, 2111400.0, 2113200.0, 2115000.0, 2116800.0, 2118600.0, 2120400.0,
            2122200.0, 2124000.0, 2125800.0, 2127600.0, 2129400.0, 2131200.0, 2133000.0, 2134800.0, 2136600.0, 2138400.0, 2140200.0, 2142000.0, 2143800.0,
            2145600.0, 2147400.0, 2149200.0, 2151000.0, 2152800.0, 2154600.0, 2156400.0, 2158200.0, 2160000.0, 2161800.0, 2163600.0, 2165400.0, 2167200.0,
            2169000.0, 2170800.0, 2172600.0, 2174400.0, 2176200.0, 2178000.0, 2179800.0, 2181600.0, 2183400.0, 2185200.0, 2187000.0, 2188800.0, 2190600.0,
            2192400.0, 2194200.0, 2196000.0, 2197800.0, 2199600.0, 2201400.0, 2203200.0, 2205000.0, 2206800.0, 2208600.0, 2210400.0, 2212200.0, 2214000.0,
            2215800.0, 2217600.0, 2219400.0, 2221200.0, 2223000.0, 2224800.0, 2226600.0, 2228400.0, 2230200.0, 2232000.0, 2233800.0, 2235600.0, 2237400.0,
            2239200.0, 2241000.0, 2242800.0, 2244600.0, 2246400.0, 2248200.0, 2250000.0, 2251800.0, 2253600.0, 2255400.0, 2257200.0, 2259000.0, 2260800.0,
            2262600.0, 2264400.0, 2266200.0, 2268000.0, 2269800.0, 2271600.0, 2273400.0, 2275200.0, 2277000.0, 2278800.0, 2280600.0, 2282400.0, 2284200.0,
            2286000.0, 2287800.0, 2289600.0, 2291400.0, 2293200.0, 2295000.0, 2296800.0, 2298600.0, 2300400.0, 2302200.0, 2304000.0, 2305800.0, 2307600.0,
            2309400.0, 2311200.0, 2313000.0, 2314800.0, 2316600.0, 2318400.0, 2320200.0, 2322000.0, 2323800.0, 2325600.0, 2327400.0, 2329200.0, 2331000.0,
            2332800.0, 2334600.0, 2336400.0};

    protected RegressionJumpCallback runThroughDummyDatasetWithPeriodicComponent(LinearRegressionWithPeriodicityNode lrNode, boolean swapResponse, double periods[], double sinPhaseShifts[], double amplitudes[]) {
        RegressionJumpCallback rjc = new RegressionJumpCallback(new String[]{FEATURE});
        for (int i = 0; i < 10; i++) {
            //assertTrue(rjc.bootstrapMode);
            rjc.value = new double[]{i};
            rjc.response = 2*i+1;
            for (int j=0;j<periods.length;j++){
                rjc.response = rjc.response + amplitudes[j]*Math.sin(2*Math.PI*rjc.value[0]/periods[j] + sinPhaseShifts[j]);
            }
            lrNode.jump(i, new Callback<Node>() {
                @Override
                public void on(Node result) {
                    rjc.on((AbstractLinearRegressionNode) result);
                }
            });
        }
        //assertFalse(rjc.bootstrapMode);
        return rjc;
    }

    @Test
    public void testNormalPreciseNoPhaseShift() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                lrNode.setProperty(AbstractLinearRegressionNode.BUFFER_SIZE_KEY, Type.INT, 80);
                lrNode.setProperty(AbstractLinearRegressionNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 100.0);
                lrNode.setProperty(AbstractLinearRegressionNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 100.0);
                lrNode.set(AbstractMLNode.FROM, FEATURE);
                lrNode.setProperty(LinearRegressionWithPeriodicityNode.PERIODS_LIST_KEY, Type.DOUBLE_ARRAY, new double[]{5.0});
                lrNode.setProperty(LinearRegressionWithPeriodicityNode.TIME_FEATURE_KEY, Type.STRING, FEATURE);

                RegressionJumpCallback rjc = runThroughDummyDatasetWithPeriodicComponent(lrNode, false, new double[]{5.0}, new double[]{0.0}, new double[]{10.0});
                lrNode.free();
                graph.disconnect(null);

                assertTrue(rjc.sinComponents.length==1);
                assertTrue(rjc.cosComponents.length==1);
                assertTrue(Math.abs(rjc.sinComponents[0] - 10) < eps);
                assertTrue(Math.abs(rjc.cosComponents[0] - 0) < eps);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < eps);
                assertTrue(Math.abs(rjc.intercept - 1) < eps);

                assertTrue(rjc.bufferError+"\t"+rjc.sinComponents[0]+"\t"+rjc.cosComponents[0], rjc.bufferError < eps);
                assertTrue(rjc.l2Reg < eps);
            }
        });

    }

    //@Test
    public void testLondonConsumption() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                lrNode.setProperty(AbstractLinearRegressionNode.BUFFER_SIZE_KEY, Type.INT, consumptionLondon.length);
                lrNode.setProperty(AbstractLinearRegressionNode.LOW_ERROR_THRESH_KEY, Type.DOUBLE, 100.0);//Does not matter. We'll stay in bootstrap mode anyway
                lrNode.setProperty(AbstractLinearRegressionNode.HIGH_ERROR_THRESH_KEY, Type.DOUBLE, 100.0);
                lrNode.set(AbstractMLNode.FROM, FEATURE);
                double periods[] = new double[]{12.0*3600, 24.0*3600, 24.0*7*3600};
                lrNode.setProperty(LinearRegressionWithPeriodicityNode.PERIODS_LIST_KEY, Type.DOUBLE_ARRAY, periods);
                lrNode.setProperty(LinearRegressionWithPeriodicityNode.TIME_FEATURE_KEY, Type.STRING, FEATURE);

                RegressionJumpCallback rjc = new RegressionJumpCallback(new String[]{FEATURE});

                for (int i = 0; i < consumptionLondon.length; i++) {
                    rjc.value = new double[]{timeLondon[i]};
                    rjc.response = consumptionLondon[i];
                    lrNode.jump(i, new Callback<Node>() {
                        @Override
                        public void on(Node result) {
                            rjc.on((AbstractLinearRegressionNode) result);
                        }
                    });
                }
                lrNode.free();
                graph.disconnect(null);

                assertTrue(rjc.sinComponents.length==3);
                assertTrue(rjc.cosComponents.length==3);
                assertTrue(rjc.coefs[0]+"\t"+rjc.intercept+"\t"+rjc.sinComponents[0]+"\t"+rjc.sinComponents[1]+"\t"+rjc.sinComponents[2]+"\t"+
                        rjc.cosComponents[0]+"\t"+rjc.cosComponents[1]+"\t"+rjc.cosComponents[2] + "\t" + rjc.bufferError, Math.abs(rjc.sinComponents[0] + 73.21832523) < eps);
                assertTrue(Math.abs(rjc.sinComponents[1] + 149.92169099) < eps);
                assertTrue(Math.abs(rjc.sinComponents[2] + 64.13438724) < eps);
                assertTrue(Math.abs(rjc.cosComponents[0] - 61.63062964) < eps);
                assertTrue(Math.abs(rjc.cosComponents[1] + 28.33547478) < eps);
                assertTrue(Math.abs(rjc.cosComponents[2] + 28.30261766) < eps);
                assertTrue(Math.abs(rjc.coefs[0] - 0) < 1e-3);
                assertTrue(Math.abs(rjc.intercept - 1201.78) < 1e-2);

                assertTrue( (rjc.bufferError-54316.048971934644) < eps); //Comparing residual sum of squares with Python
                assertTrue(rjc.l2Reg < eps);
            }
        });

    }

    //@Test
    public void testNormalPrecisePhaseShiftToCos() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                standardSettings(lrNode);
                //RegressionJumpCallback rjc = runThroughDummyDatasetWithPeriodicComponent(lrNode, false);
                RegressionJumpCallback rjc = null;
                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 2) < eps);
                assertTrue(Math.abs(rjc.intercept - 1) < eps);
                assertTrue(rjc.bufferError < eps);
                assertTrue(rjc.l2Reg < eps);
            }
        });

    }

    //@Test
    public void testNormalPrecise2() {
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                standardSettings(lrNode);
                //RegressionJumpCallback rjc = runThroughDummyDatasetWithPeriodicComponent(lrNode, true);
                RegressionJumpCallback rjc = null;
                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.intercept + 0.5) < eps);
                assertTrue(Math.abs(rjc.coefs[0] - 0.5) < eps);
                assertTrue(rjc.bufferError < eps);
            }
        });

    }


    //@Test
    public void testSuddenError() {
        //This test fails only on crash. Otherwise, it is just for
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                standardSettings(lrNode);
                //RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, false);
                RegressionJumpCallback rjc = null;

                rjc.value = new double[]{6};
                rjc.response = 1013;
                lrNode.jump(dummyDataset1.length, new Callback<Node>() {
                    @Override
                    public void on(Node result) {
                        rjc.on((AbstractLinearRegressionNode) result);
                    }
                });
                assertTrue(rjc.bootstrapMode);

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0]  - 144.9) < 1);
                assertTrue(Math.abs(rjc.intercept + 332.8) < 1);
                assertTrue(Math.abs(rjc.bufferError - 79349.32) < 20);
            }
        });
    }

    //@Test
    public void testTooLargeRegularization() {
        //This test fails only on crash. Otherwise, it is just for
        final Graph graph = new GraphBuilder().withPlugin(new MLXPlugin()).withScheduler(new NoopScheduler()).build();
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                double resid = 0;
                for (int i = 0; i < dummyDataset1.length; i++) {
                    resid += (dummyDataset1[i][1] - 6) * (dummyDataset1[i][1] - 6);
                }

                LinearRegressionWithPeriodicityNode lrNode = (LinearRegressionWithPeriodicityNode) graph.newTypedNode(0, 0, LinearRegressionWithPeriodicityNode.NAME);
                standardSettings(lrNode);
                lrNode.setProperty(LinearRegressionWithPeriodicityNode.L2_COEF_KEY, Type.DOUBLE, 1000000000.0);

                //RegressionJumpCallback rjc = runThroughDummyDataset(lrNode, false);
                RegressionJumpCallback rjc = null;

                lrNode.free();
                graph.disconnect(null);

                assertTrue(Math.abs(rjc.coefs[0] - 0) < eps);
                assertTrue(Math.abs(rjc.intercept - 6) < eps);
                assertTrue(Math.abs(rjc.bufferError - (resid / 6)) < eps);
            }
        });
    }

}