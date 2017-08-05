/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.auth;

import greycat.*;
import greycat.struct.Buffer;

/**
 * Created by Gregory NAIN on 26/05/2017.
 */
public class ActionResetPassword implements Action {

    public static final String ACTION_RESET_PASSWORD = "resetPassword";
    private BaseAuthenticationManager authManager;

    public ActionResetPassword(BaseAuthenticationManager manager) {
        this.authManager = manager;
    }

    @Override
    public void eval(TaskContext ctx) {
        if(ctx.result().size() == 0) {
            ctx.append("No user node in result. Could not create reset password token.");
            ctx.continueTask();
        } else {
            DeferCounter counter = ctx.graph().newCounter(ctx.result().size());
            TaskResult newResult = ctx.newResult();
            for(int i = 0; i < ctx.result().size(); i++) {
                authManager.createPasswordChangeAuthKey(((Node)ctx.result().get(i)).id(), result -> {
                   newResult.add(result);
                });
            }
            counter.then(() -> {
                ctx.continueWith(newResult);
            });
        }

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
