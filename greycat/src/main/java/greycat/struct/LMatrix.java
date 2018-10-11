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
package greycat.struct;

public interface LMatrix {

    LMatrix init(int rows, int columns);

    LMatrix fill(long value);

    LMatrix fillWith(long[] values);

    LMatrix fillWithRandom(long min, long max, long seed);

    int rows();

    int columns();

    long[] column(int i);

    long get(int rowIndex, int columnIndex);

    LMatrix set(int rowIndex, int columnIndex, long value);

    LMatrix add(int rowIndex, int columnIndex, long value);

    LMatrix appendColumn(long[] newColumn);

    long[] data();

    int leadingDimension();

    long unsafeGet(int index);

    LMatrix unsafeSet(int index, long value);

}
