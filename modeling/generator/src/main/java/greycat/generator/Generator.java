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

import greycat.language.Checker;
import greycat.language.Model;
import java2typescript.SourceTranslator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.roaster.model.source.JavaSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

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

    /*
    public void deepScan(File target) throws Exception {
        if (target.isDirectory()) {
            String[] files = target.list();
            if (files == null) {
                throw new RuntimeException("no files to parse found");
            } else {
                for (String name : files) {
                    if (name.trim().endsWith(FILE_EXTENSION)) {
                        this.model.parse(new File(target, name));
                    } else {
                        File current = new File(target, name);
                        if (current.isDirectory()) {
                            deepScan(current);
                        }
                    }
                }
            }

        } else if (target.getName().endsWith(FILE_EXTENSION)) {
            this.model.parse(target);
        }
    }*/

    private void generateJava(String packageName, String pluginName, File target) {
        int index = 0;
        int size = model.classes().length + model.customTypes().length + model.globalIndexes().length + 1;
        JavaSource[] sources = new JavaSource[size * 2 + 2];

        sources[index] = PluginClassGenerator.generate(packageName, pluginName, model);
        index++;

        JavaSource[] classTypes = ClassTypeGenerator.generate(packageName, model);
        System.arraycopy(classTypes, 0, sources, index, classTypes.length);
        index += classTypes.length;

        JavaSource[] customTypes = CustomTypeGenerator.generate(packageName, model);
        System.arraycopy(customTypes, 0, sources, index, customTypes.length);
        index += customTypes.length;

        JavaSource[] globalIndexes = GlobalIndexGenerator.generate(packageName, model);
        System.arraycopy(globalIndexes, 0, sources, index, globalIndexes.length);
        index += globalIndexes.length;

        JavaSource[] globalConstants = GlobalConstantGenerator.generate(packageName, model);
        System.arraycopy(globalConstants, 0, sources, index, globalConstants.length);
        index += globalConstants.length;

        for (int i = 0; i < index; i++) {
            if (sources[i] != null) {
                JavaSource src = sources[i];
                File targetPkg;
                if (src.getPackage() != null) {
                    targetPkg = new File(target.getAbsolutePath() + File.separator + src.getPackage().replace(".", File.separator));
                } else {
                    targetPkg = target;
                }
                targetPkg.mkdirs();
                File targetSrc = new File(targetPkg, src.getName() + ".java");
                try {
                    FileWriter writer = new FileWriter(targetSrc);
                    writer.write(src.toString());
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void generateJS(String packageName, String pluginName, File src, File target, String gcVersion, MavenProject mvnProject) {
        File modelWeb = new File(target, "model");
        if (!modelWeb.exists()) {
            modelWeb.mkdirs();
        }
        File modelWebStarter = new File(target, "model-starter");
        if (!modelWebStarter.exists()) {
            modelWebStarter.mkdirs();
        }
        SourceTranslator transpiler = new SourceTranslator(Arrays.asList(src.getAbsolutePath()), modelWeb.getAbsolutePath(), packageName);
        if (mvnProject != null) {
            for (Artifact a : mvnProject.getArtifacts()) {
                File file = a.getFile();
                if (file != null) {
                    if (file.isFile()) {
                        transpiler.addToClasspath(file.getAbsolutePath());
                    }
                }
            }
        } else {
            addToTransClassPath(transpiler);
        }
        transpiler.process();
        transpiler.addHeader("import * as greycat from 'greycat';");
        transpiler.addHeader("import {java} from 'j2ts-jre';");
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
        String tgzVersion = gcVersion.replace("-SNAPSHOT", "") + ".0.0";
        File greycatTgz = null;
        try {
            greycatTgz = new File(new File(new File(src.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getCanonicalFile(), "greycat"), "target"), "greycat-" + tgzVersion + ".tgz");
            greycatTgz = greycatTgz.getCanonicalFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        gcVersion = isSnaphot ? greycatTgz.getAbsolutePath() : tgzVersion;
        String packageJsonContent = "{\n" +
                "  \"name\": \"" + packageName + "\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"description\": \"\",\n" +
                "  \"main\": \"lib/" + packageName + "\",\n" +
                "  \"author\": \"\",\n" +
                "  \"types\": \"lib/" + packageName + "\",\n" +
                "  \"description\":\"empty\",\n" +
                "  \"repository\":\"empty\",\n" +
                "  \"license\":\"UNLICENSED\"," +
                "  \"dependencies\": {\n" +
                "    \"greycat\": \"" + gcVersion + "\"\n" +
                "  },\n" +
                "  \"devDependencies\": {\n" +
                "    \"typescript\": \"2.3.4\"\n" +
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
            Files.write(mainJS.toPath(), ("var greycat = require(\"greycat\");\n" +
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
                    "    \"greycat\": \"" + gcVersion + "\",\n" +
                    "    \"" + packageName + "\": \"" + new File(modelWeb, "model-1.0.0.tgz").getAbsolutePath() + "\"\n" +
                    "  },\n" +
                    "  \"devDependencies\": {\n" +
                    "    \"typescript\": \"2.3.4\",\n" +
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
            Files.write(mainTS.toPath(), ("import * as greycat from 'greycat';\n" +
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

        // Install required package in TS
        ProcessBuilder processBuilder = new ProcessBuilder("npm", "install");
        processBuilder.directory(modelWeb);
        processBuilder.inheritIO();
        // Run TSC
        ProcessBuilder processBuilder2 = new ProcessBuilder("node", "node_modules/typescript/lib/tsc.js");
        processBuilder2.directory(modelWeb);
        processBuilder2.inheritIO();
        // Pack Model
        ProcessBuilder processBuilder3 = new ProcessBuilder("npm", "pack");
        processBuilder3.directory(modelWeb);
        processBuilder3.inheritIO();
        //Install required packaged in JS project
        ProcessBuilder processBuilder4 = new ProcessBuilder("npm", "install");
        processBuilder4.directory(modelWebStarter);
        processBuilder4.inheritIO();
        try {
            processBuilder.start().waitFor();
            processBuilder2.start().waitFor();
            processBuilder3.start().waitFor();
            processBuilder4.start().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mvnGenerate(String packageName, String pluginName, File target, File targetWeb, boolean generateJava, boolean generateJS, String gcVersion, MavenProject project) {
        model.consolidate();
        Checker.check(model);
        if (generateJava || generateJS) {
            generateJava(packageName, pluginName, target);
        }
        if (generateJS) {
            generateJS(packageName, pluginName, target, targetWeb, gcVersion, project);
        }
    }

    /*
    public void generate(String packageName, String pluginName, File target, boolean generateJava, boolean generateJS, String gcVersion) {
        if (generateJava || generateJS) {
            generateJava(packageName, pluginName, target);
        }

        if (generateJS) {
            generateJS(packageName, pluginName, target, gcVersion, null);
        }
    }*/

    private void addToTransClassPath(SourceTranslator transpiler) {
        String classPath = System.getProperty("java.class.path");
        int index = 0;
        boolean finish = false;
        while (index < classPath.length() && !finish) {
            if (classPath.charAt(index) == ':') {
                int slashIdx = index;
                while (slashIdx >= 0 && !finish) {
                    if (classPath.charAt(slashIdx) == '/') {
                        if (slashIdx + 7 < index && classPath.charAt(slashIdx + 1) == 'g' && classPath.charAt(slashIdx + 2) == 'r' && classPath.charAt(slashIdx + 3) == 'e'
                                && classPath.charAt(slashIdx + 4) == 'y' && classPath.charAt(slashIdx + 5) == 'c' && classPath.charAt(slashIdx + 6) == 'a' && classPath.charAt(slashIdx + 7) == 't') {
                            while (slashIdx >= 0 && !finish) {
                                if (classPath.charAt(slashIdx) == ':') {
                                    transpiler.addToClasspath(classPath.substring(slashIdx + 1, index));
                                    finish = true;
                                }
                                slashIdx--;
                            }
                        }
                    }
                    slashIdx--;
                }
            }
            index++;
        }
    }


}
