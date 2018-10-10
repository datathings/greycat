package greycatMLTest;

import greycat.ml.neuralnet.process.CRandomGenerator;
import org.junit.Test;

public class CRandomTest {
    @Test
    public void testrandom() {

        CRandomGenerator randomGenerator = new CRandomGenerator();
        randomGenerator.init(123456789, 1);

        long max = 10;
        int result = 0;

        for (long t = 0; t < max; t++) {
            result = randomGenerator.nextInt();

            System.out.println(Integer.toUnsignedString(result));
        }


//        assert (result == 172838706);

    }
}
