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
package greycat.language;

import static greycat.Tasks.newTask;

public class Checker {

    public static void check(Model model) {
        //check that all attributes have a type
        model.classes.values().forEach(aClass -> {
            aClass.properties().forEach(o -> {
                if (o instanceof Attribute) {
                    Attribute attribute = (Attribute) o;
                    if (attribute.type() == null) {
                        throw new RuntimeException("Untyped attribute " + attribute.name() + " contained in " + attribute.parent());
                    }
                } else if (o instanceof Constant) {
                    final Constant constant = (Constant) o;
                    if (constant.type().equals("Task") && constant.value() != null) {
                        checkTask(constant.value());
                    }
                }
            });
        });
        model.constants.values().forEach(constant -> {
            if (constant.type().equals("Task") && constant.value() != null) {
                checkTask(constant.value());
            }
        });
    }

    private static void checkTask(String value) {
        try {
            greycat.Task t = newTask().parse(value, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Task parsing error");
        }
    }

}
