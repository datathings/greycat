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
package greycat.internal.custom;

import greycat.struct.NDManager;

public class IndexManager implements NDManager {

    @Override
    public Object get(long id) {
        return id;
    }

    @Override
    public long updateExistingLeafNode(long oldKey, double[] key, Object valueToInsert) {
        return (long) valueToInsert;
    }

    @Override
    public boolean updateParentsOnExisting() {
        return false;
    }

    @Override
    public boolean updateParentsOnNewValue() {
        return true;
    }

    @Override
    public boolean parentsHaveNodes() {
        return false;
    }

    @Override
    public long getNewLeafNode(double[] key, Object valueToInsert) {
        return (long) valueToInsert;
    }

    @Override
    public long getNewParentNode() {
        return -1;
    }

    @Override
    public long updateParent(long parentkey, double[] key, Object valueToInsert) {
        return parentkey;
    }
}

