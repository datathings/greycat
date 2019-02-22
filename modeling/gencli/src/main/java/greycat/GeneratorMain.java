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
package greycat;

import greycat.generator.Generator;

import java.io.File;
import java.nio.file.Files;
import java.util.Stack;

public class GeneratorMain {

    static void deleteDir(File dir) {
        File[] currList;
        Stack<File> stack = new Stack<File>();
        stack.push(dir);
        while (!stack.isEmpty()) {
            if (stack.lastElement().isDirectory()) {
                currList = stack.lastElement().listFiles();
                if (currList.length > 0) {
                    for (File curr : currList) {
                        stack.push(curr);
                    }
                } else {
                    stack.pop().delete();
                }
            } else {
                stack.pop().delete();
            }
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            System.err.println("usage: greycatGen <model.gcm>");
            return;
        }
        String model = args[0];
        File tempJavaGen;
        File tempGen;
        try {
            tempJavaGen = Files.createTempDirectory("greycat-gen-cli").toFile();
            tempGen = new File("gen");
            if (tempGen.exists()) {
                deleteDir(tempGen);
            }
            tempGen.mkdirs();
            Generator generator = new Generator();
            generator.parse(new File(model));
            String gcVersion = System.getProperty("greycat.version");
            generator.generate("model", "gen", tempJavaGen, tempGen, true, true, gcVersion.substring(0, gcVersion.indexOf('.')), "1.0.0", null);

            deleteDir(tempJavaGen);

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Hello From Java Cli " + model);
    }
}
