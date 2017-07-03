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

import greycat.language.Attribute;

import java.util.List;

public class DefaultValueGenerator {

    public static StringBuilder createMethodBody(Attribute att) {
        StringBuilder builder = new StringBuilder();

        if (TypeManager.isPrimitive(att.type())) {
            builder.append("super.set(").append(att.name().toUpperCase()).append(", ").
                    append(att.name().toUpperCase()).append("_TYPE, ").append(att.value().get(0).get(0)).append(");");

        } else if (TypeManager.isPrimitiveArray(att.type())) {
            // primitive arrays
            builder.append(TypeManager.className(att.type())).append(" ").append(att.name()).append(" = ");
            builder.append("(").append(TypeManager.className(att.type())).append(")").append(" super.getOrCreate(").
                    append(att.name().toUpperCase()).append(", ").append(att.name().toUpperCase()).append("_TYPE);");

            for (List<Object> val : att.value()) {
                Object tuple1 = val.get(0);
                builder.append(att.name()).append(".addElement(").append(tuple1).append(");");
            }

        } else if (TypeManager.isMap(att.type())) {
            // maps
            builder.append(TypeManager.className(att.type())).append(" ").append(att.name()).append(" = ");
            builder.append("(").append(TypeManager.className(att.type())).append(")").append(" super.getOrCreate(").
                    append(att.name().toUpperCase()).append(", ").append(att.name().toUpperCase()).append("_TYPE);");

            for (List<Object> val : att.value()) {
                Object tuple1 = val.get(0);
                Object tuple2 = val.get(1);
                builder.append(att.name()).append(".put(").append(tuple1).append(",").append(tuple2).append(");");
            }

        } else if (TypeManager.isMatrix(att.type())) {
            // matrices
            builder.append(TypeManager.className(att.type())).append(" ").append(att.name()).append(" = ");
            builder.append("(").append(TypeManager.className(att.type())).append(")").append(" super.getOrCreate(").
                    append(att.name().toUpperCase()).append(", ").append(att.name().toUpperCase()).append("_TYPE);");
            for (List<Object> val : att.value()) {
                Object row = val.get(0);
                Object column = val.get(1);
                Object value = val.get(2);
                builder.append(att.name()).append(".add(").append(row).append(",").append(column).append(",").append(value).append(");");
            }

        } else if (TypeManager.isTree(att.type())) {
            // trees
            builder.append(TypeManager.className(att.type())).append(" ").append(att.name()).append(" = ");
            builder.append("(").append(TypeManager.className(att.type())).append(")").append(" super.getOrCreate(").
                    append(att.name().toUpperCase()).append(", ").append(att.name().toUpperCase()).append("_TYPE);");
            List<Object> keys = att.value().get(0);
            Object value = att.value().get(1).get(0);

            if (TypeManager.isNDTree(att.type())) {
                List<Object> minBound = att.value().get(2);
                List<Object> maxBound = att.value().get(3);
                List<Object> resolution = att.value().get(4);

                builder.append(att.name()).append(".setMinBound(").
                        append("new double[] {").
                        append(minBound.toString().replace("[", "").replace("]", "")).
                        append("}").append(");");

                builder.append(att.name()).append(".setMaxBound(").
                        append("new double[] {").
                        append(maxBound.toString().replace("[", "").replace("]", "")).
                        append("}").append(");");

                builder.append(att.name()).append(".setResolution(").
                        append("new double[] {").
                        append(resolution.toString().replace("[", "").replace("]", "")).
                        append("}").append(");");

            }

            builder.append(att.name()).append(".insert(").
                    append("new double[] {").
                    append(keys.toString().replace("[", "").replace("]", "")).
                    append("}").append(",").append(value).append(");");

        }

        return builder;

    }

}
