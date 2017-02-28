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

    /**
     * @return the netlib character version of this designation, for use with
     * F2J.
     */
    public String netlib() {
        switch (this) {
            case All:
                return "A";
            case Part:
                return "S";
            case Overwrite:
                return "O";
            default:
                return "N";
        }
    }
}