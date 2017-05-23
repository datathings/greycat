/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.Graph;
import greycat.Validator;
import greycat.plugin.Plugin;
import greycat.struct.matrix.MatrixOps;
import greycat.struct.matrix.PlainMatrixEngine;

public class BlasPlugin implements Plugin {

    /**
     * {@native ts
     * greycat.struct.matrix.MatrixOps.setDefaultEngine(new greycatBlas.blas.HybridMatrixEngine());
     * }
     */
    @Override
    public void start(Graph graph) {
        if (Validator.validate()) {
            MatrixOps.setDefaultEngine(new HybridMatrixEngine());
        } else {
            System.exit(-1);
        }

    }

    @Override
    public void stop() {
        MatrixOps.setDefaultEngine(new PlainMatrixEngine());
    }
    
}
