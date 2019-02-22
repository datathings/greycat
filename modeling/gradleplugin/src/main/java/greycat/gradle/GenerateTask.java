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
package greycat.gradle;

import greycat.generator.Generator;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import java.io.File;


public class GenerateTask extends DefaultTask {

    @TaskAction
    public void generate() throws Exception {
        GreyCatExtension extension = getProject().getExtensions().findByType(GreyCatExtension.class);
        if (extension == null) {
            extension = new GreyCatExtension();
        }
        String input = extension.getInput();
        File f = new File(getProject().getProjectDir(), input);
        Generator generator = new Generator();
        generator.parse(f);
        File target = new File(getProject().getProjectDir(), "gen");
        File targetJava = new File(target, "java");
        File targetTs = new File(target, "web");
        target.mkdirs();
        targetJava.mkdirs();
        targetTs.mkdirs();
        generator.generate("generated", "generated", targetJava, targetTs, true, true, "9", (String)getProject().getVersion(), null);
    }

}


