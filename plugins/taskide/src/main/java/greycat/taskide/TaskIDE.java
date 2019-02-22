/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
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
package greycat.taskide;

import greycat.Graph;
import greycat.Type;
import greycat.internal.heap.HeapBuffer;
import greycat.struct.Buffer;
import greycat.websocket.WSServer;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import greycat.internal.task.TaskHelper;
import greycat.plugin.ActionDeclaration;
import greycat.plugin.ActionRegistry;

public class TaskIDE {

    public static void attach(final WSServer server, final Graph graph) {
        server.addHandler("taskide", new ResourceHandler(new ClassPathResourceManager(TaskIDE.class.getClassLoader(), "taskide")).addWelcomeFiles("index.html").setDirectoryListingEnabled(false));
        server.addHandler("actionregistry", new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
                Buffer builder = new HeapBuffer();
                builder.writeString("[");
                ActionRegistry registry = graph.actionRegistry();
                ActionDeclaration[] declarations = registry.declarations();
                for (int i = 0; i < declarations.length; i++) {
                    ActionDeclaration declaration = declarations[i];
                    if (i != 0) {
                        builder.writeString(",");
                    }
                    builder.writeString("{\"name\":");
                    TaskHelper.serializeString(declaration.name(), builder, false);
                    builder.writeString(",\"description\":");
                    TaskHelper.serializeString(declaration.description(), builder, false);
                    byte[] params = declaration.params();
                    if (params != null) {
                        builder.writeString(",\"params\":[");
                        for (int j = 0; j < params.length; j++) {
                            if (j != 0) {
                                builder.writeString(",");
                            }
                            TaskHelper.serializeString(Type.typeName(params[j]), builder, false);
                        }
                        builder.writeString("]");
                    }
                    builder.writeString("}");
                }
                builder.writeString("]");
                httpServerExchange.getResponseHeaders().add(new HttpString("Access-Control-Allow-Origin"), "*");
                httpServerExchange.setStatusCode(StatusCodes.OK);
                httpServerExchange.getResponseSender().send(builder.toString());
            }
        });

    }

}
