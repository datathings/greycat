/**
 * Copyright 2017 The GreyCat Authors.  All rights reserved.
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
package greycat.utility;

import greycat.struct.Buffer;

/**
 * {@native ts
 * private static dictionary : number[] = ['A'.charCodeAt(0), 'B'.charCodeAt(0), 'C'.charCodeAt(0), 'D'.charCodeAt(0), 'E'.charCodeAt(0), 'F'.charCodeAt(0), 'G'.charCodeAt(0), 'H'.charCodeAt(0), 'I'.charCodeAt(0), 'J'.charCodeAt(0), 'K'.charCodeAt(0), 'L'.charCodeAt(0), 'M'.charCodeAt(0), 'N'.charCodeAt(0), 'O'.charCodeAt(0), 'P'.charCodeAt(0), 'Q'.charCodeAt(0), 'R'.charCodeAt(0), 'S'.charCodeAt(0), 'T'.charCodeAt(0), 'U'.charCodeAt(0), 'V'.charCodeAt(0), 'W'.charCodeAt(0), 'X'.charCodeAt(0), 'Y'.charCodeAt(0), 'Z'.charCodeAt(0), 'a'.charCodeAt(0), 'b'.charCodeAt(0), 'c'.charCodeAt(0), 'd'.charCodeAt(0), 'e'.charCodeAt(0), 'f'.charCodeAt(0), 'g'.charCodeAt(0), 'h'.charCodeAt(0), 'i'.charCodeAt(0), 'j'.charCodeAt(0), 'k'.charCodeAt(0), 'l'.charCodeAt(0), 'm'.charCodeAt(0), 'n'.charCodeAt(0), 'o'.charCodeAt(0), 'p'.charCodeAt(0), 'q'.charCodeAt(0), 'r'.charCodeAt(0), 's'.charCodeAt(0), 't'.charCodeAt(0), 'u'.charCodeAt(0), 'v'.charCodeAt(0), 'w'.charCodeAt(0), 'x'.charCodeAt(0), 'y'.charCodeAt(0), 'z'.charCodeAt(0), '0'.charCodeAt(0), '1'.charCodeAt(0), '2'.charCodeAt(0), '3'.charCodeAt(0), '4'.charCodeAt(0), '5'.charCodeAt(0), '6'.charCodeAt(0), '7'.charCodeAt(0), '8'.charCodeAt(0), '9'.charCodeAt(0), '+'.charCodeAt(0), '/'.charCodeAt(0)];
 * private static powTwo : any = {0:1,1:2,2:4,3:8,4:16,5:32,6:64,7:128,8:256,9:512,10:1024,11:2048,12:4096,13:8192,14:16384,15:32768,16:65536,17:131072,18:262144,19:524288,20:1048576,21:2097152,22:4194304,23:8388608,24:16777216,25:33554432,26:67108864,27:134217728,28:268435456,29:536870912,30:1073741824,31:2147483648,32:4294967296,33:8589934592,34:17179869184,35:34359738368,36:68719476736,37:137438953472,38:274877906944,39:549755813888,40:1099511627776,41:2199023255552,42:4398046511104,43:8796093022208,44:17592186044416,45:35184372088832,46:70368744177664,47:140737488355328,48:281474976710656,49:562949953421312,50:1125899906842624,51:2251799813685248,52:4503599627370496,53:9007199254740992};
 * private static longIndexes : Long[] = [Long.fromNumber(0), Long.fromNumber(1), Long.fromNumber(2), Long.fromNumber(3), Long.fromNumber(4), Long.fromNumber(5), Long.fromNumber(6), Long.fromNumber(7), Long.fromNumber(8), Long.fromNumber(9), Long.fromNumber(10), Long.fromNumber(11), Long.fromNumber(12), Long.fromNumber(13), Long.fromNumber(14), Long.fromNumber(15), Long.fromNumber(16), Long.fromNumber(17), Long.fromNumber(18), Long.fromNumber(19), Long.fromNumber(20), Long.fromNumber(21), Long.fromNumber(22), Long.fromNumber(23), Long.fromNumber(24), Long.fromNumber(25), Long.fromNumber(26), Long.fromNumber(27), Long.fromNumber(28), Long.fromNumber(29), Long.fromNumber(30), Long.fromNumber(31), Long.fromNumber(32), Long.fromNumber(33), Long.fromNumber(34), Long.fromNumber(35), Long.fromNumber(36), Long.fromNumber(37), Long.fromNumber(38), Long.fromNumber(39), Long.fromNumber(40), Long.fromNumber(41), Long.fromNumber(42), Long.fromNumber(43), Long.fromNumber(44), Long.fromNumber(45), Long.fromNumber(46), Long.fromNumber(47), Long.fromNumber(48), Long.fromNumber(49), Long.fromNumber(50), Long.fromNumber(51), Long.fromNumber(52), Long.fromNumber(53), Long.fromNumber(54), Long.fromNumber(55), Long.fromNumber(56), Long.fromNumber(57), Long.fromNumber(58), Long.fromNumber(59), Long.fromNumber(60), Long.fromNumber(61), Long.fromNumber(62), Long.fromNumber(63)];
 * public static encodeLongToBuffer(l:number, buffer:greycat.struct.Buffer) {
 * var empty=true;
 * var tmp = l;
 * if(l < 0) {
 * tmp = -tmp;
 * }
 * for (var i = 47; i >= 5; i -= 6) {
 * if (!(empty && ((tmp / Base64.powTwo[i]) & 0x3F) == 0)) {
 * empty = false;
 * buffer.write(Base64.dictionary[(tmp / Base64.powTwo[i]) & 0x3F]);
 * }
 * }
 * buffer.write(Base64.dictionary[(tmp & 0x1F)*2 + (l<0?1:0)]);
 * }
 * public static encodeIntToBuffer(l:number, buffer:greycat.struct.Buffer) {
 * var empty=true;
 * var tmp = l;
 * if(l < 0) {
 * tmp = -tmp;
 * }
 * for (var i = 29; i >= 5; i -= 6) {
 * if (!(empty && ((tmp / Base64.powTwo[i]) & 0x3F) == 0)) {
 * empty = false;
 * buffer.write(Base64.dictionary[(tmp / Base64.powTwo[i]) & 0x3F]);
 * }
 * }
 * buffer.write(Base64.dictionary[(tmp & 0x1F)*2 + (l<0?1:0)]);
 * }
 * public static decodeToLong(s : greycat.struct.Buffer) {
 * return Base64.decodeToLongWithBounds(s, 0, s.length());
 * }
 * public static decodeToLongWithBounds(s:greycat.struct.Buffer, offsetBegin:number, offsetEnd:number) {
 * var result = Long.ZERO;
 * result = result.add(Base64.longIndexes[Base64.dictionary.indexOf(s.read((offsetEnd - 1))) & 0xFF].shiftRightUnsigned(1));
 * for (var i = 1; i < (offsetEnd - offsetBegin); i++) {
 * result = result.add(Base64.longIndexes[Base64.dictionary.indexOf(s.read((offsetEnd - 1) - i)) & 0xFF].shiftLeft((6 * i)-1));
 * }
 * if (((Base64.dictionary.indexOf(s.read((offsetEnd - 1))) & 0xFF) & 0x1) != 0) {
 * result = result.mul(-1);
 * }
 * return result.toNumber();
 * }
 * public static decodeToInt(s : greycat.struct.Buffer) {
 * return Base64.decodeToIntWithBounds(s, 0, s.length());
 * }
 * public static decodeToIntWithBounds(s:greycat.struct.Buffer, offsetBegin:number, offsetEnd:number) {
 * var result = 0;
 * result += ((Base64.dictionary.indexOf(s.read((offsetEnd - 1))) & 0xFF) / 2) & 0xFF;
 * for (var i = 1; i < (offsetEnd - offsetBegin); i++) {
 * result += (Base64.dictionary.indexOf(s.read((offsetEnd - 1) - i)) & 0xFF) * Base64.powTwo[(6 * i)-1];
 * }
 * if (((Base64.dictionary.indexOf(s.read((offsetEnd - 1))) & 0xFF) & 0x1) != 0) {
 * result = -result;
 * }
 * return result;
 * }
 * public static encodeDoubleToBuffer(d : number, buffer : greycat.struct.Buffer) {
 * var result = [];
 * var floatArr = new Float64Array(1);
 * var bytes = new Uint8Array(floatArr.buffer);
 * floatArr[0] = d;
 * var exponent = (((bytes[7] & 0x7f) * 16) | bytes[6] / 16) - 0x3ff;
 * var signAndExp = (((bytes[7] / 128) & 0x1) * 2048) + (exponent + 1023);
 * //encode sign + exp
 * result.push(Base64.dictionary[(signAndExp / 64) & 0x3F]);
 * result.push(Base64.dictionary[signAndExp & 0x3F]);
 * result.push(Base64.dictionary[bytes[6] & 0x0F]);
 * result.push(Base64.dictionary[(bytes[5] / 4) & 0x3F]);
 * result.push(Base64.dictionary[((bytes[5] & 0x3) * 16) | (bytes[4] / 16)]);
 * result.push(Base64.dictionary[((bytes[4] & 0x0F) * 4) | (bytes[3] / 64)]);
 * result.push(Base64.dictionary[(bytes[3] & 0x3F)]);
 * result.push(Base64.dictionary[(bytes[2] / 4) & 0x3F]);
 * result.push(Base64.dictionary[((bytes[2] & 0x3) * 16) | (bytes[1] / 16)]);
 * result.push(Base64.dictionary[((bytes[1] & 0x0F) * 4) | (bytes[0] / 64)]);
 * result.push(Base64.dictionary[(bytes[0] & 0x3F)]);
 * var indexMax = result.length;
 * while (indexMax >= 3 && result[i] == 65) {
 * indexMax--;
 * }
 * for (var i = 0; i < indexMax; i++) {
 * buffer.write(result[i]);
 * }
 * }
 * public static decodeToDouble(s : greycat.struct.Buffer) {
 * return Base64.decodeToDoubleWithBounds(s, 0, s.length());
 * }
 * public static decodeToDoubleWithBounds(s : greycat.struct.Buffer, offsetBegin : number, offsetEnd : number) {
 * var signAndExp = ((Base64.dictionary.indexOf(s.read(offsetBegin)) & 0xFF) * 64) + (Base64.dictionary.indexOf(s.read(offsetBegin + 1)) & 0xFF);
 * var sign = ((signAndExp & 0x800) != 0 ? -1 : 1);
 * var exp = signAndExp & 0x7FF;
 * //Mantisse
 * var mantissaBits = 0;
 * for (var i = 2; i < (offsetEnd - offsetBegin); i++) {
 * mantissaBits += (Base64.dictionary.indexOf(s.read(offsetBegin + i)) & 0xFF) * Base64.powTwo[48 - (6 * (i-2))];
 * }
 * return (exp != 0) ? sign * Math.pow(2, exp - 1023) * (1 + (mantissaBits / Math.pow(2, 52))) : sign * Math.pow(2, -1022) * (0 + (mantissaBits / Math.pow(2, 52)));
 * }
 * public static encodeBoolArrayToBuffer(boolArr : Array<boolean>, buffer : greycat.struct.Buffer) {
 * var tmpVal = 0;
 * for (var i = 0; i < boolArr.length; i++) {
 * tmpVal = tmpVal | ((boolArr[i] ? 1 : 0) * Base64.powTwo[i % 6]);
 * if (i % 6 == 5 || i == boolArr.length - 1) {
 * buffer.write(Base64.dictionary[tmpVal]);
 * tmpVal = 0;
 * }
 * }
 * }
 * public static decodeBoolArray(s : greycat.struct.Buffer, arraySize : number) {
 * return Base64.decodeToBoolArrayWithBounds(s, 0, s.length(), arraySize);
 * }
 * public static decodeToBoolArrayWithBounds(s : greycat.struct.Buffer, offsetBegin : number, offsetEnd : number, arraySize : number) {
 * var resultTmp : any[] = [];
 * for (var i = 0; i < (offsetEnd - offsetBegin); i++) {
 * var bitarray = Base64.dictionary.indexOf(s.read(offsetBegin + i)) & 0xFF;
 * for (var bit_i = 0; bit_i < 6; bit_i++) {
 * if ((6 * i) + bit_i < arraySize) {
 * resultTmp[(6 * i) + bit_i] = (bitarray & (1 * Base64.powTwo[bit_i])) != 0;
 * } else {
 * break;
 * }
 * }
 * }
 * return resultTmp;
 * }
 * public static encodeStringToBuffer(s : string, buffer : greycat.struct.Buffer) {
 * var sLength = s.length;
 * var currentSourceChar : number;
 * var currentEncodedChar = 0;
 * var freeBitsInCurrentChar = 6;
 * for(var charIdx = 0; charIdx < sLength; charIdx++) {
 * currentSourceChar = s.charCodeAt(charIdx);
 * if(freeBitsInCurrentChar == 6) {
 * buffer.write(Base64.dictionary[(currentSourceChar / 4) & 0x3F]);
 * currentEncodedChar = (currentSourceChar & 0x3) * 16;
 * freeBitsInCurrentChar = 4;
 * } else if(freeBitsInCurrentChar == 4) {
 * buffer.write(Base64.dictionary[(currentEncodedChar | ((currentSourceChar / 16) & 0xF)) & 0x3F]);
 * currentEncodedChar = (currentSourceChar & 0xF) * 4;
 * freeBitsInCurrentChar = 2;
 * } else if(freeBitsInCurrentChar == 2) {
 * buffer.write(Base64.dictionary[(currentEncodedChar | ((currentSourceChar / 64) & 0x3)) & 0x3F]);
 * buffer.write(Base64.dictionary[currentSourceChar & 0x3F]);
 * freeBitsInCurrentChar = 6;
 * }
 * }
 * if(freeBitsInCurrentChar != 6) {
 * buffer.write(Base64.dictionary[currentEncodedChar]);
 * }
 * }
 * public static decodeString(s : greycat.struct.Buffer) {
 * return Base64.decodeToStringWithBounds(s, 0, s.length());
 * }
 * public static decodeToStringWithBounds(s : greycat.struct.Buffer, offsetBegin : number, offsetEnd : number) {
 * var result = "";
 * var currentSourceChar : number;
 * var currentDecodedChar = 0;
 * var freeBitsInCurrentChar = 8;
 * for(var charIdx = offsetBegin; charIdx < offsetEnd; charIdx++) {
 * currentSourceChar = Base64.dictionary.indexOf(s.read(charIdx));
 * if(freeBitsInCurrentChar == 8) {
 * currentDecodedChar = currentSourceChar * 4;
 * freeBitsInCurrentChar = 2;
 * } else if(freeBitsInCurrentChar == 2) {
 * result += String.fromCharCode(currentDecodedChar | (currentSourceChar / 16));
 * currentDecodedChar = (currentSourceChar & 0xF) * 16;
 * freeBitsInCurrentChar = 4;
 * } else if(freeBitsInCurrentChar == 4) {
 * result += String.fromCharCode(currentDecodedChar | (currentSourceChar / 4));
 * currentDecodedChar = (currentSourceChar & 0x3) * 64;
 * freeBitsInCurrentChar = 6;
 * } else if(freeBitsInCurrentChar == 6) {
 * result += String.fromCharCode(currentDecodedChar | currentSourceChar);
 * freeBitsInCurrentChar = 8;
 * }
 * }
 * return result;
 * }
 * }
 */
public class Base64 {

    private final static byte[] encodeArray = new byte[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private final static int[] decodeArray = new int[123];

    static {
        int i = 0;
        for (byte c = 'A'; c <= 'Z'; c++) {
            decodeArray[c] = i;
            i++;
        }
        for (byte c = 'a'; c <= 'z'; c++) {
            decodeArray[c] = i;
            i++;
        }
        for (byte c = '0'; c <= '9'; c++) {
            decodeArray[c] = i;
            i++;
        }
        decodeArray['+'] = i;
        i++;
        decodeArray['/'] = i;
    }


    /**
     * Encodes a long in a base-64 string. Sign is encoded on bit 0 of the long =&gt; LS bit of the right-most char of the string. 1 for negative; 0 otherwise.<br>
     * The result is written directly in the buffer.
     *
     * @param l the long to encode
     * @param buffer the buffer to fill
     */
    public static void encodeLongToBuffer(long l, Buffer buffer) {
        boolean empty = true;
        long tmp = l;
        if (l < 0) {
            tmp = -tmp;
        }
        for (int i = 47; i >= 5; i -= 6) {
            if (!(empty && ((int) (tmp >> i) & 0x3F) == 0)) {
                empty = false;
                buffer.write(encodeArray[(int) (tmp >> i) & 0x3F]);
            }
        }
        buffer.write(Base64.encodeArray[(int) ((tmp & 0x1F) << 1) + (l < 0 ? 1 : 0)]);
    }

    /**
     * Decodes a Base64 string to a long value. The string is read directly from the buffer.
     * @param buffer        the buffer containing the string to decode
     * @param offsetBegin   the offset to the beginning of the string in the buffer
     * @param offsetEnd     the offset to the end of the string
     * @return              the decoded long value
     */
    public static long decodeToLongWithBounds(Buffer buffer, long offsetBegin, long offsetEnd) {
        long result = 0;
        result += (Base64.decodeArray[buffer.read(offsetEnd - 1)] & 0xFF) >> 1;
        long length = offsetEnd - offsetBegin;
        for (long i = 1; i < length; i++) {
            result += ((long) (Base64.decodeArray[buffer.read((offsetEnd - 1) - i)] & 0xFF)) << ((6 * i) - 1);
        }
        if (((Base64.decodeArray[buffer.read(offsetEnd - 1)] & 0xFF) & 0x1) != 0) {
            result = -result;
        }
        return result;
    }

    /**
     * Encodes a int in a base-64 string. Sign is encoded on bit 0 of the long =&gt; LS bit of the right-most char of the string. 1 for negative; 0 otherwise.<br>
     * The result is written directly in the buffer.
     *
     * @param l         the int to encode
     * @param buffer    the buffer to fill
     */
    public static void encodeIntToBuffer(int l, Buffer buffer) {
        boolean empty = true;
        int tmp = l;
        if (l < 0) {
            tmp = -tmp;
        }
        for (int i = 29; i >= 5; i -= 6) {
            if (!(empty && ((int) (tmp >> i) & 0x3F) == 0)) {
                empty = false;
                buffer.write(encodeArray[(tmp >> i) & 0x3F]);
            }
        }
        buffer.write(Base64.encodeArray[((tmp & 0x1F) << 1) + (l < 0 ? 1 : 0)]);
    }

    /**
     * Decodes a Base64 string to an int value. The string is read directly from the buffer.
     * @param buffer        the buffer containing the string to decode
     * @param offsetBegin   the offset to the beginning of the string in the buffer
     * @param offsetEnd     the offset to the end of the string
     * @return              the decoded int value
     */
    public static int decodeToIntWithBounds(Buffer buffer, long offsetBegin, long offsetEnd) {
        int result = 0;
        result += (Base64.decodeArray[buffer.read(offsetEnd - 1)] & 0xFF) >> 1;
        long length = offsetEnd - offsetBegin;
        for (int i = 1; i < length; i++) {
            result += (Base64.decodeArray[buffer.read((offsetEnd - 1) - i)] & 0xFF) << ((6 * i) - 1);
        }
        if (((Base64.decodeArray[buffer.read(offsetEnd - 1)] & 0xFF) & 0x1) != 0) {
            result = -result;
        }
        return result;
    }


    /**
     * Encodes a boolean array into a Base64 string
     *
     * @param boolArr   the array to encode
     * @param buffer    the buffer to fill
     */
    public static void encodeBoolArrayToBuffer(boolean[] boolArr, Buffer buffer) {
        int tmpVal = 0;
        for (int i = 0; i < boolArr.length; i++) {
            tmpVal = tmpVal | ((boolArr[i] ? 1 : 0) << i % 6);
            if (i % 6 == 5 || i == boolArr.length - 1) {
                buffer.write(Base64.encodeArray[tmpVal]);
                tmpVal = 0;
            }
        }
    }

    /**
     * Decodes a Base64 string to a boolean array. The string is read directly from the buffer.
     * @param buffer        the buffer containing the string to decode
     * @param offsetBegin   the offset to the beginning of the string in the buffer
     * @param offsetEnd     the offset to the end of the string
     * @param arraySize     The size of the array
     * @return              the decoded boolean array
     */
    public static boolean[] decodeToBoolArrayWithBounds(Buffer buffer, long offsetBegin, long offsetEnd, int arraySize) {
        boolean[] resultTmp = new boolean[arraySize];
        long length = offsetEnd - offsetBegin;
        for (int i = 0; i < length; i++) {
            int bitarray = Base64.decodeArray[buffer.read(offsetBegin + i)] & 0xFF;
            for (int bit_i = 0; bit_i < 6; bit_i++) {
                if ((6 * i) + bit_i < arraySize) {
                    resultTmp[(6 * i) + bit_i] = (bitarray & (1 << bit_i)) != 0;
                } else {
                    break;
                }
            }
        }
        return resultTmp;
    }

    /**
     * Encodes a double into Base64 string Following the IEEE-754.
     * 2 first chars for sign + exponent; remaining chars on the right for the mantissa.
     * Trailing 'A's (aka 0) are dismissed for compression.
     *
     * @param d the double to encode
     * @param buffer the buffer to fill
     */
    public static void encodeDoubleToBuffer(double d, Buffer buffer) {
        long l = Double.doubleToLongBits(d);
        //encode sign + exp
        buffer.write(Base64.encodeArray[(int) (l >> 58) & 0x3F]);
        buffer.write(Base64.encodeArray[(int) (l >> 52) & 0x3F]);
        //encode mantisse
        buffer.write(Base64.encodeArray[(int) (l >> 48) & 0x0F]);
        for (int i = 42; i >= 0; i -= 6) {
            if (((l >> i) & 0x3F) == 0 && (l & (~(0xFFFFFFFFFFFFFFFFl << i))) == 0) {
                return;
            }
            buffer.write(Base64.encodeArray[(int) (l >> i) & 0x3F]);
        }
    }

    /**
     * Decodes a Base64 string to an double value. The string is read directly from the buffer.
     * @param buffer        the buffer containing the string to decode
     * @param offsetBegin   the offset to the beginning of the string in the buffer
     * @param offsetEnd     the offset to the end of the string
     * @return              the decoded double value
     */
    public static double decodeToDoubleWithBounds(Buffer buffer, long offsetBegin, long offsetEnd) {
        long result = 0;
        //sign + exponent
        result += ((long) Base64.decodeArray[buffer.read(offsetBegin)] & 0xFF) << 58;
        result += ((long) Base64.decodeArray[buffer.read(offsetBegin + 1)] & 0xFF) << 52;
        //Mantisse
        for (int i = 2; i < (offsetEnd - offsetBegin); i++) {
            result += ((long) Base64.decodeArray[buffer.read(offsetBegin + i)] & 0xFF) << (48 - (6 * (i - 2)));
        }
        return Double.longBitsToDouble(result);
    }

    /**
     * Encodes a string into Base64 string.
     *
     * @param s         the string to encode
     * @param buffer    the buffer to fill
     */
    public static void encodeStringToBuffer(String s, Buffer buffer) {
        if(s == null) {
            return;
        }
        int sLength = s.length();
        char currentSourceChar;
        int currentEncodedChar = 0;
        int freeBitsInCurrentChar = 6;
        for (int charIdx = 0; charIdx < sLength; charIdx++) {
            currentSourceChar = s.charAt(charIdx);
            if (freeBitsInCurrentChar == 6) {
                buffer.write(Base64.encodeArray[currentSourceChar >> 2 & 0x3F]);
                currentEncodedChar = (currentSourceChar & 0x3) << 4;
                freeBitsInCurrentChar = 4;
            } else if (freeBitsInCurrentChar == 4) {
                buffer.write(Base64.encodeArray[(currentEncodedChar | ((currentSourceChar >> 4) & 0xF)) & 0x3F]);
                currentEncodedChar = (currentSourceChar & 0xF) << 2;
                freeBitsInCurrentChar = 2;
            } else if (freeBitsInCurrentChar == 2) {
                buffer.write(Base64.encodeArray[(currentEncodedChar | ((currentSourceChar >> 6) & 0x3)) & 0x3F]);
                buffer.write(Base64.encodeArray[currentSourceChar & 0x3F]);
                freeBitsInCurrentChar = 6;
            }
        }
        if (freeBitsInCurrentChar != 6) {
            buffer.write(Base64.encodeArray[currentEncodedChar]);
        }
    }

    /**
     * Decodes a Base64 string to a string. The string is read directly from the buffer.
     * @param buffer        the buffer containing the string to decode
     * @param offsetBegin   the offset to the beginning of the string in the buffer
     * @param offsetEnd     the offset to the end of the string
     * @return              the decoded string value
     */
    public static String decodeToStringWithBounds(Buffer buffer, long offsetBegin, long offsetEnd) {
        if (offsetBegin == offsetEnd) {
            return null;
        }
        String result = "";

        int currentSourceChar;
        int currentDecodedChar = 0;
        int freeBitsInCurrentChar = 8;

        for (long charIdx = offsetBegin; charIdx < offsetEnd; charIdx++) {
            currentSourceChar = Base64.decodeArray[buffer.read(charIdx)];
            if (freeBitsInCurrentChar == 8) {
                currentDecodedChar = currentSourceChar << 2;
                freeBitsInCurrentChar = 2;
            } else if (freeBitsInCurrentChar == 2) {
                result += (char) (currentDecodedChar | (currentSourceChar >> 4));
                currentDecodedChar = (currentSourceChar & 0xF) << 4;
                freeBitsInCurrentChar = 4;
            } else if (freeBitsInCurrentChar == 4) {
                result += (char) (currentDecodedChar | (currentSourceChar >> 2));
                currentDecodedChar = (currentSourceChar & 0x3) << 6;
                freeBitsInCurrentChar = 6;
            } else if (freeBitsInCurrentChar == 6) {
                result += (char) (currentDecodedChar | currentSourceChar);
                freeBitsInCurrentChar = 8;
            }
        }

        return result;
    }


    /* UTILITIES FOR DEBUG -- DO NOT REMOVE
    private static String printBits(long val) {
        String toString = Long.toBinaryString(val);
        String res = "";

        for (int i = 0; i < 64 - toString.length(); i++) {
            res += "0";
        }
        return res + toString;
    }
    private static String printBits(int val) {
        String toString = Integer.toBinaryString(val);
        String res = "";

        for (int i = 0; i < 32 - toString.length(); i++) {
            res += "0";
        }
        return res + toString;
    }
    */


}
