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

import greycat.struct.EStructArray;

/**
 * Type plugin allows to propose alternative implementations for basic Types.<br>
 * Custom types are wrapper of EStructArray and should leverage them as backend.
 */
@FunctionalInterface
public interface TypeFactory {

    /**
     * Create a new Node
     *
     * @param backend backend
     * @return newly created Type object
     */
    Object wrap(EStructArray backend);

}
