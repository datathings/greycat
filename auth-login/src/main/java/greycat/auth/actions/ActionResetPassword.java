/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.auth.actions;


import greycat.Action;
import greycat.Constants;
import greycat.Node;
import greycat.TaskContext;
import greycat.auth.IdentityManager;
import greycat.auth.login.LoginManager;
import greycat.struct.Buffer;

/**
 * Created by Gregory NAIN on 26/05/2017.
 */
public class ActionResetPassword implements Action {

    public static final String ACTION_RESET_PASSWORD = "resetPassword";
    private LoginManager identityManager;

    public ActionResetPassword(LoginManager manager) {
        this.identityManager = manager;
    }

    @Override
    public void eval(TaskContext ctx) {
        identityManager.createPasswordUUID((Node) ctx.result().get(0),result -> {
            ctx.continueWith(ctx.wrap(result));
        });
    }

    @Override
    public void serialize(Buffer buffer) {
        buffer.writeString(ACTION_RESET_PASSWORD);
        buffer.writeChar(Constants.TASK_PARAM_OPEN);
        buffer.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public String name() {
        return ACTION_RESET_PASSWORD;
    }
}
