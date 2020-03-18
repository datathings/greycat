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
package greycat.utility;

import greycat.struct.BufferIterator;
import greycat.Constants;
import greycat.struct.Buffer;

public class DefaultBufferIterator implements BufferIterator {

    private final Buffer _origin;
    private final long _originSize;
    private long _cursor = -1;

    public DefaultBufferIterator(Buffer p_origin) {
        _origin = p_origin;
        _originSize = p_origin.length();
    }

    @Override
    public final boolean hasNext() {
        return _originSize > 0 && (_cursor + 1) < _originSize;
    }

    @Override
    public final synchronized Buffer next() {
        long previousCursor = _cursor;
        while ((_cursor + 1) < _originSize) {
            _cursor++;
            byte current = _origin.read(_cursor);
            if (current == Constants.BUFFER_SEP) {
                return new BufferView(_origin, previousCursor + 1, _cursor - 1);
            }
        }
        if (previousCursor < _originSize) {
            return new BufferView(_origin, previousCursor + 1, _cursor);
        }
        return null;
    }

    @Override
    public void skip(int length) {
        _cursor += length;
    }
}
