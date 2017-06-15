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

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Model {

    private final Map<String, Classifier> classifierMap;

    public Model() {
        classifierMap = new HashMap<>();
    }

    public Classifier[] classifiers() {
        return classifierMap.values().toArray(new Classifier[classifierMap.size()]);
    }

    public void addClassifier(Classifier classifier) {
        classifierMap.put(classifier.name(), classifier);
    }

    public Classifier get(String fqn) {
        return classifierMap.get(fqn);
    }


    public void parse(File content) throws Exception {
        build(new ANTLRFileStream(content.getAbsolutePath()));
    }

    private void build(ANTLRInputStream in) {
        BufferedTokenStream tokens = new CommonTokenStream(new GreyCatModelLexer(in));
        GreyCatModelParser parser = new GreyCatModelParser(tokens);
        GreyCatModelParser.ModelDclContext modelDclCtx = parser.modelDcl();

        // enums
        for (GreyCatModelParser.EnumDclContext enumDclCtx : modelDclCtx.enumDcl()) {
            String fqn = enumDclCtx.name.getText();
            final Enum enumClass = getOrCreateAndAddEnum(fqn);
            for (TerminalNode literal : enumDclCtx.enumLiteralsDcl().IDENT()) {
                enumClass.addLiteral(literal.getText());
            }
        }

        // classes
        for (GreyCatModelParser.ClassDclContext classDclCxt : modelDclCtx.classDcl()) {
            String classFqn = classDclCxt.name.getText();;
            final Class newClass = getOrCreateAndAddClass(classFqn);

            // parents
            if (classDclCxt.parentDcl() != null) {
                final Class newClassTT = getOrCreateAndAddClass(classDclCxt.parentDcl().name.getText());
                newClass.setParent(newClassTT);
            }

            // attributes
            for (GreyCatModelParser.AttributeDclContext attDcl : classDclCxt.attributeDcl()) {
                String name = attDcl.name.getText();
                GreyCatModelParser.AttributeTypeDclContext attTypeDclCxt = attDcl.attributeTypeDcl();
                String value = attTypeDclCxt.getText();
                boolean isArray = false;
                if (value.endsWith("[]")) {
                    value = value.substring(0, value.length() - 2);
                    isArray = true;
                }

                final Attribute attribute = new Attribute(name, value, isArray);
                newClass.addProperty(attribute);
            }

            // relations
            for (GreyCatModelParser.RelationDclContext relDclCxt : classDclCxt.relationDcl()) {
                String name, type;
                boolean isToOne = relDclCxt.toOneDcl() != null;
                if (isToOne) {
                    // toOne
                    name = relDclCxt.toOneDcl().name.getText();
                    type = relDclCxt.toOneDcl().type.getText();
                } else {
                    // toMany
                    name = relDclCxt.toManyDcl().name.getText();
                    type = relDclCxt.toManyDcl().type.getText();
                }
                final Relation relation = new Relation(name, type, isToOne);

                if (!isToOne) {
                    // relation keys
                    if (relDclCxt.toManyDcl().relationIndexDcl() != null) {
                        GreyCatModelParser.IndexedAttributesDclContext idxAttDclCtx =
                                relDclCxt.toManyDcl().relationIndexDcl().indexedAttributesDcl();

                        for (TerminalNode idxAttIdent : idxAttDclCtx.IDENT()) {
                            relation.addIndexedAttribute(idxAttIdent.getText());
                        }
                    }
                }

                newClass.addProperty(relation);
            }

            // global keys
            for (int i = 0; i < classDclCxt.keyDcl().size(); i++) {
                GreyCatModelParser.KeyDclContext keyDclCxt = classDclCxt.keyDcl().get(i);
                String idxName = newClass.name() + "Key" + i;
                if (keyDclCxt.name != null) {
                    idxName = keyDclCxt.name.getText();
                }
                Key idx = new Key(idxName);
                for (TerminalNode idxDclIdent : keyDclCxt.indexedAttributesDcl().IDENT()) {
                    Property indexedProperty = getProperty(newClass, idxDclIdent.getText());
                    Attribute att = (Attribute) indexedProperty;
                    idx.addAttribute(att);
                }
                if (keyDclCxt.withTimeDcl() != null) {
                    idx.setWithTime(true);
                }
                newClass.addKey(idx);

            }
        }
    }

    private Property getProperty(Class clazz, String propertyName) {
        Property p = clazz.getProperty(propertyName);
        if (p != null) {
            return p;
        } else {
            return getProperty(clazz.parent(), propertyName);
        }
    }

    private Class getOrCreateAndAddClass(String fqn) {
        Class previous = (Class) this.get(fqn);
        if (previous != null) {
            return previous;
        }
        previous = new Class(fqn);
        this.addClassifier(previous);
        return previous;
    }

    private Enum getOrCreateAndAddEnum(String fqn) {
        Enum previous = (Enum) this.get(fqn);
        if (previous != null) {
            return previous;
        }
        previous = new Enum(fqn);
        this.addClassifier(previous);
        return previous;
    }

}
