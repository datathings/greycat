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
package greycat.base;

import greycat.Container;
import greycat.struct.*;

public class BaseCustomTypeSingle extends BaseCustomType {

    protected static final int DEF_NODE = 0;

    public BaseCustomTypeSingle(EStructArray p_backend) {
        super(p_backend);
        if (p_backend.size() == 0) {
            p_backend.newEStruct();
        }
    }

    @Override
    public Object getAt(int index) {
        return _backend.estruct(DEF_NODE).getAt(index);
    }

    @Override
    public final Object getRawAt(int index) {
        return _backend.estruct(DEF_NODE).getRawAt(index);
    }

    @Override
    public final Object getTypedRawAt(int index, int type) {
        return _backend.estruct(DEF_NODE).getTypedRawAt(index, type);
    }

    @Override
    public int typeAt(int index) {
        return _backend.estruct(DEF_NODE).typeAt(index);
    }

    @Override
    public Container setAt(int index, int type, Object value) {
        return _backend.estruct(DEF_NODE).setAt(index, type, value);
    }

    @Override
    public final Container removeAt(int index) {
        return _backend.estruct(DEF_NODE).removeAt(index);
    }

    @Override
    public final Object getOrCreateAt(int index, int type) {
        return _backend.estruct(DEF_NODE).getOrCreateAt(index, type);
    }

    @Override
    public final <A> A getAtWithDefault(int key, A defaultValue) {
        return _backend.estruct(DEF_NODE).getAtWithDefault(key, defaultValue);
    }

    @Override
    public final int[] attributeIndexes() {
        return _backend.estruct(DEF_NODE).attributeIndexes();
    }

}
