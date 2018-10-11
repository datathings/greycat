package greycatMLTest;

import greycat.ml.CRandomGenerator;
import org.junit.Test;

public class CRandomTest {

    /**
     * @native ts
     */
    @Test
    public void testrandom() {

        CRandomGenerator randomGenerator = new CRandomGenerator();
        randomGenerator.setSeed(123456789);

        long max = 1000;
        int result = 0;
        double result_d = 0;
        double result_g = 0;
        for (long t = 0; t < max; t++) {
            result = randomGenerator.nextInt();
        }
        for (long t = 0; t < max; t++) {
            result_d = randomGenerator.nextDouble();
        }
        for (long t = 0; t < max; t++) {
            result_g = randomGenerator.nextGaussian();
        }
//        System.out.println(result + " " + result_d + " " + result_g);

        //for 1000
        if (max == 1000) {
            assert (result == 70281542);
            assert (Math.abs(result_d - 0.069621) < 1e-5);
            assert (Math.abs(result_g + 1.139341) < 1e-5);
        } else if (max == 10000000) {
            assert (result == 28487886);
            assert (Math.abs(result_d - 0.034087) < 1e-5);
            assert (Math.abs(result_g - 0.582132) < 1e-5);
        }


//        assert (result == 70281542);

    }
}
