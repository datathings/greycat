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
package greycat.mavenplugin;

import greycat.generator.Generator;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GeneratorPlugin extends AbstractMojo {

    /**
     * File or directory containing the model definition.
     * If there are several files in the directory they will be merged into one.
     * <p>
     * File(s) should have the "{@value Generator#FILE_EXTENSION}".
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/gcm/model.gcm")
    private File input;

    /**
     * GreyCat plugin name
     */
    @Parameter(defaultValue = "ModelPlugin")
    private String pluginName;

    /**
     * Root package in which the Java classes are generated.
     * They are translated into TypeScript namespaces.
     */
    @Parameter(defaultValue = "model")
    private String packageName;

    /**
     * Folder in which the files should be generated
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/greycat-modeling")
    private File targetGen;

    /**
     * Folder in which the files should be generated
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources-web")
    private File targetGenJS;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Defines if the Java classes are generated
     */
    @Parameter(defaultValue = "true")
    private boolean generateJava;

    /**
     * Defines if the JavaScript classes are generated
     */
    @Parameter(defaultValue = "false")
    private boolean generateJS;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Generator generator = new Generator();
        try {
            generator.parse(input);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Error during GCM parse stage", e);
        }

        final String[] gcVersion = {""};
        project.getPluginArtifacts().forEach(artifact -> {
            if (artifact.getGroupId().equals("com.datathings") && artifact.getArtifactId().equals("greycat-mavenplugin")) {
                gcVersion[0] = artifact.getVersion();
            }
        });
        final List<File> cps = new ArrayList<File>();
        if (project != null) {
            for (Artifact a : project.getArtifacts()) {
                File file = a.getFile();
                if (file != null) {
                    if (file.isFile()) {
                        cps.add(file);
                    }
                }
            }
        }
        generator.generate(packageName, pluginName, targetGen, targetGenJS, generateJava, generateJS, gcVersion[0], project.getVersion(), cps);
        project.addCompileSourceRoot(targetGen.getAbsolutePath());
    }
}
