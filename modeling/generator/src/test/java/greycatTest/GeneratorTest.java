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
package greycatTest;

import greycat.generator.Generator;
import org.junit.Test;

import java.io.File;

public class GeneratorTest {

    public static void main(String[] args) throws Exception {
        File target = new File("/Users/duke/Documents/datathings/greycat/modeling/generator/target/gen");
        target.exists();
        File targetWeb = new File("/Users/duke/Documents/datathings/greycat/modeling/generator/target/gen-web");
        targetWeb.exists();
        Generator generator = new Generator();
        generator.parse(new File("/Users/duke/Documents/datathings/greycat/modeling/generator/src/test/resources/hello.gcm"));
        generator.generate("generator", "myGen", target, targetWeb, true, false, "9", null);
    }

}
