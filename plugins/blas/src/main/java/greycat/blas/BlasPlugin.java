/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.Graph;
import greycat.plugin.Plugin;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.PlainMatrixEngine;

public class BlasPlugin implements Plugin {

    @Override
    public void start(Graph graph) {
        MatrixOps.setDefaultEngine(new HybridMatrixEngine());
    }

    @Override
    public void stop() {
        MatrixOps.setDefaultEngine(new PlainMatrixEngine());
    }
    
}
