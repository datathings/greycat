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
package greycat.internal;

import greycat.plugin.NodeFactory;
import greycat.plugin.NodeDeclaration;

class CoreNodeDeclaration implements NodeDeclaration {

    private final String _name;
    private NodeFactory _factory;

    CoreNodeDeclaration(String name) {
        _name = name;
    }

    @Override
    public final String name() {
        return _name;
    }

    @Override
    public final NodeFactory factory() {
        return _factory;
    }

    @Override
    public final void setFactory(NodeFactory newFactory) {
        _factory = newFactory;
    }
}
