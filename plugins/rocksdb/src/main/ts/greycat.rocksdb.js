/*
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
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var RocksStorage = (function () {
    function RocksStorage() {
    }
    RocksStorage.prototype.get = function (keys, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.put = function (stream, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.putSilent = function (stream, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.remove = function (keys, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.connect = function (graph, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.lock = function (callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.unlock = function (previousLock, callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.disconnect = function (callback) {
        throw new Error("Method not implemented.");
    };
    RocksStorage.prototype.listen = function (synCallback) {
        throw new Error("Method not implemented.");
    };
    return RocksStorage;
}());
exports.RocksStorage = RocksStorage;
var level = require('level-rocksdb');
//# sourceMappingURL=greycat.rocksdb.js.map