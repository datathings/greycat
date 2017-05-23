/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.blas;

import greycat.struct.matrix.TransposeType;

public class BlasHelper {

    private static final String TRANSPOSE_TYPE_CONJUCATE = "c";

    private static final String TRANSPOSE_TYPE_NOTRANSPOSE = "n";

    private static final String TRANSPOSE_TYPE_TRANSPOSE = "t";

    public static String transTypeToChar(TransposeType type) {
        if (type == TransposeType.NOTRANSPOSE) {
            return TRANSPOSE_TYPE_NOTRANSPOSE;
        } else if (type == TransposeType.TRANSPOSE) {
            return TRANSPOSE_TYPE_TRANSPOSE;
        }
        return null;
    }

}
