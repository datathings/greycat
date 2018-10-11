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
package greycat;

import greycat.struct.Buffer;

public interface TaskResult<A> {

    TaskResultIterator iterator();

    A get(int index);

    TaskResult<A> set(int index, A input);

    TaskResult<A> allocate(int index);

    TaskResult<A> add(A input);

    TaskResult<A> clear();

    TaskResult<A> clone();

    void free();

    int size();

    Object[] asArray();

    //void saveToBuffer(Buffer buffer);

    Exception exception();

    String output();

    TaskResult<A> setException(Exception e);

    TaskResult<A> setOutput(String output);

    TaskResult<A> setNotifications(Buffer buf);

    TaskResult<A> fillWith(TaskResult<A> source);

    void saveToBuffer(Buffer buffer);

    Buffer notifications();

}
