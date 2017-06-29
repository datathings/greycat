package greycatTest;

import greycat.language.Checker;
import greycat.language.Model;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MultiLineCommentTest {

    @Test
    public void testOpposite() throws IOException {
        Model model = new Model();
        model.parseResource("multiLineComment.gcm", this.getClass().getClassLoader());
        model.consolidate();
        Checker.check(model);
        Assert.assertEquals(1,model.classes().length);
        Assert.assertEquals(3,model.classes()[0].properties().size());
    }

}
