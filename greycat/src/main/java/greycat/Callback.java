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

/**
 * <p>Generic structure to define tasks to be executed when an asynchronous result is released.</p>
 * In Java 8, Typescript and JavaScript, this structure can be replaced by a closure.
 *
 * @param <A> The type of the expected result.
 */
@FunctionalInterface
public interface Callback<A> {

    /**
     * This method is called when an asynchronous result is delivered.
     *
     * @param result The expected result.
     */
    void on(A result);

}
