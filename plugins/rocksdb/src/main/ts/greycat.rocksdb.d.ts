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

import * as greycat from "@greycat/greycat";
export declare class RocksStorage implements greycat.plugin.Storage {
    get(keys: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void;
    put(stream: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void;
    putSilent(stream: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void;
    remove(keys: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void;
    connect(graph: greycat.Graph, callback: greycat.Callback<boolean>): void;
    lock(callback: greycat.Callback<greycat.struct.Buffer>): void;
    unlock(previousLock: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void;
    disconnect(callback: greycat.Callback<boolean>): void;
    listen(synCallback: greycat.Callback<greycat.struct.Buffer>): void;
}
