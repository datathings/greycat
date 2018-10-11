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
package greycat.plugin;

import greycat.Graph;
import greycat.struct.Buffer;
import greycat.Callback;
import greycat.utility.Tuple;

/**
 * Storage defines the interface any storage solution must comply with to be plugged to GreyCat.
 */
public interface Storage {

    /**
     * Used to retrieve objects from the storage.<br>
     * The {@code keys} buffer is a sequential list of keys, assembled in a flat buffer and separator by BUFFER_SEP<br>
     *
     * @param keys     The buffer of keys as specified above.
     * @param callback Is called when all objects are collected.
     *                 The size of the array in parameter is always 1/3 of the keys array size.
     *                 Objects in the result array are in the same order as the keys.
     */
    void get(Buffer keys, Callback<Buffer> callback);

    /**
     * Used to push objects to the storage.<br>
     *
     * @param stream   The objects to store organized as a list of elements, assembled in a flat buffer.
     * @param callback Called when the operation is complete. The parameter will be true if the operation succeeded, false otherwise.
     */
    void put(Buffer stream, Callback<Boolean> callback);

    /**
     * Used to push objects to the storage without notification.<br>
     *
     * @param stream   The objects to store organized as a list of elements, assembled in a flat buffer.
     * @param callback Called when the operation is complete. The parameter will be true if the operation succeeded, false otherwise.
     */
    void putSilent(Buffer stream, Callback<Buffer> callback);

    /**
     * Called to remove objects from the storage.
     * The {@code keys} array is a sequential list of &lt;world, timepoint, id&gt; tuples organized as follows:<br>
     * Say you wanna remove objects &lt;1, 2, 3&gt; and &lt;1, 5, 6&gt;, the array will be: [1,2,3,1,5,6]
     *-
     * @param keys     The array of keys as specified above.
     * @param callback Is called when all keys are deleted.
     */
    void remove(Buffer keys, Callback<Boolean> callback);

    /**
     * Connects the storage
     *
     * @param graph    Graph this storage is attached to
     * @param callback Called when the connection process is complete. The parameter will be true if the operation succeeded, false otherwise.
     */
    void connect(Graph graph, Callback<Boolean> callback);

    /**
     * Lock a reserved number (to be used as a prefix).
     *
     * @param callback Called when the connection process is complete. The parameter will be new lock, null in case of error.
     */
    void lock(Callback<Buffer> callback);

    /**
     * Unlock a previously reserved lock
     *
     * @param previousLock the previously reserved lock number
     * @param callback     Called when the connection process is complete. The parameter will be true if the operation succeeded, false otherwise.
     */
    void unlock(Buffer previousLock, Callback<Boolean> callback);

    /**
     * Disconnects the storage
     *
     * @param callback Called when the disconnection process is complete. The parameter will be true if the operation succeeded, false otherwise.
     */
    void disconnect(Callback<Boolean> callback);

    /**
     * Listen for all updates
     *
     * @param synCallback Called synchronously when update process is performed
     */
    void listen(Callback<Buffer> synCallback);

}