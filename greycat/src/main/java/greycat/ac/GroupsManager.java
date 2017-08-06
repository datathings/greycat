package greycat.ac;

import java.util.Collection;

/**
 * Created by Gregory NAIN on 06/08/2017.
 */
public interface GroupsManager {

    Collection<Group> all();

    Group get(int gid);

    Group add(Group parent, String name);
}
