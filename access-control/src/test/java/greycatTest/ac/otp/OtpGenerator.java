package greycatTest.ac.otp;

import greycat.ac.auth.Base32String;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Gregory NAIN on 09/08/2017.
 */
public class OtpGenerator {

    private static final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8         9
            = {1,10,100,1000,10000,100000,1000000,10000000,100000000,1000000000};

    public static String computePin(String secret, long time) {
        return computePin(secret, time, 6);
    }

    public static String computePin(String secret, long time, int size) {
        if(size < 0 || size > 9) {
            throw new RuntimeException("Pin length must be between 0 <= length <= 9. Was:"+size);
        }
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            long adjustedTime = getTimeValueAtTime(time/1000);

            byte[] value = ByteBuffer.allocate(8).putLong(adjustedTime).array();
            byte[] hash = mac.doFinal(value);

            // Dynamically truncate the hash
            // OffsetBits are the low order bits of the last byte of the hash
            int offset = hash[hash.length - 1] & 0xF;
            // Grab a positive integer value starting at the given offset.
            int truncatedHash = hashToInt(hash, offset) & 0x7FFFFFFF;
            int pinValue = truncatedHash % DIGITS_POWER[size];
            return padOutput(pinValue, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    private static String padOutput(int value, int size) {
        String result = Integer.toString(value);
        for (int i = result.length(); i < size; i++) {
            result = "0" + result;
        }
        return result;
    }

    private static int hashToInt(byte[] bytes, int start) {
        DataInput input = new DataInputStream(
                new ByteArrayInputStream(bytes, start, bytes.length - start));
        int val;
        try {
            val = input.readInt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return val;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException {
        return Base32String.decode(secret);
    }

    private static final int DEFAULT_INTERVAL = 30;

    private static long getTimeValueAtTime(long time) {
        if (time >= 0) {
            return time / DEFAULT_INTERVAL;
        } else {
            return (time - (DEFAULT_INTERVAL - 1)) / DEFAULT_INTERVAL;
        }
    }

}
