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

public interface IMatrix {

    IMatrix init(int rows, int columns);

    IMatrix fill(int value);

    IMatrix fillWith(int[] values);

    IMatrix fillWithRandom(int min, int max, int seed);

    int rows();

    int columns();

    int[] column(int i);

    int get(int rowIndex, int columnIndex);

    IMatrix set(int rowIndex, int columnIndex, int value);

    IMatrix add(int rowIndex, int columnIndex, int value);

    IMatrix appendColumn(int[] newColumn);

    int[] data();

    int leadingDimension();

    int unsafeGet(int index);

    IMatrix unsafeSet(int index, int value);

}
