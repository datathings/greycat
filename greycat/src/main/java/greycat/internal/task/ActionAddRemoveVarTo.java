/**
 * Copyright 2017-2018 The GreyCat Authors.  All rights reserved.
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
package greycat.internal.task;

import greycat.*;
import greycat.base.BaseNode;
import greycat.struct.Buffer;
import greycat.struct.Relation;

class ActionAddRemoveVarTo implements Action {

    private final String _name;
    private final String _varFrom;
    private final boolean _isAdd;

    ActionAddRemoveVarTo(final boolean isAdd, final String name, final String varFrom) {
        this._isAdd = isAdd;
        this._name = name;
        this._varFrom = varFrom;
    }

    @Override
    public void eval(final TaskContext ctx) {
        final TaskResult previousResult = ctx.result();
        final TaskResult savedVar = ctx.variable(ctx.template(_varFrom));
        if (previousResult != null && savedVar != null) {
            final String relName = ctx.template(_name);
            final TaskResultIterator previousResultIt = previousResult.iterator();
            Object iter = previousResultIt.next();
            while (iter != null) {
                if (iter instanceof BaseNode) {
                    final TaskResultIterator savedVarIt = savedVar.iterator();
                    Object toAddIter = savedVarIt.next();
                    while (toAddIter != null) {
                        if (toAddIter instanceof BaseNode) {
                            final Node castedToAddIter = (Node) toAddIter;
                            final Node castedIter = (Node) iter;
                            final int type = castedIter.type(relName);
                            if (type == Type.INDEX) {
                                final Index index = (Index) castedIter.get(relName);
                                if(_isAdd){
                                    index.update(castedToAddIter);
                                } else {
                                    index.unindex(castedToAddIter);
                                }
                            } else {
                                final Relation castedIterRel = (Relation) castedIter.getOrCreate(relName, Type.RELATION);
                                if (_isAdd) {
                                    castedIterRel.add(castedToAddIter.id());
                                } else {
                                    castedIterRel.remove(castedToAddIter.id());
                                }
                            }
                        }
                        toAddIter = savedVarIt.next();
                    }
                }
                iter = previousResultIt.next();
            }
        }
        ctx.continueTask();
    }

    @Override
    public void serialize(final Buffer builder) {
        if (_isAdd) {
            builder.writeString(CoreActionNames.ADD_VAR_TO);
        } else {
            builder.writeString(CoreActionNames.REMOVE_VAR_FROM);
        }
        builder.writeChar(Constants.TASK_PARAM_OPEN);
        TaskHelper.serializeString(_name, builder, true);
        builder.writeChar(Constants.TASK_PARAM_SEP);
        TaskHelper.serializeString(_varFrom, builder, true);
        builder.writeChar(Constants.TASK_PARAM_CLOSE);
    }

    @Override
    public final String name() {
        return (_isAdd ? CoreActionNames.ADD_VAR_TO : CoreActionNames.REMOVE_VAR_FROM);
    }

}
