package greycat.blasTest;

import greycat.Callback;
import greycat.Graph;
import greycat.GraphBuilder;
import greycat.blas.BlasPlugin;
import greycat.struct.DMatrix;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.PlainMatrixEngine;
import greycat.struct.matrix.VolatileDMatrix;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by assaad on 28/02/2017.
 */
public class GraphTest {
    @Test
    public void testLoading() {
        Graph g = GraphBuilder
                .newBuilder()
                .withPlugin(new BlasPlugin())
                .build();

        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                DMatrix vm = VolatileDMatrix.random(200, 1000, 1234, 1, 2);
                DMatrix vm2 = VolatileDMatrix.random(1000, 300, 2536, -1, 1);

                long start = System.currentTimeMillis();
                DMatrix mult = MatrixOps.multiply(vm, vm2);
                long end = System.currentTimeMillis();
                System.out.println("Blas Matrix Engine took: " + (end - start) + " ms");

                MatrixOps.setDefaultEngine(new PlainMatrixEngine());
                start = System.currentTimeMillis();
                DMatrix mult2 = MatrixOps.multiply(vm, vm2);
                end = System.currentTimeMillis();
                System.out.println("Plain Matrix Engine took: " + (end - start) + " ms");

                double eps = 1e-10;
                Assert.assertTrue(MatrixOps.compare(mult, mult2) < eps);

            }
        });

    }
}
