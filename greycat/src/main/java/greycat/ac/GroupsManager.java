/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac;

import greycat.Callback;

import java.util.Collection;

/**
 * Created by Gregory NAIN on 06/08/2017.
 */
public interface GroupsManager {

    Collection<Group> all();

    Group get(int gid);

    Group add(Group parent, String name);


    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);

    void loadInitialData(Callback<Boolean> done);

    void printCurrentConfiguration(StringBuilder sb);
}
