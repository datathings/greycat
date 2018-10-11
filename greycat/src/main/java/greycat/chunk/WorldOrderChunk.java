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
package greycat.chunk;

import greycat.NodeListener;
import greycat.struct.LongLongMap;
import greycat.utility.Listeners;

public interface WorldOrderChunk extends Chunk, LongLongMap {

    long magic();

    void lock();

    void unlock();

    void externalLock();

    void externalUnlock();

    long type();

    void setType(long extraValue);

    int listen(NodeListener listener);

    void unlisten(int registrationID);

    Listeners listeners();

}
