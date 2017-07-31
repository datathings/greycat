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
package greycat.generator;

import com.squareup.javapoet.JavaFile;
import greycat.language.Checker;
import greycat.language.Model;
import java2typescript.SourceTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Generator {
    public static final String FILE_EXTENSION = ".gcm";

    private final Model model;

    public Generator() {
        this.model = new Model();
    }

    static String upperCaseFirstChar(String init) {
        return init.substring(0, 1).toUpperCase() + init.substring(1);
    }

    public void parse(File target) throws Exception {
        if (target.getName().endsWith(FILE_EXTENSION)) {
            this.model.parse(target);
        } else {
            throw new RuntimeException("no file with correct extension found");
        }
    }

    private void generateJava(String packageName, String pluginName, File target) {
        List<JavaFile> collector = new ArrayList<JavaFile>();
        TypeGenerator.generate(packageName, model, collector);
        IndexGenerator.generate(packageName, model, collector);
        ConstantGenerator.generate(packageName, model, collector);
        PluginGenerator.generate(packageName, pluginName, model, collector);
        for (JavaFile file : collector) {
            try {
                file.writeTo(target);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    private void generateJS(String packageName, String pluginName, File src, File target, String gcVersion, String projectVersion, List<File> classPath) {
        File modelWeb = new File(target, "model");
        if (!modelWeb.exists()) {
            modelWeb.mkdirs();
        }
        File modelWebStarter = new File(target, "model-starter");
        if (!modelWebStarter.exists()) {
            modelWebStarter.mkdirs();
        }
        SourceTranslator transpiler = new SourceTranslator(Arrays.asList(src.getAbsolutePath()), modelWeb.getAbsolutePath(), packageName);
        if (classPath != null) {
            for (int i = 0; i < classPath.size(); i++) {
                transpiler.addToClasspath(classPath.get(i).getAbsolutePath());
            }
        } else {
            addToTransClassPath(transpiler);
        }
        transpiler.process();
        transpiler.addHeader("import * as greycat from '@greycat/greycat';");
        transpiler.addHeader("import {java} from '@greycat/j2ts-jre';");
        transpiler.generate();
        File tsGen = new File(modelWeb, packageName + ".ts");
        try {
            Files.write(tsGen.toPath(), ("export = " + packageName).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String tsConfigContent = "{\n" +
                "  \"compilerOptions\": {\n" +
                "    \"module\": \"commonjs\",\n" +
                "    \"noImplicitAny\": false,\n" +
                "    \"removeComments\": true,\n" +
                "    \"preserveConstEnums\": true,\n" +
                "    \"sourceMap\": true,\n" +
                "    \"target\": \"es5\",\n" +
                "    \"declaration\": true,\n" +
                "    \"outDir\": \"lib\"\n" +
                "  },\n" +
                "  \"files\": [\n" +
                "    \"" + packageName + ".ts\"\n" +
                "  ]\n" +
                "}";
        try {
            File tsConfig = new File(modelWeb, "tsconfig.json");
            tsConfig.createNewFile();
            Files.write(tsConfig.toPath(), tsConfigContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean isSnaphot = (gcVersion.contains("SNAPSHOT"));
        if (isSnaphot) {
            StringBuilder tgzVersion = new StringBuilder(gcVersion.replace("-SNAPSHOT", ""));
            while (tgzVersion.toString().split("\\.").length != 3) {
                tgzVersion.append(".0");
            }
            gcVersion = tgzVersion.toString();
            /*
            File greycatTgz = null;
            try {
                MavenResolver resolver = new MavenResolver();
                HashSet<String> urls = new HashSet<String>();
                urls.add("https://oss.sonatype.org/content/repositories/snapshots");
                greycatTgz = resolver.resolve("com.datathings", "greycat", gcVersion, "tgz", urls);
                if (greycatTgz == null) {
                    throw new RuntimeException("Could not resolve dependency: gp:com.datathings artifact:greycat version:" + gcVersion + " ext:tgz");
                }
                //greycatTgz = new File(new File(new File(src.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getCanonicalFile(), "greycat"), "target"), "greycat-" + tgzVersion + ".tgz");
                greycatTgz = greycatTgz.getCanonicalFile();
                System.out.println("using GreyCat Snapshot from " + greycatTgz.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            gcVersion = greycatTgz != null ? greycatTgz.getAbsolutePath().replace('\\', '/') : tgzVersion.toString();
            */
        }

        if (projectVersion.contains("SNAPSHOT")) {
            projectVersion = projectVersion.replace("-SNAPSHOT", "");
            while (projectVersion.split("\\.").length != 3) {
                projectVersion += ".0";
            }
        }

        String packageJsonContent = "{\n" +
                "  \"name\": \"" + packageName + "\",\n" +
                "  \"version\": \"" + projectVersion + "\",\n" +
                "  \"description\": \"\",\n" +
                "  \"main\": \"lib/" + packageName + "\",\n" +
                "  \"author\": \"\",\n" +
                "  \"types\": \"lib/" + packageName + "\",\n" +
                "  \"description\":\"empty\",\n" +
                "  \"repository\":\"empty\",\n" +
                "  \"license\":\"UNLICENSED\"," +
                "  \"dependencies\": {\n" +
                "    \"@greycat/greycat\": \"" + gcVersion + "\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"typescript\": \"2.4.2\"\n" +
                "  }" +
                "}";
        try {
            File packageJson = new File(modelWeb, "package.json");
            packageJson.createNewFile();
            Files.write(packageJson.toPath(), packageJsonContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Generate a base of NPM project
        File mainJS = new File(modelWebStarter, "main.js");
        File packageJson2 = new File(modelWebStarter, "package.json");
        File readme = new File(modelWebStarter, "readme.md");
        File mainTS = new File(modelWebStarter, "main2.ts");
        try {
            mainJS.createNewFile();
            Files.write(mainJS.toPath(), ("var greycat = require(\"@greycat/greycat\");\n" +
                    "var " + packageName + " = require(\"" + packageName + "\");\n" +
                    "\n" +
                    "var g = greycat.GraphBuilder.newBuilder().withPlugin(new " + packageName + "." + pluginName + "()).build();\n" +
                    "\n" +
                    "g.connect(function (isSucceed) {\n" +
                    "console.log(\"--- GreyCat ready ---\");\n" +
                    "    var n = g.newNode(0,0);\n" +
                    "    n.set(\"name\",greycat.CustomType.STRING, \"myName\");\n" +
                    "    console.log(n.toString());\n" +
                    "});").getBytes());

            packageJson2.createNewFile();
            Files.write(packageJson2.toPath(), ("{\n" +
                    "  \"name\": \"" + packageName + "-starter\",\n" +
                    "  \"version\": \"1.0.0\",\n" +
                    "  \"description\": \"\",\n" +
                    "  \"main\": \"main.js\",\n" +
                    "  \"author\": \"\",\n" +
                    "  \"description\":\"empty\",\n" +
                    "  \"repository\":\"empty\",\n" +
                    "  \"license\":\"UNLICENSED\"," +
                    "  \"dependencies\": {\n" +
                    "    \"@greycat/greycat\": \"" + gcVersion + "\",\n" +
                    "    \"" + packageName + "\": \"" + new File(modelWeb, packageName + "-" + projectVersion + ".tgz").getAbsolutePath().replace('\\', '/') + "\"\n" +
                    "  },\n" +
                    "  \"devDependencies\": {\n" +
                    "    \"typescript\": \"2.4.2\",\n" +
                    "    \"ts-node\": \"3.0.4\"\n" +
                    "  }" +
                    "}").getBytes());

            Files.write(readme.toPath(), ("# JavaScript usage\n" +
                    "\n" +
                    "`node main.js\n" +
                    "\n" +
                    "# TypeScript usage\n" +
                    "\n" +
                    "*(only the first time)*\n" +
                    "\n" +
                    "`npm install -g ts-node typescript`\n" +
                    "\n" +
                    "then\n" +
                    "\n" +
                    "`ts-node main2.ts`").getBytes());
            Files.write(mainTS.toPath(), ("import * as greycat from '@greycat/greycat';\n" +
                    "import * as " + packageName + " from '" + packageName + "';\n" +
                    "\n" +
                    "var g = greycat.GraphBuilder.newBuilder().withPlugin(new " + packageName + ".ModelPlugin()).build();\n" +
                    "\n" +
                    "g.connect(function (isSucceed) {\n" +
                    "    console.log(\"--- GreyCat ready ---\");\n" +
                    "    var n = g.newNode(0,0);\n" +
                    "    n.set(\"name\",greycat.CustomType.STRING, \"myName\");\n" +
                    "    console.log(n.toString());\n" +
                    "})\n").getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }

        ProcessBuilder processBuilderPre;
        ProcessBuilder processBuilder;
        ProcessBuilder processBuilder2;
        ProcessBuilder processBuilder3;
        ProcessBuilder processBuilder4;

        // Install required package in TS

        if (isWindows()) {
            processBuilderPre = new ProcessBuilder("CMD", "/C", "npm", "config", "set", "@greycat:registry", "https://registry.datathings.com/repository/npm-public/");
            processBuilder = new ProcessBuilder("CMD", "/C", "npm", "install");
            processBuilder2 = new ProcessBuilder("CMD", "/C", "node", "node_modules/typescript/lib/tsc.js");
            processBuilder3 = new ProcessBuilder("CMD", "/C", "npm", "pack");
            processBuilder4 = new ProcessBuilder("CMD", "/C", "npm", "install");
        } else {
            processBuilderPre = new ProcessBuilder("npm", "config", "set", "@greycat:registry", "https://registry.datathings.com/repository/npm-public/");
            processBuilder = new ProcessBuilder("npm", "install");
            processBuilder2 = new ProcessBuilder("node", "node_modules/typescript/lib/tsc.js");
            processBuilder3 = new ProcessBuilder("npm", "pack");
            processBuilder4 = new ProcessBuilder("npm", "install");
        }

        processBuilderPre.directory(modelWeb);
        processBuilderPre.inheritIO();
        // Run TSC
        processBuilder.directory(modelWeb);
        processBuilder.inheritIO();
        // Run TSC
        processBuilder2.directory(modelWeb);
        processBuilder2.inheritIO();
        // Pack Model
        processBuilder3.directory(modelWeb);
        processBuilder3.inheritIO();
        //Install required packaged in JS project
        processBuilder4.directory(modelWebStarter);
        processBuilder4.inheritIO();
        try {
            processBuilderPre.start().waitFor();
            processBuilder.start().waitFor();
            processBuilder2.start().waitFor();
            processBuilder3.start().waitFor();
            processBuilder4.start().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generate(String packageName, String pluginName, File target, File targetWeb, boolean generateJava, boolean generateJS, String gcVersion, String projectVersion, List<File> classPath) {
        model.consolidate();
        Checker.check(model);
        if (generateJava || generateJS) {
            generateJava(packageName, pluginName, target);
        }
        if (generateJS) {
            generateJS(packageName, pluginName, target, targetWeb, gcVersion, projectVersion, classPath);
        }
    }

    private void addToTransClassPath(SourceTranslator transpiler) {
        String classpath = System.getProperty("java.class.path");
        String[] classpathEntries = classpath.split(File.pathSeparator);
        for (int i = 0; i < classpathEntries.length; i++) {
            transpiler.addToClasspath(classpathEntries[i]);
        }
    }

}
