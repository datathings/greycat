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

public interface SuperTimeTreeChunk extends Chunk {

    void insert(long key, long value);

    long previousOrEqual(long key);

    void range(long startKey, long endKey, long maxElements, SuperTreeWalker walker);

    long magic();

    long subTreeCapacity();

    long previous(long key);

    long next(long key);

    long end();

    void setEnd(long v);

    int size();

    long timeSensitivity();

    void setTimeSensitivity(long v);

    long timeSensitivityOffset();

    void setTimeSensitivityOffset(long v);

    long lastKey();

    long lastValue();

    void setLastValue(long v);

}