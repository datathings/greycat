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
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


public class ModelChecker {


    @SuppressWarnings("Duplicates")
    public void check(File content) throws Exception {
        final Set<String> classifiers = new HashSet<>();

        BufferedTokenStream tokens = new CommonTokenStream(new greycat.language.GreyCatModelLexer(new ANTLRFileStream(content.getAbsolutePath())));
        greycat.language.GreyCatModelParser parser = new greycat.language.GreyCatModelParser(tokens);
        greycat.language.GreyCatModelParser.ModelDclContext modelDclCtx = parser.modelDcl();

        // enums
        for (greycat.language.GreyCatModelParser.EnumDclContext enumDclCxt : modelDclCtx.enumDcl()) {
            String enumFqn = enumDclCxt.name.getText();
            if (classifiers.contains(enumFqn)) {
                long line = enumDclCxt.getStart().getLine();
                raiseModelCheckingException(line, enumFqn + " is not an unique identifier");
            }
            classifiers.add(enumFqn);
        }

        // classes
        for (greycat.language.GreyCatModelParser.ClassDclContext classDclCxt : modelDclCtx.classDcl()) {
            String classFqn = classDclCxt.name.getText();
            if (classifiers.contains(classFqn)) {
                long line = classDclCxt.getStart().getLine();
                raiseModelCheckingException(line, classFqn + " is not an unique identifier");
            }
            classifiers.add(classFqn);

            // parents
            if (classDclCxt.parentDcl() != null) {
                String parentClassFqn = classDclCxt.parentDcl().name.getText();
                if (!isClassifierDeclared(parentClassFqn, modelDclCtx)) {
                    long line = classDclCxt.parentDcl().getStart().getLine();
                    raiseModelCheckingException(line, parentClassFqn + " is specified as parent of " + classFqn + " but is not declared");
                }
            }

            // attributes (nothing to check)


            // relations
            for (greycat.language.GreyCatModelParser.RelationDclContext relDecCxt : classDclCxt.relationDcl()) {
                String relName, relType;
                boolean isToOne = relDecCxt.toOneDcl() != null;
                if (isToOne) {
                    // toOne
//                    relName = relDecCxt.toOneDcl().name.getText();
                    relType = relDecCxt.toOneDcl().type.getText();
                } else {
                    // toMany
//                    relName = relDecCxt.toManyDcl().name.getText();
                    relType = relDecCxt.toManyDcl().type.getText();
                }
                if (!isClassifierDeclared(relType, modelDclCtx)) {
                    long line = relDecCxt.getStart().getLine();
                    raiseModelCheckingException(line, relType + " is specified as relation of " + classFqn + " but is not declared");
                }

                if (!isToOne) {
                    // relation indexes
                    if (relDecCxt.toManyDcl().relationIndexDcl() != null) {
                        long line = relDecCxt.toManyDcl().relationIndexDcl().getStart().getLine();
                        greycat.language.GreyCatModelParser.IndexedAttributesDclContext idxAttsDclCtx =
                                relDecCxt.toManyDcl().relationIndexDcl().indexedAttributesDcl();

                        for (TerminalNode relationIdxIdent : idxAttsDclCtx.IDENT()) {
                            String relIndexedAttName = relationIdxIdent.getText();
                            if (!isAttOfCassifier(relType, relIndexedAttName, modelDclCtx)) {
                                raiseModelCheckingException(line, relIndexedAttName + " is specified as index of relation " + relType + " but is not an attribute of " + relType);
                            }
                        }
                    }
                }
            }

            // global indexes
            for (int i = 0; i < classDclCxt.indexDcl().size(); i++) {
                greycat.language.GreyCatModelParser.IndexDclContext indexDclCxt = classDclCxt.indexDcl().get(i);
                long line = indexDclCxt.getStart().getLine();
                for (TerminalNode idxDclIdent : indexDclCxt.indexedAttributesDcl().IDENT()) {
                    String indexedAttName = idxDclIdent.getText();

                    if (!isAttOfCassifier(classFqn, indexedAttName, modelDclCtx)) {
                        raiseModelCheckingException(line, indexedAttName + " is specified as index of " + classFqn + " but is not an attribute of " + classFqn);
                    }
                }
            }
        }
    }

    private boolean isAttOfCassifier(String classifier, String att, greycat.language.GreyCatModelParser.ModelDclContext modelDclCtx) {
        for (greycat.language.GreyCatModelParser.ClassDclContext classDclCxt : modelDclCtx.classDcl()) {
            String classFqn = classDclCxt.name.getText();
            if (classFqn.equals(classifier)) {
                for (greycat.language.GreyCatModelParser.AttributeDclContext attDcl : classDclCxt.attributeDcl()) {
                    String attName = attDcl.name.getText();
                    if (attName.equals(att)) {
                        return true;
                    }
                }
                // recursively check parents
                if (classDclCxt.parentDcl() != null) {
                    return isAttOfCassifier(classDclCxt.parentDcl().name.getText(), att, modelDclCtx);
                }
            }
        }

        return false;
    }

    private boolean isClassifierDeclared(String fqn, greycat.language.GreyCatModelParser.ModelDclContext modelDclCtx) {

        // classes
        for (greycat.language.GreyCatModelParser.ClassDclContext classDclCxt : modelDclCtx.classDcl()) {
            String classFqn = classDclCxt.name.getText();
            if (classFqn.equals(fqn)) {
                return true;
            }

        }
        // enums
        for (greycat.language.GreyCatModelParser.EnumDclContext enumDclCtx : modelDclCtx.enumDcl()) {
            String enumFqn = enumDclCtx.name.getText();
            if (enumFqn.equals(fqn)) {
                return true;
            }
        }
        return false;

    }

    private void raiseModelCheckingException(long line, String message) {
        throw new RuntimeException("Line " + line + ": " + message);
    }

}