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
package greycat.memory;

public class OffHeapVolatileContainer implements OffHeapContainer {
    private long indexedAddr;

    public OffHeapVolatileContainer() {
        indexedAddr = OffHeapConstants.NULL_PTR;
    }

    @Override
    public long addrByIndex(long elemIndex) {
        return indexedAddr;
    }

    @Override
    public void setAddrByIndex(long elemIndex, long newAddr) {
        indexedAddr = newAddr;
    }

    @Override
    public void lock() {
        // do nothing
    }

    @Override
    public void unlock() {
        // do nothing
    }

    @Override
    public void declareDirty() {
        // do nothing
    }
}
