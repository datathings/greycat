/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat;


import java.util.Map;

/**
 * Created by Gregory NAIN on 18/07/2017.
 */
public interface AuthenticationManager {

    void verifyCredentials(Map<String, String> credentials, Callback<Long> callback);

    void createPasswordChangeAuthKey(long uid, Callback<String> callback);

    void resetPassword(String authKey, String newPass, Callback<Integer> callback);

    void load(Callback<Boolean> done);

    void save(Callback<Boolean> done);

    void loadInitialData(Callback<Boolean> done);
}
