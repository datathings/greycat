///
/// Copyright 2017 The GreyCat Authors.  All rights reserved.
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

import * as greycat from '@greycat/greycat';
import * as jre from '@greycat/j2ts-jre';

export class WSClient implements greycat.plugin.Storage {

    private url: string;
    private callbacks;
    private ws: WebSocket = null;
    private graph: greycat.Graph = null;
    private generator: number = 0;

    private static REQ_GET = 0;
    private static REQ_PUT = 1;
    private static REQ_LOCK = 2;
    private static REQ_UNLOCK = 3;
    private static REQ_REMOVE = 4;
    private static REQ_TASK = 5;
    private static RESP_GET = 6;
    private static RESP_PUT = 7;
    private static RESP_REMOVE = 8;
    private static RESP_LOCK = 9;
    private static RESP_UNLOCK = 10;
    private static RESP_TASK = 11;

    private static NOTIFY_UPDATE = 12;
    private static NOTIFY_PRINT = 13;
    private static NOTIFY_PROGRESS = 14;

    private static HEART_BEAT_PING = 15;
    private static HEART_BEAT_PONG = 16;

    private static REQ_TASK_STATS = 17;
    private static RESP_TASK_STATS = 18;

    private static REQ_TASK_STOP = 19;
    private static RESP_TASK_STOP = 20;

    private heartBeatFunctionId;

    constructor(p_url: string) {
        this.url = p_url;
        this.callbacks = {};
    }

    private _listeners: greycat.Callback<greycat.struct.Buffer>[] = [];

    listen(cb: greycat.Callback<greycat.struct.Buffer>) {
        this._listeners.push(cb);
    }

    private heartbeat() {
        const concat = this.graph.newBuffer();
        concat.write(WSClient.HEART_BEAT_PING);
        let flatData = concat.data();
        concat.free();
        this.ws.send(flatData);
    }

    connect(p_graph: greycat.Graph, callback: greycat.Callback<boolean>): void {
        this.graph = p_graph;
        let self = this;

        if (this.ws == null) {
            let selfPointer = this;
            let initialConnection = true;
            this.ws = new WebSocket(this.url);

            this.ws.onmessage = function (msg: MessageEvent) {
                let fr = new FileReader();
                fr.onload = function () {
                    selfPointer.process_rpc_resp(new Int8Array(fr.result));
                };
                fr.readAsArrayBuffer(msg.data);
            };

            this.ws.onclose = function (event: CloseEvent) {
                console.log('Connection closed.', event);
                if (initialConnection) {
                    callback(false);
                }
                self.ws = null;
                clearInterval(selfPointer.heartBeatFunctionId);
            };

            this.ws.onerror = function (event: ErrorEvent) {
                console.error('An error occurred while connecting to server:', event, this.readyState);
                if (initialConnection) {
                    callback(false);
                }
                self.ws = null;
                clearInterval(selfPointer.heartBeatFunctionId);
            };

            this.ws.onopen = function (event: Event) {
                initialConnection = false;
                callback(true);
                selfPointer.heartBeatFunctionId = setInterval(selfPointer.heartbeat.bind(selfPointer), 50 * 1000);
            };
        } else {
            //do nothing
            callback(true);
        }
    }

    disconnect(callback: greycat.Callback<boolean>): void {
        if (this.ws != null) {
            clearInterval(this.heartBeatFunctionId);
            this.ws.close();
            this.ws = null;
            callback(true);
        }
    }

    get(keys: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void {
        this.send_rpc_req(WSClient.REQ_GET, keys, callback);
    }

    put(stream: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        this.send_rpc_req(WSClient.REQ_PUT, stream, callback);
    }

    putSilent(stream: greycat.struct.Buffer, callback: greycat.Callback<greycat.struct.Buffer>): void {
        this.send_rpc_req(WSClient.REQ_PUT, stream, function (b: boolean) {
            callback(null);
        });
    }

    remove(keys: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        this.send_rpc_req(WSClient.REQ_REMOVE, keys, callback);
    }

    lock(callback: greycat.Callback<greycat.struct.Buffer>): void {
        this.send_rpc_req(WSClient.REQ_LOCK, null, callback);
    }

    unlock(previousLock: greycat.struct.Buffer, callback: greycat.Callback<boolean>): void {
        this.send_rpc_req(WSClient.REQ_UNLOCK, previousLock, callback);
    }

    taskStats(callback: greycat.Callback<string>): void {
        this.send_rpc_req(WSClient.REQ_TASK_STATS, null, callback);
    }

    taskStop(id: number, callback: greycat.Callback<boolean>): void {
        let reqBuffer = this.graph.newBuffer();
        greycat.utility.Base64.encodeIntToBuffer(id, reqBuffer);
        this.send_rpc_req(WSClient.REQ_TASK_STOP, reqBuffer, callback);
        reqBuffer.free();
    }

    execute(callback: greycat.Callback<greycat.TaskResult<any>>, task: greycat.Task, prepared: greycat.TaskContext): void {
        let reqBuffer = this.graph.newBuffer();
        let finalGraph = this.graph;
        task.saveToBuffer(reqBuffer);
        let printHash = -1;
        let progressHash = -1;
        if (prepared != null) {
            reqBuffer.write(greycat.Constants.BUFFER_SEP);
            let printHook = prepared.printHook();
            if (printHook != null) {
                printHash = this.generator;
                this.generator = this.generator + 1 % 1000000;
                this.callbacks[printHash] = printHook;
                greycat.utility.Base64.encodeIntToBuffer(printHash, reqBuffer);
            } else {
                printHash = -1;
            }
            reqBuffer.write(greycat.Constants.BUFFER_SEP);
            let progressHook = prepared.progressHook();
            if (progressHook != null) {
                progressHash = this.generator;
                this.generator = this.generator + 1 % 1000000;
                this.callbacks[progressHash] = progressHook;
                greycat.utility.Base64.encodeIntToBuffer(progressHash, reqBuffer);
            } else {
                progressHash = -1;
            }
            reqBuffer.write(greycat.Constants.BUFFER_SEP);
            prepared.saveToBuffer(reqBuffer);
        }
        let finalCB = callback;
        let finalCallbacks = this.callbacks;
        let finalPrintHash = printHash;
        let finalProgressHash = progressHash;
        this.send_rpc_req(WSClient.REQ_TASK, reqBuffer, function (resultBuffer) {
            if (finalPrintHash != -1) {
                delete finalCallbacks[finalPrintHash];
            }
            if (finalProgressHash != -1) {
                delete finalCallbacks[finalProgressHash];
            }
            reqBuffer.free();
            let collector = new greycat.utility.L3GMap<jre.java.util.List<greycat.utility.Tuple<any[], number>>>(true);
            let baseTaskResult: greycat.base.BaseTaskResult<any> = new greycat.base.BaseTaskResult<any>(null, false);
            baseTaskResult.load(resultBuffer, 0, finalGraph, collector);
            finalGraph.remoteNotify(baseTaskResult.notifications());
            baseTaskResult.loadRefs(finalGraph, collector, function (b: boolean) {
                resultBuffer.free();
                finalCB(baseTaskResult);
            });
        });
    }

    /*
     private static process_notify(buffer: greycat.struct.Buffer, graph: greycat.Graph) {
     if (buffer != null) {
     let type = 0;
     let world = 0;
     let time = 0;
     let id = 0;
     let hash = 0;
     let step = 0;
     let cursor = 0;
     let previous = 0;
     let end = buffer.length();
     while (cursor < end) {
     let current = buffer.read(cursor);
     if (current == greycat.Constants.KEY_SEP) {
     switch (step) {
     case 0:
     type = greycat.utility.Base64.decodeToIntWithBounds(buffer, previous, cursor);
     break;
     case 1:
     world = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 2:
     time = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 3:
     id = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 4:
     hash = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     }
     previous = cursor + 1;
     if (step == 4) {
     step = 0;
     let ch: greycat.chunk.Chunk = graph.space().getAndMark(type, world, time, id);
     if (ch != null) {
     ch.sync(hash);
     graph.space().unmark(ch.index());
     }
     } else {
     step++;
     }
     }
     cursor++;
     }
     switch (step) {
     case 0:
     type = greycat.utility.Base64.decodeToIntWithBounds(buffer, previous, cursor);
     break;
     case 1:
     world = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 2:
     time = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 3:
     id = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     case 4:
     hash = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
     break;
     }
     if (step == 4) {
     //invalidate
     let ch: greycat.chunk.Chunk = graph.space().getAndMark(type, world, time, id);
     if (ch != null) {
     ch.sync(hash);
     graph.space().unmark(ch.index());
     }
     }
     }
     }*/

    process_rpc_resp(payload: Int8Array) {
        let payloadBuf = this.graph.newBuffer();
        payloadBuf.writeAll(payload);
        let it = payloadBuf.iterator();
        let codeView = it.next();
        if (codeView != null && codeView.length() != 0) {
            let firstCode = codeView.read(0);
            switch (firstCode) {
                case WSClient.RESP_TASK_STATS:
                    let callbackStatsCodeView = it.next();
                    let statsContentView = it.next();
                    let callbackStatsCode = greycat.utility.Base64.decodeToIntWithBounds(callbackStatsCodeView, 0, callbackStatsCodeView.length());
                    let statsContent = greycat.utility.Base64.decodeToStringWithBounds(statsContentView, 0, statsContentView.length());
                    let statsCallback = this.callbacks[callbackStatsCode];
                    delete this.callbacks[callbackStatsCode];
                    if (statsCallback) {
                        statsCallback(statsContent);
                    } else {
                        console.error('Received a REQ_TASK_STATS callback with unknown hash: ' + callbackStatsCode, this.callbacks);
                    }
                    break;
                case WSClient.RESP_TASK_STOP:
                    let stopCodeView = it.next();
                    let stopCode = greycat.utility.Base64.decodeToIntWithBounds(stopCodeView, 0, stopCodeView.length());
                    let stopCallback = this.callbacks[stopCode];
                    if (stopCallback) {
                        delete this.callbacks[stopCallback];
                        stopCallback(true);
                    } else {
                        console.error('Received a RESP_TASK_STOP callback with unknown hash: ' + stopCode, this.callbacks);
                    }
                    break;
                case WSClient.HEART_BEAT_PING: {
                    const concat = this.graph.newBuffer();
                    concat.write(WSClient.HEART_BEAT_PONG);
                    concat.writeString('ok');
                    let flatData = concat.data();
                    concat.free();
                    this.ws.send(flatData);
                }
                    break;
                case WSClient.HEART_BEAT_PONG: {//Ignore
                }
                    break;
                case WSClient.NOTIFY_UPDATE:
                    while (it.hasNext()) {
                        this.graph.remoteNotify(it.next());
                    }
                    //optimize this
                    if (this._listeners.length > 0) {
                        const notifyBuffer = this.graph.newBuffer();
                        notifyBuffer.writeAll(payloadBuf.slice(1, payloadBuf.length() - 1));
                        for (let i = 0; i < this._listeners.length; i++) {
                            this._listeners[i](notifyBuffer);
                        }
                        notifyBuffer.free();
                    }
                    break;
                case WSClient.NOTIFY_PRINT:
                    let callbackPrintCodeView = it.next();
                    let printContentView = it.next();
                    let callbackPrintCode = greycat.utility.Base64.decodeToIntWithBounds(callbackPrintCodeView, 0, callbackPrintCodeView.length());
                    let printContent = greycat.utility.Base64.decodeToStringWithBounds(printContentView, 0, printContentView.length());
                    let printCallback = this.callbacks[callbackPrintCode];
                    if (printCallback) {
                        printCallback(printContent);
                    } else {
                        console.error('Received a NOTIFY_PRINT callback with unknown hash: ' + callbackPrintCode, this.callbacks);
                    }
                    break;
                case WSClient.NOTIFY_PROGRESS:
                    let progressCallbackCodeView = it.next();
                    let progressCallbackView = it.next();
                    let progressCallbackCode = greycat.utility.Base64.decodeToIntWithBounds(progressCallbackCodeView, 0, progressCallbackCodeView.length());
                    let report = new greycat.internal.task.CoreProgressReport();
                    report.loadFromBuffer(progressCallbackView);
                    let progressHook = this.callbacks[progressCallbackCode];
                    if (progressHook != null) {
                        progressHook(report);
                    }
                    break;
                case WSClient.RESP_LOCK:
                case WSClient.RESP_GET:
                case WSClient.RESP_TASK:
                    let callBackCodeView = it.next();
                    let callbackCode = greycat.utility.Base64.decodeToIntWithBounds(callBackCodeView, 0, callBackCodeView.length());
                    let resolvedCallback = this.callbacks[callbackCode];
                    if (resolvedCallback) {
                        let newBuf = this.graph.newBuffer();//will be free by the core
                        let isFirst = true;
                        while (it.hasNext()) {
                            if (isFirst) {
                                isFirst = false;
                            } else {
                                newBuf.write(greycat.Constants.BUFFER_SEP);
                            }
                            newBuf.writeAll(it.next().data());
                        }
                        delete this.callbacks[callbackCode];
                        resolvedCallback(newBuf);
                    } else {
                        console.error('Received a RESP_TASK callback with unknown hash: ' + callbackPrintCode, this.callbacks);
                    }
                    break;
                default:
                    let genericCodeView = it.next();
                    let genericCode = greycat.utility.Base64.decodeToIntWithBounds(genericCodeView, 0, genericCodeView.length());
                    let genericCallback = this.callbacks[genericCode];
                    if (genericCallback) {
                        delete this.callbacks[genericCode];
                        genericCallback(true);
                    } else {
                        console.error('Received a generic callback with unknown hash: ' + callbackPrintCode, this.callbacks);
                    }
            }
        }
    }

    send_rpc_req(code: number, payload: greycat.struct.Buffer, callback: greycat.Callback<any>): void {
        if (this.ws == null) {
            throw new Error('Not connected!');
        }
        let buffer: greycat.struct.Buffer = this.graph.newBuffer();
        buffer.write(code);
        buffer.write(greycat.Constants.BUFFER_SEP);
        let hash = this.generator;
        this.generator = this.generator + 1 % 1000000;
        this.callbacks[hash] = callback;
        greycat.utility.Base64.encodeIntToBuffer(hash, buffer);
        if (payload != null) {
            buffer.write(greycat.Constants.BUFFER_SEP);
            buffer.writeAll(payload.data());
        }
        let flatData = buffer.data();
        buffer.free();
        this.ws.send(flatData);
    }

}

/*
 class ChunkKey {
 type: number;

 world: number;

 time: number;

 id: number;

 static build(buffer: greycat.struct.Buffer): ChunkKey {
 let result = new ChunkKey();
 var cursor = 0;
 let length = buffer.length();
 var previous = 0;
 var index = 0;
 while (cursor < length) {
 let current = buffer.read(cursor);
 if (current == greycat.Constants.KEY_SEP) {
 switch (index) {
 case 0:
 result.type = greycat.utility.Base64.decodeToIntWithBounds(buffer, previous, cursor);
 break;
 case 1:
 result.world = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 case 2:
 result.time = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 case 3:
 result.id = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 }
 index++;
 previous = cursor + 1;
 }
 cursor++;
 }
 switch (index) {
 case 0:
 result.type = greycat.utility.Base64.decodeToIntWithBounds(buffer, previous, cursor);
 break;
 case 1:
 result.world = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 case 2:
 result.time = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 case 3:
 result.id = greycat.utility.Base64.decodeToLongWithBounds(buffer, previous, cursor);
 break;
 }
 return result;
 };
 }
 */



