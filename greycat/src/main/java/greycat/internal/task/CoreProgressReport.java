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

/**
 * Created by Gregory NAIN on 14/04/17.
 */
public class CoreProgressReport implements TaskProgressReport {

    private String _comment;
    private double _progress;
    private String _actionPath;
    private String _actionSumPath;

    public CoreProgressReport() {
    }

    @Override
    public double progress() {
        return this._progress;
    }

    @Override
    public String actionPath() {
        return this._actionPath;
    }

    @Override
    public String actionSumPath() {
        return this._actionSumPath;
    }

    @Override
    public String comment() {
        return this._comment;
    }


    public CoreProgressReport setComment(String _comment) {
        this._comment = _comment;
        return this;
    }

    public CoreProgressReport setActionPath(String path) {
        this._actionPath = path;
        return this;
    }

    public CoreProgressReport setSumPath(String sumPath) {
        this._actionSumPath = sumPath;
        return this;
    }

    public CoreProgressReport setProgress(double progress) {
        this._progress = progress;
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
                        _actionPath = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                        index++;
                        break;
                    case 1:
                        _actionSumPath = Base64.decodeToStringWithBounds(buffer, previous, cursor);
                        index++;
                        break;
                    case 2:
                        _progress = Base64.decodeToDoubleWithBounds(buffer, previous, cursor);
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
        Base64.encodeStringToBuffer(this._actionPath, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        Base64.encodeStringToBuffer(this._actionSumPath, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        Base64.encodeDoubleToBuffer(this._progress, buffer);
        buffer.write(CoreConstants.CHUNK_ESEP);
        if (this._comment != null) {
            Base64.encodeStringToBuffer(this._comment, buffer);
        }

    }

    public String toString() {
        return this._actionPath + "\t" + this._actionSumPath + "\t" + _progress + ":" + _comment;
    }

}
