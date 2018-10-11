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
 * Task closure function to select nodes for next action
 */
@FunctionalInterface
public interface TaskFunctionSelect {

    /**
     * Selection function called that specify if a specified node
     * will be in the result set or not
     *
     * @param node node to select or not for next action
     * @param context current context
     * @return true to keep this node for the next action
     */
    boolean select(Node node, TaskContext context);
}
