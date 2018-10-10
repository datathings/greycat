package greycatMLTest;

import greycat.ml.neuralnet.process.CRandomGenerator;
import org.junit.Test;

import java.util.Random;

public class CRandomTest {
    @Test
    public void testrandom() {

        CRandomGenerator randomGenerator = new CRandomGenerator();
        randomGenerator.init(123456789, 1);

        long max = 1000;
        int result = 0;

        for (long t = 0; t < max; t++) {
            result = randomGenerator.nextInt();
//            System.out.println(result);
        }


        assert (result == 70281542);

    }
}
