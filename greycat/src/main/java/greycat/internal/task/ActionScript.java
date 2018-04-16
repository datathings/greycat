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
package greycat.internal.task;

import greycat.Constants;
import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;
import greycat.utility.Tuple;
import greycat.TaskResult;

import javax.script.*;
import java.io.IOException;
import java.io.Writer;

class ActionScript implements Action {

    private final String _script;
    private final boolean _async;

    ActionScript(final String script, final boolean async) {
        this._script = script;
        this._async = async;
    }

    /**
     * {@native ts
     * var isolation = function(this: any, script: string){
     * var variables = ctx.variables();
     * for(var i=0;i<variables.length;i++){this[variables[i].left()] = variables[i].right();}
     * this['print'] = function(v: string){ctx.append(v+'\n');};
     * this['result'] = ctx.result();
     * return eval(script);
     * }
     * var scriptResult = isolation(this._script);
     * if(!this._async){
     * if(scriptResult != null && scriptResult != undefined){
     * ctx.continueWith(ctx.wrap(scriptResult));
     * } else {
     * ctx.continueTask();
     * }
     * }
     * }
     */
    @Override
    public void eval(TaskContext ctx) {
        final ScriptContext scriptCtx = new SimpleScriptContext();
        final Tuple<String, TaskResult>[] variables = ctx.variables();
        for (int i = 0; i < variables.length; i++) {
            scriptCtx.setAttribute(variables[i].left(), variables[i].right(), ScriptContext.ENGINE_SCOPE);
        }
        scriptCtx.setWriter(new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                ctx.append(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
                //noop
            }

            @Override
            public void close() throws IOException {
                //noop
            }
        });
        //protect scope if !async
        scriptCtx.setAttribute("ctx", ctx, ScriptContext.ENGINE_SCOPE);
        scriptCtx.setAttribute("result", ctx.result(), ScriptContext.ENGINE_SCOPE);
        try {
            Object result = TaskHelper.SCRIPT_ENGINE.eval(this._script, scriptCtx);
            if (!_async) {
                if (result != null) {
                    ctx.continueWith(ctx.wrap(result));
                } else {
                    ctx.continueTask();
                }
            }
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void serialize(final Buffer builder) {
        if (_async) {
            builder.writeString(CoreActionNames.ASYNC_SCRIPT);
        } else {
            builder.writeString(CoreActionNames.SCRIPT);
        }
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_script, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }



    @Override
    public final String name() {
        return CoreActionNames.SCRIPT;
    }
}



