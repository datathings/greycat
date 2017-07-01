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

import greycat.language.Constant;
import greycat.language.Model;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

public class GlobalConstantGenerator {
    private static final String CONSTANT_CLASS_NAME = "Constants";

    static JavaSource[] generate(String packageName, Model model) {
        JavaSource[] sources = new JavaSource[1];
        sources[0] = generateGlobalConstant(packageName, model.globalConstants());

        return sources;
    }

    private static JavaSource generateGlobalConstant(String packageName, Constant[] globalConstants) {
        final JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(packageName);
        javaClass.setName(CONSTANT_CLASS_NAME);
        // constants
        for (Constant constant : globalConstants) {
            String value = constant.value();
            if (constant.type().equals("Task") && value != null) {
                value = "greycat.Tasks.newTask().parse(\"" + value.replaceAll("\"", "'").trim() + "\",null);";
            } else if (!constant.type().equals("String") && value != null) {
                value = value.replaceAll("\"", "");
            }
            FieldSource field = javaClass.addField()
                    .setVisibility(Visibility.PUBLIC)
                    .setName(constant.name())
                    .setType(TypeManager.className(constant.type()))
                    .setLiteralInitializer(value)
                    .setStatic(true);
            if (value != null) {
                field.setFinal(true);
            }
        }
        return javaClass;
    }
}
