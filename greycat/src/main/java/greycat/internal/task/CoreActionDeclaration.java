/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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

import greycat.plugin.ActionFactory;
import greycat.plugin.ActionDeclaration;

class CoreActionDeclaration implements ActionDeclaration {

    private ActionFactory _factory = null;
    private int[] _params = null;
    private String _description = null;
    private final String _name;

    CoreActionDeclaration(String name) {
        this._name = name;
    }

    @Override
    public final ActionFactory factory() {
        return _factory;
    }

    @Override
    public final ActionDeclaration setFactory(ActionFactory factory) {
        this._factory = factory;
        return this;
    }

    @Override
    public final int[] params() {
        return _params;
    }

    /**
     * @native ts
     * this._params = Int32Array.from(params);
     * return this;
     */
    @Override
    public final ActionDeclaration setParams(int... params) {
        this._params = params;
        return this;
    }

    @Override
    public final String description() {
        return _description;
    }

    @Override
    public final ActionDeclaration setDescription(String description) {
        this._description = description;
        return this;
    }

    @Override
    public final String name() {
        return _name;
    }

}
