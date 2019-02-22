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
package greycat.struct;

public interface LongArray extends ArrayStruct {

    long get(int index);

    void set(int index, long value);

    void initWith(long[] values);

    long[] extract();

    boolean removeElement(long value);

    boolean removeElementbyIndex(int index);

    LongArray addElement(long value);

    void addAll(long[] values);

    boolean insertElementAt(int position, long value);

    boolean replaceElementby(long element, long value);

}
