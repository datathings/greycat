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
package greycatTest.internal.memory;

import greycat.internal.heap.HeapBuffer;
import org.junit.Assert;
import org.junit.Test;
import greycat.internal.CoreConstants;
import greycat.plugin.MemoryFactory;
import greycat.struct.Buffer;
import greycat.struct.BufferIterator;
import org.junit.runner.notification.RunListener;

import static greycat.Constants.BUFFER_SEP;

public abstract class AbstractBufferTest {

    private MemoryFactory factory;

    private byte[] data = new byte[]{1, 2, 3, 4, 5};
    private byte[] data2 = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};

    public AbstractBufferTest(MemoryFactory factory) {
        this.factory = factory;
    }

    @Test
    public void writeAllTest() {
        Buffer buffer = factory.newBuffer();

        //write not initialized buffer
        buffer.writeAll(data);
        byte[] res = buffer.data();
        Assert.assertEquals(data.length, res.length);

        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(data[i], res[i]);
        }

        //write more data than the capacity
        buffer.writeAll(data2);
        res = buffer.data();
        Assert.assertEquals(data.length + data2.length, res.length);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], res[i]);
        }
        for (int i = 0; i < data2.length; i++) {
            Assert.assertEquals(data2[i], res[i + data.length]);
        }

        //write less data than the capacity
        buffer.writeAll(data);
        res = buffer.data();
        Assert.assertEquals(data.length + data2.length + data.length, res.length);
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], res[i]);
        }
        for (int i = 0; i < data2.length; i++) {
            Assert.assertEquals(data2[i], res[i + data.length]);
        }
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], res[i + data.length + data2.length]);

        }
        buffer.free();

    }

    @Test
    public void iteratorTest() {

        Buffer buffer = factory.newBuffer();

        byte[] one = new byte[3];
        one[0] = (byte) "1".codePointAt(0);
        one[1] = (byte) "2".codePointAt(0);
        one[2] = (byte) "3".codePointAt(0);

        byte[] two = new byte[3];
        two[0] = (byte) "5".codePointAt(0);
        two[1] = (byte) "6".codePointAt(0);
        two[2] = (byte) "7".codePointAt(0);

        byte[] three = new byte[2];
        three[0] = (byte) "8".codePointAt(0);
        three[1] = (byte) "9".codePointAt(0);


        buffer.writeAll(one);
        buffer.write(BUFFER_SEP);
        buffer.writeAll(two);
        buffer.write(BUFFER_SEP);
        buffer.writeAll(three);

        BufferIterator it = buffer.iterator();
        Assert.assertEquals(it.hasNext(), true);

        Buffer view1 = it.next();
        Assert.assertNotNull(view1);
        Assert.assertEquals(view1.length(), 3);
        byte[] view1flat = view1.data();
        view1flat[0] = one[0];
        view1flat[1] = one[1];
        view1flat[2] = one[2];
        Assert.assertEquals(it.hasNext(), true);

        Buffer view2 = it.next();
        Assert.assertNotNull(view2);
        Assert.assertEquals(view2.length(), 3);
        byte[] view2flat = view2.data();
        view2flat[0] = two[0];
        view2flat[1] = two[1];
        view2flat[2] = two[2];
        Assert.assertEquals(it.hasNext(), true);

        Buffer view3 = it.next();
        Assert.assertNotNull(view3);
        Assert.assertEquals(view3.length(), 2);
        byte[] view3flat = view3.data();
        view3flat[0] = three[0];
        view3flat[1] = three[1];
        Assert.assertEquals(it.hasNext(), false);

        buffer.free();
    }

    @Test
    public void iteratorNullTest() {
        Buffer buffer = factory.newBuffer();
        buffer.write(BUFFER_SEP);
        buffer.write(BUFFER_SEP);
        BufferIterator it = buffer.iterator();
        Assert.assertTrue(it.hasNext());
        Buffer view = it.next();
        Assert.assertNotNull(view);
        Assert.assertEquals(view.length(), 0);
        Buffer view2 = it.next();
        Assert.assertNotNull(view2);
        Assert.assertEquals(view2.length(), 0);
        Buffer view3 = it.next();
        Assert.assertNotNull(view3);
        Assert.assertEquals(view3.length(), 0);
        Assert.assertEquals(false, it.hasNext());
        buffer.free();
    }

    @Test
    public void iterator2Test() {
        Buffer buffer = factory.newBuffer();
        byte[] bytes = new byte[]{12, 11, BUFFER_SEP, 87, BUFFER_SEP, 87, 45};
        buffer.writeAll(bytes);
        BufferIterator it = buffer.iterator();
        Assert.assertArrayEquals(new byte[]{12, 11}, it.next().data());
        Assert.assertArrayEquals(new byte[]{87}, it.next().data());
        Assert.assertArrayEquals(new byte[]{87, 45}, it.next().data());
        Assert.assertEquals(false, it.hasNext());
        buffer.free();
    }

    @Test
    public void oneElementBufferTest() {
        Buffer buffer = factory.newBuffer();
        byte[] bytesOneElementBuffer = new byte[]{15, CoreConstants.BUFFER_SEP, 16, CoreConstants.BUFFER_SEP, 17};
        buffer.writeAll(bytesOneElementBuffer);
        BufferIterator itOneElementBuffer = buffer.iterator();
        Assert.assertArrayEquals(new byte[]{15}, itOneElementBuffer.next().data());
        Assert.assertArrayEquals(new byte[]{16}, itOneElementBuffer.next().data());
        Assert.assertArrayEquals(new byte[]{17}, itOneElementBuffer.next().data());
        Assert.assertEquals(false, itOneElementBuffer.hasNext());
        buffer.free();
    }

    @Test
    public void emptyBufferTest() {
        Buffer buffer = factory.newBuffer();
        byte[] bytes = new byte[]{BUFFER_SEP, BUFFER_SEP, BUFFER_SEP, 15, BUFFER_SEP, BUFFER_SEP};
        buffer.writeAll(bytes);
        BufferIterator it = buffer.iterator();
        Buffer next = it.next();
        Assert.assertArrayEquals(new byte[0], next.data());
        Assert.assertEquals(0, next.length());
        next =  it.next();
        Assert.assertArrayEquals(new byte[0], next.data());
        Assert.assertEquals(0, next.length());
        next = it.next();
        Assert.assertArrayEquals(new byte[0], next.data());
        Assert.assertEquals(0, next.length());
        Assert.assertArrayEquals(new byte[]{15}, it.next().data());
        next = it.next();
        Assert.assertArrayEquals(new byte[0], next.data());
        Assert.assertEquals(0, next.length());
        next = it.next();
        Assert.assertArrayEquals(new byte[0], next.data());
        Assert.assertEquals(0, next.length());
        Assert.assertEquals(false, it.hasNext());
        buffer.free();
    }

    @Test
    public void iteratorOnLimitSizeTest() {
        Buffer buffer = factory.newBuffer();
        byte[] bytes = new byte[]{47, 47, 47, 47, 43, 59, 73, 65, 65, 65, 65, 65, 69, 35, 0, 103};
        buffer.writeAll(bytes);
        BufferIterator it = buffer.iterator();
        Assert.assertArrayEquals(new byte[]{47, 47, 47, 47, 43, 59, 73, 65, 65, 65, 65, 65, 69}, it.next().data());
        Assert.assertArrayEquals(new byte[]{0, 103}, it.next().data());
        Assert.assertEquals(false, it.hasNext());
        buffer.free();
    }

    @Test
    public void readTest() {
        Buffer buffer = factory.newBuffer();
        byte[] bytes = new byte[]{BUFFER_SEP, BUFFER_SEP};
        buffer.writeAll(bytes);
        BufferIterator it = buffer.iterator();
        Buffer view = it.next();
        boolean catched = false;
        try {
            view.read(10);
        } catch (ArrayIndexOutOfBoundsException e) {
            catched = true;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            Assert.assertEquals(true, catched);
        }
        buffer.free();
    }

    @Test
    public void readWriteIntTest() {
        Buffer buffer = factory.newBuffer();
        buffer.writeInt(42);
        int res = buffer.readInt(0);
        Assert.assertEquals(42, res);

        buffer = factory.newBuffer();
        buffer.writeInt(-70000);
        res = buffer.readInt(0);
        Assert.assertEquals(-70000, res);
    }

    @Test
    public void readWriteMinIntTest() {
        Buffer buffer = factory.newBuffer();
        buffer.writeInt(-2147483648);
        int res = buffer.readInt(0);
        Assert.assertEquals(-2147483648, res);
    }

    @Test
    public void readWriteMaxIntTest() {
        Buffer buffer = factory.newBuffer();
        buffer.writeInt(2147483647);
        int res = buffer.readInt(0);
        Assert.assertEquals(2147483647, res);
    }

    @Test
    public void readWriteIntAtTest() {
        Buffer buffer = factory.newBuffer();
        buffer.writeInt(21);
        buffer.writeInt(24);
        buffer.writeInt(27);

        int res = buffer.readInt(0);
        Assert.assertEquals(21, res);
        res = buffer.readInt(4);
        Assert.assertEquals(24, res);
        res = buffer.readInt(8);
        Assert.assertEquals(27, res);

        buffer.writeIntAt(-42,4);

        res = buffer.readInt(0);
        Assert.assertEquals(21, res);
        res = buffer.readInt(4);
        Assert.assertEquals(-42, res);
        res = buffer.readInt(8);
        Assert.assertEquals(27, res);
    }


    /**
     * @ignore ts
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void readIntOutOfBoundTest() {
        Buffer buffer = factory.newBuffer();
        buffer.readInt(0);
    }

    /**
     * @ignore ts
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void readIntOutOfBound2Test() {
        Buffer buffer = factory.newBuffer();
        buffer.write((byte)0);
        buffer.readInt(0);
    }
}
