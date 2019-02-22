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

import greycat.Action;
import greycat.TaskContext;
import greycat.struct.Buffer;

class ActionInject implements Action {

    private final Object _value;

    ActionInject(final Object value) {
        if (value == null) {
            throw new RuntimeException("inputValue should not be null");
        }
        this._value = value;
    }

    @Override
    public void eval(final TaskContext ctx) {
        ctx.continueWith(ctx.wrap(_value).clone());
    }

    @Override
    public void serialize(final Buffer builder) {
        throw new RuntimeException("Remote injection not supported.");
    }

    @Override
    public String toString() {
        return "inject()";
    }

    @Override
    public final String name() {
        return CoreActionNames.INJECT;
    }
}
