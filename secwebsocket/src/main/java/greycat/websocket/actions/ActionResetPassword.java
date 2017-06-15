/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.websocket.actions;

import greycat.*;
import greycat.struct.Buffer;
import greycat.websocket.sec.GCIdentityManager;

/**
 * Created by Gregory NAIN on 26/05/2017.
 */
public class ActionResetPassword implements Action {

    public static final String ACTION_RESET_PASSWORD = "resetPassword";
    private GCIdentityManager identityManager;

    public ActionResetPassword(GCIdentityManager manager) {
        this.identityManager = manager;
    }

    @Override
    public void eval(TaskContext ctx) {
        ctx.continueWith(ctx.wrap(identityManager.createPasswordChangeUUID((Node) ctx.result().get(0))));
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
