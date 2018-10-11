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

import greycat.Type;
import greycat.plugin.TypeDeclaration;
import greycat.plugin.TypeRegistry;
import greycat.utility.HashHelper;

import java.util.HashMap;
import java.util.Map;

class CoreTypeRegistry implements TypeRegistry {

    private final Map<String, TypeDeclaration> backend = new HashMap<String, TypeDeclaration>();
    private final Map<Integer, TypeDeclaration> backend_hash = new HashMap<Integer, TypeDeclaration>();

    CoreTypeRegistry() {
    }

    @Override
    public final synchronized TypeDeclaration getOrCreateDeclaration(String name) {
        TypeDeclaration previous = backend.get(name);
        if (previous == null) {
            previous = new CoreTypeDeclaration(name);
            backend.put(name, previous);
            int hash = HashHelper.hash(name);
            if (!Type.isCustom(hash)) {
                throw new RuntimeException("Lottery winner ! Please change your type name, you conflicted with GreyCat native types");
            }
            backend_hash.put(hash, previous);
        }
        return previous;
    }

    @Override
    public final TypeDeclaration declaration(String name) {
        return backend.get(name);
    }

    @Override
    public final TypeDeclaration declarationByHash(int hash) {
        return backend_hash.get(hash);
    }

}
