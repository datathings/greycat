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
package greycat.internal.task;

import greycat.Constants;
import greycat.TaskProgressReport;
import greycat.internal.CoreConstants;
import greycat.struct.Buffer;
import greycat.utility.Base64;
import greycat.TaskProgressType;

/**
 * Created by Gregory NAIN on 14/04/17.
 */
public class CoreProgressReport implements TaskProgressReport {

    private byte _type;
    private int _index;
    private int _total;
    private String _comment;

    public CoreProgressReport(){
    }

    @Override
    public byte type() {
        return this._type;
    }

    @Override
    public int index() {
        return this._index;
    }

    @Override
    public int total() {
        return this._total;
    }

    @Override
    public String comment() {
        return this._comment;
    }

    public CoreProgressReport setType(byte _type) {
        this._type = _type;
        return this;
    }

    public CoreProgressReport setIndex(int _index) {
        this._index = _index;
        return this;
    }

    public CoreProgressReport setTotal(int _total) {
        this._total = _total;
        return this;
    }

    public CoreProgressReport setComment(String _comment) {
        this._comment = _comment;
        return this;
    }

    public void loadFromBuffer(Buffer buffer) {

        int cursor = 0;
        int previous = 0;
        int index = 0;
        while (cursor < buffer.length()) {
            byte current = buffer.read(cursor);
            if (current == Constants.CHUNK_ESEP || cursor + 1 == buffer.length()) {
                switch (index) {
                    case 0:
                        _type = (byte)Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        index++;
                        break;
                    case 1:
                        _index = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        index++;
                        break;
                    case 2:
                        _total = Base64.decodeToIntWithBounds(buffer, previous, cursor);
                        index++;
                        break;
                    case 3:
                        if (cursor != previous) {
                            _comment = Base64.decodeToStringWithBounds(buffer, previous, cursor + 1);
                        }
                        break;
                }
                previous = cursor + 1;
            }
            cursor++;
        }
    }

    public void saveToBuffer(Buffer buffer) {
        Base64.encodeIntToBuffer(this._type, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        Base64.encodeIntToBuffer(this._index, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        Base64.encodeIntToBuffer(this._total, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        if (this._comment != null) {
            Base64.encodeStringToBuffer(this._comment, buffer);
        }

    }

    public String toString() {
        return TaskProgressType.toString(_type) + "\t\t" + _index + "/" + _total + "\t" + _comment;
    }

}
