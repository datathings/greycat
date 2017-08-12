/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycat.ac.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class OtpEngine {

    // taken from Google pam docs - we probably don't need to mess with these
    //final static int secretSize = 10;
    final static int secretSize = 128;
    final static int numOfScratchCodes = 5;
    final static int scratchCodeSize = 8;
    private static final int[] DIGITS_POWER
            // 0 1  2   3    4     5      6       7        8         9
            = {1,10,100,1000,10000,100000,1000000,10000000,100000000,1000000000};

    int window_size = 3;  // default 3 - max 17 (from google docs)

    /**
     * set the windows size. This is an integer value representing the number of 30 second windows we allow
     * The bigger the window, the more tolerant of clock skew we are.
     */
    public void setWindowSize(int s) {
        if (s >= 1 && s <= 17)
            window_size = s;
    }

    /**
     * Generate a random secret key. This must be saved by the server and associated with the
     * users account to verify the code displayed by Google Authenticator.
     * The user must register this secret on their device.
     */
    public static String generateSecretKey() {
        // Allocating the buffer
        //byte[] buffer = new byte[secretSize + numOfScratchCodes* scratchCodeSize];
        byte[] buffer = new byte[secretSize];
        // Filling the buffer with random numbers.
        // Notice: you want to reuse the same random generator
        // while generating larger random number sequences.
        new Random().nextBytes(buffer);
        return Base32String.encode(buffer);
    }

    /**
     * Return a URL that generates and displays a QR barcode. The user scans this bar code with the
     * Google Authenticator application on their smartphone to register the auth code. They can also manually enter the
     * secret if desired
     */
    private static String qrCodeUrl = "https://www.google.com/chart?chs=200x200&chld=M%%7C0&cht=qr&chl=otpauth://totp/%s%%3Fsecret%%3D%s%%26issuer%%3D%s";

    public static String getQRURL(String login, String secret, String issuer) {
        return String.format(qrCodeUrl, login, secret, issuer);
    }

    private static String authenticatorUri = "otpauth://totp/%s?secret=%s&issuer=%s";

    public static String getAuthenticatorUri(String login, String secret, String issuer) {
        return String.format(authenticatorUri, login, secret, issuer);
    }

    /**
     * Check the code entered by the user to see if it is valid
     */
    public boolean check_code(String secret, String code, long timeMsec) {
        try {

            byte[] decodedKey = Base32String.decode(secret);

            // convert unix msec time into a 30 second "window"
            // this is per the TOTP spec (see the RFC for details)
            long t = (timeMsec / 1000L) / 30L;
            // Window is used to check codes generated in the near past.
            // You can use this value to tune how far you're willing to go.

            for (int i = -window_size; i <= window_size; ++i) {
                long hash;
                try {
                    hash = verify_code(decodedKey, t + i, code.length());
                } catch (Exception e) {
                    // Yes, this is bad form - but
                    // the exceptions thrown would be rare and a static configuration problem
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                    //return false;
                }
                if (hash == Long.parseLong(code)) {
                    return true;
                }
            }
        } catch (Base32String.DecodingException e) {
            e.printStackTrace();
        }
        // The validation code is invalid.
        return false;
    }


    private static int verify_code(byte[] key, long t, int size)
            throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[20 - 1] & 0xF;

        // We're using a long because Java hasn't got unsigned int.
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            // We are dealing with signed bytes:
            // we just keep the first byte.
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= DIGITS_POWER[size];

        return (int) truncatedHash;
    }
}