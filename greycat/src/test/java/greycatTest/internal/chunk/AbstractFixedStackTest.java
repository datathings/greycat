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
package greycatTest.internal.chunk;

import greycat.chunk.Stack;
import org.junit.Assert;

public class AbstractFixedStackTest {

    static public final int CAPACITY = 15;

    public void test(Stack stack) {
        // stack is initially full, dequeue until empty
        for (int i = 0; i < CAPACITY; i++) {
            Assert.assertTrue(stack.dequeueTail() == i);
        }
        // insert some values again
        Assert.assertTrue(stack.enqueue(0));
        Assert.assertTrue(stack.enqueue(1));
        Assert.assertTrue(stack.enqueue(2));
        Assert.assertTrue(stack.enqueue(3));
        Assert.assertTrue(stack.enqueue(4));
        // dequeue tail
        Assert.assertTrue(stack.dequeueTail() == 0);
        // enqueue
        Assert.assertTrue(stack.enqueue(5));
        // dequeue index
        Assert.assertTrue(stack.dequeue(2));
        // dequeue tail
        Assert.assertTrue(stack.dequeueTail() == 1);
        Assert.assertTrue(stack.dequeueTail() == 3);
        // dequeue invalid index
        Assert.assertFalse(stack.dequeue(2));
        Assert.assertFalse(stack.dequeue(1));
        Assert.assertFalse(stack.dequeue(0));
        // dequeue valid index
        Assert.assertTrue(stack.dequeue(4));
        // dequeue tail
        Assert.assertTrue(stack.dequeueTail() == 5);

    }
}
