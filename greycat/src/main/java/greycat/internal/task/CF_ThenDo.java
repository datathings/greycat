/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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

import greycat.ActionFunction;
import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;

class CF_ThenDo implements Action {

    private final ActionFunction _wrapped;

    CF_ThenDo(final ActionFunction p_wrapped) {
        if (p_wrapped == null) {
            throw new RuntimeException("action should not be null");
        }
        this._wrapped = p_wrapped;
    }

    @Override
    public void eval(final TaskContext ctx) {
        //execute wrapped task but does not call the next method of the wrapped context
        //this allow to have exactly one call to the Context.next method
        _wrapped.eval(ctx);
    }

    @Override
    public String toString() {
        return "then()";
    }

    @Override
    public void serialize(final Buffer builder) {
        throw new RuntimeException("Not managed yet!");
    }

    @Override
    public final String name() {
        return CoreActionNames.THEN_DO;
    }

}
