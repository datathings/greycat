/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat;

/**
 * Created by Gregory NAIN on 03/03/17.
 */
public class EnterpriseGraphBuilder extends GraphBuilder {

    @Override
    public Graph build() {
        if (Validator.validate()) {
            return super.build();
        } else {
            System.exit(-1);
            return null;
        }
    }
}
