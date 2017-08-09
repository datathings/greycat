/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import greycat.Callback;
import greycat.ac.groups.BaseGroup;

import java.util.Collection;

/**
 * Created by Gregory NAIN on 05/08/2017.
 */
public interface SecurityManager {

    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);

    void loadInitialData(Callback<Boolean> done);
}
