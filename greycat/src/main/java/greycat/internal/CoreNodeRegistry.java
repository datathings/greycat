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
package greycat.internal;

import greycat.utility.HashHelper;
import greycat.plugin.NodeDeclaration;
import greycat.plugin.NodeRegistry;

import java.util.HashMap;
import java.util.Map;

class CoreNodeRegistry implements NodeRegistry {

    private final Map<String, NodeDeclaration> backend = new HashMap<String, NodeDeclaration>();
    private final Map<Integer, NodeDeclaration> backend_hash = new HashMap<Integer, NodeDeclaration>();

    CoreNodeRegistry() {
    }

    @Override
    public final synchronized NodeDeclaration getOrCreateDeclaration(String name) {
        NodeDeclaration previous = backend.get(name);
        if (previous == null) {
            previous = new CoreNodeDeclaration(name);
            backend.put(name, previous);
            backend_hash.put(HashHelper.hash(name), previous);
        }
        return previous;
    }

    @Override
    public final NodeDeclaration declaration(String name) {
        return backend.get(name);
    }

    @Override
    public final NodeDeclaration declarationByHash(int hash) {
        return backend_hash.get(hash);
    }
    
}
