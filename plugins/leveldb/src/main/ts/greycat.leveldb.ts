///
/// Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
/// <p>
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
/// <p>
/// http://www.apache.org/licenses/LICENSE-2.0
/// <p>
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import * as greycat from 'greycat';
import level from 'level';
class LevelStorage implements greycat.plugin.Storage {

    db;

    constructor(path: string) {
        this.db = level(path);
    }

    get(keys: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void {

    }

    put(stream: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        let batch = this.db.lookupBatch();
        let it = stream.iterator();
        while (it.hasNext()) {
            let keyView = it.next();
            let valueView = it.next();
            if (valueView != null) {
                batch.put(keyView.data(), valueView.data());
            }
        }
        batch.write(function () {
            callback(true);
        });
    }

    remove(keys: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {

    }

    connect(graph: greycat.Graph, callback: greycat.Callback<boolean>): void {
    }

    lock(callback: greycat.Callback<greycat.struct.Buffer>): void {
    }

    unlock(previousLock: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
    }

    disconnect(callback: greycat.Callback<boolean>): void {
    }

}
export = LevelStorage;