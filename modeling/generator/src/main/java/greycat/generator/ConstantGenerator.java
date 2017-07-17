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

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import greycat.language.Constant;
import greycat.language.Model;

import java.util.List;

import static greycat.generator.Helper.*;
import static javax.lang.model.element.Modifier.*;

class ConstantGenerator {
    private static final String CONSTANT_CLASS_NAME = "Constants";

    static void generate(String packageName, Model model, List<JavaFile> collector) {
        collector.add(generateGlobalConstant(packageName, model.constants()));
    }

    private static JavaFile generateGlobalConstant(String packageName, Constant[] globalConstants) {
        TypeSpec.Builder javaClass = TypeSpec.classBuilder(CONSTANT_CLASS_NAME);
        javaClass.addModifiers(PUBLIC, FINAL);
        for (Constant constant : globalConstants) {
            String value = constant.value();
            if (constant.type().equals("Task") && value != null) {
                value = "greycat.Tasks.newTask().parse(\"" + value.replaceAll("\"", "'").trim() + "\",null);";
            } else if (constant.type().equals("String") && value != null) {
                value = "\""+value+"\"";
            }
            FieldSpec.Builder field = FieldSpec.builder(clazz(constant.type()), constant.name())
                    .addModifiers(PUBLIC, STATIC);
            if (value != null) {
                field.addModifiers(FINAL).initializer(value);
            }
            javaClass.addField(field.build());
        }
        return JavaFile.builder(packageName, javaClass.build()).build();
    }
}
