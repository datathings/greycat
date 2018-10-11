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

public interface StringArray extends ArrayStruct {

    String get(int index);

    void set(int index, String value);

    void initWith(String[] values);

    String[] extract();

    boolean removeElement(String value);

    boolean removeElementbyIndex(int index);

    StringArray addElement(String value);

    boolean insertElementAt(int position, String value);

    boolean replaceElementby(String element, String value);

    void addAll(String[] values);

}
