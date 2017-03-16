/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;


/**
 * The job the singular value solvers are to do. This only limits which singular
 * vectors are computed, all the singular values are always computed
 */
enum JobSVD {
    /**
     * Compute all of the singular vectors
     */
    All,

    /**
     * Do not compute any singular vectors
     */
    None,

    /**
     * Overwrite passed data. For an <code>M*N</code> matrix, this either
     * overwrites the passed matrix with as many singular vectors as there is
     * room for. Details depend on the actual algorithm
     */
    Overwrite,

    /**
     * Compute parts of the singular vectors. For an <code>M*N</code> matrix,
     * this computes <code>getMin(M,N)</code> singular vectors
     */
    Part;
}