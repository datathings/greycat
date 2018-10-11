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

class CoreTaskReader {

    private final String flat;

    private final int offset;

    private int _end = -1;

    CoreTaskReader(String p_flat, int p_offset) {
        this.flat = p_flat;
        this.offset = p_offset;
    }

    public int available() {
        return flat.length() - offset;
    }

    public char charAt(int cursor) {
        return flat.charAt(offset + cursor);
    }

    public String extract(int begin, int end) {
        return flat.substring(offset + begin, offset + end);
    }

    public void markend(int p_end) {
        this._end = p_end;
    }

    public int end() {
        if (_end == -1) {
            return available();
        } else {
            return _end;
        }
    }

    public CoreTaskReader slice(int cursor) {
        return new CoreTaskReader(flat, offset + cursor);
    }

}
