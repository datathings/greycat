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

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Model {

    protected final Map<String, Class> classes;
    protected final Map<String, Constant> constants;
    protected final Map<String, Index> indexes;
    private final Map<String, CustomType> customTypes;

    public Model() {
        classes = new HashMap<String, Class>();
        constants = new HashMap<String, Constant>();
        indexes = new HashMap<String, Index>();
        customTypes = new HashMap<String, CustomType>();
    }

    public void parse(File content) throws Exception {
        internal_parse(CharStreams.fromFileName(content.getAbsolutePath()), new Resolver() {
            @Override
            public CharStream resolver(String name) {
                try {
                    File subFile = new File(content.getParentFile().getCanonicalFile(), name);
                    if (subFile.exists()) {
                        return CharStreams.fromFileName(subFile.getAbsolutePath());
                    } else {
                        subFile = new File(content.getParentFile().getCanonicalFile(), name + ".gcm");
                        if (subFile.exists()) {
                            return CharStreams.fromFileName(subFile.getAbsolutePath());
                        } else {
                            return null;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }

    public void parseResource(String name, ClassLoader classLoader) throws IOException {
        InputStream is = classLoader.getResourceAsStream(name);
        Resolver resolver = new Resolver() {
            @Override
            public CharStream resolver(String name) {
                InputStream sub = classLoader.getResourceAsStream(name);
                if (sub != null) {
                    try {
                        return CharStreams.fromStream(sub);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        internal_parse(CharStreams.fromStream(is), resolver);
    }

    public Constant[] globalConstants() {
        return constants.values().toArray(new Constant[constants.size()]);
    }

    public Index[] globalIndexes() {
        return indexes.values().toArray(new Index[indexes.size()]);
    }

    public CustomType[] customTypes() {
        return customTypes.values().toArray(new CustomType[customTypes.size()]);
    }

    public Class[] classes() {
        return classes.values().toArray(new Class[classes.size()]);
    }

    private Set<String> alreadyLoaded = new HashSet<String>();

    private MessageDigest md;

    interface Resolver {
        CharStream resolver(String name);
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private void internal_parse(CharStream in, Resolver resolver) {
        if (md == null) {
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        String sha1 = convertToHex(md.digest(in.getText(new Interval(0, in.size())).getBytes()));
        if (alreadyLoaded.contains(sha1)) {
            return;
        } else {
            //circular dependency protection
            alreadyLoaded.add(sha1);
        }
        BufferedTokenStream tokens = new CommonTokenStream(new GreyCatModelLexer(in));
        GreyCatModelParser parser = new GreyCatModelParser(tokens);
        GreyCatModelParser.ModelDclContext modelDclCtx = parser.modelDcl();
        //first subImport
        modelDclCtx.importDcl().forEach(importDclContext -> {
            String subPath = importDclContext.STRING().getText().replace("\"", "");
            CharStream subStream = resolver.resolver(subPath);
            if (subStream == null) {
                throw new RuntimeException("Import not resolved " + subPath);
            }
            try {
                internal_parse(subStream, resolver);
            } catch (Exception e) {
                throw new RuntimeException("Parse Error while parsing " + subPath, e);
            }
        });
        // constants
        for (GreyCatModelParser.ConstDclContext constDclCtx : modelDclCtx.constDcl()) {
            String const_name = constDclCtx.name.getText();
            Constant c = constants.get(const_name);
            if (c == null) {
                c = new Constant(const_name);
                constants.put(const_name, c);
            }
            c.setType(getType(constDclCtx.typeDcl()));
            String value = null;
            if (constDclCtx.constValueDcl() != null) {
                if (constDclCtx.constValueDcl().simpleValueDcl() != null) {
                    value = constDclCtx.constValueDcl().simpleValueDcl().getText();
                } else if (constDclCtx.constValueDcl().taskValueDcl() != null) {
                    GreyCatModelParser.TaskValueDclContext taskDcl = constDclCtx.constValueDcl().taskValueDcl();
                    value = taskDcl.getText();
                }
            }
            c.setValue(value);
        }
        // classes
        for (GreyCatModelParser.ClassDclContext classDclCtx : modelDclCtx.classDcl()) {
            String classFqn = classDclCtx.name.getText();
            Class newClass = getOrAddClass(classFqn);
            // parents
            if (classDclCtx.parentDcl() != null) {
                final Class parentClass = getOrAddClass(classDclCtx.parentDcl().IDENT().getText());
                newClass.setParent(parentClass);
            }
            // attributes
            for (GreyCatModelParser.AttributeDclContext attDcl : classDclCtx.attributeDcl()) {
                String name = attDcl.name.getText();
                final Attribute attribute = newClass.getOrCreateAttribute(name);
                attribute.setType(getType(attDcl.typeDcl()));
            }
            // relations
            for (GreyCatModelParser.RelationDclContext relDclCtx : classDclCtx.relationDcl()) {
                newClass.getOrCreateRelation(relDclCtx.name.getText()).setType(relDclCtx.type.getText());
            }
            // references
            for (GreyCatModelParser.ReferenceDclContext refDclCtx : classDclCtx.referenceDcl()) {
                newClass.getOrCreateReference(refDclCtx.name.getText()).setType(refDclCtx.type.getText());
            }
            // local indexes
            for (GreyCatModelParser.LocalIndexDclContext localIndexDclCtx : classDclCtx.localIndexDcl()) {
                final Index index = newClass.getOrCreateIndex(localIndexDclCtx.name.getText());
                index.setType(localIndexDclCtx.type.getText());
                final Class indexedClass = getOrAddClass(index.type());
                for (TerminalNode idxDclIdent : localIndexDclCtx.indexAttributesDcl().IDENT()) {
                    index.addAttributeRef(new AttributeRef(indexedClass.getOrCreateAttribute(idxDclIdent.getText())));
                }
            }
            // constants
            for (GreyCatModelParser.ConstDclContext constDclCtx : classDclCtx.constDcl()) {
                Constant constant = newClass.getOrCreateConstant(constDclCtx.name.getText());
                constant.setType(getType(constDclCtx.typeDcl()));
                String value = null;
                if (constDclCtx.constValueDcl() != null) {
                    if (constDclCtx.constValueDcl().simpleValueDcl() != null) {
                        value = constDclCtx.constValueDcl().simpleValueDcl().getText();
                    } else if (constDclCtx.constValueDcl().taskValueDcl() != null) {
                        GreyCatModelParser.TaskValueDclContext taskDcl = constDclCtx.constValueDcl().taskValueDcl();
                        value = taskDcl.getText();
                    }
                }
                constant.setValue(value);
            }
        }

        // opposite management
        for (GreyCatModelParser.ClassDclContext classDclCtx : modelDclCtx.classDcl()) {
            String classFqn = classDclCtx.name.getText();
            Class classType = classes.get(classFqn);

            // relations
            for (GreyCatModelParser.RelationDclContext relDclCtx : classDclCtx.relationDcl()) {
                if (relDclCtx.oppositeDcl() != null) {
                    Relation rel = (Relation) classType.properties.get(relDclCtx.name.getText());
                    Class oppositeClass = classes.get(rel.type());
                    Object oppositeProperty = oppositeClass.properties.get(relDclCtx.oppositeDcl().name.getText());
                    if (oppositeProperty instanceof Relation) {
                        Relation oppositeRel = (Relation) oppositeProperty;
                        rel.setOpposite(new Opposite(oppositeRel));
                        oppositeRel.setOpposite(new Opposite(rel));
                    } else if (oppositeProperty instanceof Reference) {
                        Reference oppositeRef = (Reference) oppositeProperty;
                        rel.setOpposite(new Opposite(oppositeRef));
                        oppositeRef.setOpposite(new Opposite(rel));
                    }
                }
            }
            // references
            for (GreyCatModelParser.ReferenceDclContext refDclCtx : classDclCtx.referenceDcl()) {
                if (refDclCtx.oppositeDcl() != null) {
                    Reference ref = (Reference) classType.properties.get(refDclCtx.name.getText());
                    Class oppositeClass = classes.get(ref.type());
                    Object oppositeProperty = oppositeClass.properties.get(refDclCtx.oppositeDcl().name.getText());
                    if (oppositeProperty instanceof Relation) {
                        Relation oppositeRel = (Relation) oppositeProperty;
                        ref.setOpposite(new Opposite(oppositeRel));
                        oppositeRel.setOpposite(new Opposite(ref));
                    } else if (oppositeProperty instanceof Reference) {
                        Reference oppositeRef = (Reference) oppositeProperty;
                        ref.setOpposite(new Opposite(oppositeRef));
                        oppositeRef.setOpposite(new Opposite(ref));
                    }
                }
            }
        }

        // indexes
        for (GreyCatModelParser.GlobalIndexDclContext globalIdxDclContext : modelDclCtx.globalIndexDcl()) {
            final String name = globalIdxDclContext.name.getText();
            final String type = globalIdxDclContext.type.getText();
            final Index index = getOrAddGlobalIndex(name, type);
            final Class indexedClass = getOrAddClass(index.type());
            for (TerminalNode idxDclIdent : globalIdxDclContext.indexAttributesDcl().IDENT()) {
                index.addAttributeRef(new AttributeRef(indexedClass.getOrCreateAttribute(idxDclIdent.getText())));
            }
        }
        // custom types
        for (GreyCatModelParser.CustomTypeDclContext customTypeDclCtx : modelDclCtx.customTypeDcl()) {
            String customTypeName = customTypeDclCtx.name.getText();
            final CustomType newCustomType = getOrAddCustomType(customTypeName);
            // attributes
            for (GreyCatModelParser.AttributeDclContext attDcl : customTypeDclCtx.attributeDcl()) {
                Attribute att = newCustomType.getOrCreateAttribute(attDcl.name.getText());
                if (attDcl.typeDcl().builtInTypeDcl() != null) {
                    att.setType(attDcl.typeDcl().builtInTypeDcl().getText());
                } else if (attDcl.typeDcl().customBuiltTypeDcl() != null) {
                    att.setType(attDcl.typeDcl().customBuiltTypeDcl().getText());
                }
            }
            // constants
            for (GreyCatModelParser.ConstDclContext constDclCtx : customTypeDclCtx.constDcl()) {
                Constant constant = newCustomType.getOrCreateConstant(constDclCtx.name.getText());
                constant.setType(getType(constDclCtx.typeDcl()));
                String value = null;
                if (constDclCtx.constValueDcl() != null) {
                    if (constDclCtx.constValueDcl().simpleValueDcl() != null) {
                        value = constDclCtx.constValueDcl().simpleValueDcl().getText();
                    } else if (constDclCtx.constValueDcl().taskValueDcl() != null) {
                        GreyCatModelParser.TaskValueDclContext taskDcl = constDclCtx.constValueDcl().taskValueDcl();
                        value = taskDcl.getText();
                    }
                }
                constant.setValue(value);
            }
        }
    }

    public void consolidate() {
        classes.values().forEach(aClass -> {
            List<String> toRemove = new ArrayList<String>();
            aClass.properties().forEach(o -> {
                if (o instanceof Attribute) {
                    Attribute attribute = (Attribute) o;
                    Attribute parent = aClass.attributeFromParent(attribute.name());
                    if (parent != null) {
                        toRemove.add(attribute.name());
                        attribute.references.forEach(attributeRef -> attributeRef.update(parent));
                        attribute.references.clear();
                    }
                }
            });
            toRemove.forEach(s -> aClass.properties.remove(s));
        });
    }

    private String getType(GreyCatModelParser.TypeDclContext typeDclContext) {
        String type = null;
        if (typeDclContext.builtInTypeDcl() != null) {
            type = typeDclContext.builtInTypeDcl().getText();
        } else if (typeDclContext.customBuiltTypeDcl() != null) {
            type = typeDclContext.customBuiltTypeDcl().getText();
        }
        return type;
    }

    private Class getOrAddClass(String fqn) {
        Class c = classes.get(fqn);
        if (c == null) {
            c = new Class(fqn);
            classes.put(fqn, c);
        }
        return c;
    }

    private Index getOrAddGlobalIndex(String name, String type) {
        Index gi = indexes.get(name);
        if (gi == null) {
            gi = new Index(name);
            gi.setType(type);
            indexes.put(name, gi);
        }
        return gi;
    }

    private CustomType getOrAddCustomType(String name) {
        CustomType ct = customTypes.get(name);
        if (ct == null) {
            ct = new CustomType(name);
            customTypes.put(name, ct);
        }
        return ct;
    }

}
