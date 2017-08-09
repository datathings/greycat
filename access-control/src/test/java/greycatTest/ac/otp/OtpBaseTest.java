/**
 * Copyright 2017 DataThings - All rights reserved.
 */
package greycatTest.ac.otp;

import greycat.ac.auth.OtpEngine;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Created by Gregory NAIN on 09/08/2017.
 */
public class OtpBaseTest {


    @Test
    public void baseTest() {

        OtpEngine engine = new OtpEngine();
        String secret = OtpEngine.generateSecretKey();

        String pin = OtpGenerator.computePin(secret, System.currentTimeMillis(),9);
        System.out.println(pin);

        assertTrue(engine.check_code(secret, pin, System.currentTimeMillis()));



    }

}
