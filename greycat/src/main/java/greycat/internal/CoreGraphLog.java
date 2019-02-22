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
package greycat.internal;

import greycat.Graph;
import greycat.Log;
import greycat.plugin.TaskExecutor;

@SuppressWarnings("Duplicates")
public class CoreGraphLog implements Log {

    private static final String debug_msg = "DEBUG  ";
    private static final String info_msg = "INFO   ";
    private static final String warn_msg = "WARN   ";
    private static final String error_msg = "ERROR  ";

    Graph graph;
    boolean remote;

    public CoreGraphLog(Graph graph) {
        this.graph = graph;
    }


    @Override
    public final Log debug(String message, Object... params) {
        StringBuilder builder = new StringBuilder();
        builder.append(debug_msg);
        builder.append(processMessage(message, params));
        writeMessage(builder);
        return this;
    }

    @Override
    public final Log info(String message, Object... params) {
        StringBuilder builder = new StringBuilder();
        builder.append(info_msg);
        builder.append(processMessage(message, params));
        writeMessage(builder);
        return this;
    }

    @Override
    public final Log warn(String message, Object... params) {
        StringBuilder builder = new StringBuilder();
        builder.append(warn_msg);
        builder.append(processMessage(message, params));
        writeMessage(builder);
        return this;
    }

    @Override
    public final Log error(String message, Object... params) {
        StringBuilder builder = new StringBuilder();
        builder.append(error_msg);
        builder.append(processMessage(message, params));
        writeMessage(builder);
        return this;
    }

    @Override
    public final Log activateRemote() {
        this.remote = true;
        return this;
    }

    public void writeMessage(StringBuilder builder) {
        String msg = builder.toString();
        System.out.println(msg);
        Object str = graph.storage();
        if (str != null && remote) {
            TaskExecutor exec = (TaskExecutor) str;
            exec.log(msg);
        }
    }

    private static final char beginParam = '{';
    private static final char endParam = '}';

    private static String processMessage(String message, Object... params) {
        if (params == null || params.length == 0) {
            return message;
        }
        StringBuilder buffer = null;
        boolean previousCharfound = false;
        int param = 0;
        for (int i = 0; i < message.length(); i++) {
            char currentChar = message.charAt(i);
            if (previousCharfound) {
                if (currentChar == endParam) {
                    if (param == 0) {
                        buffer = new StringBuilder();
                        buffer.append(message.substring(0, i - 1));
                    }
                    if (params.length > param && params[param] != null) {
                        buffer.append(params[param].toString());
                    }
                    param++;
                    previousCharfound = false;
                } else {
                    if (buffer != null) {
                        buffer.append(currentChar);
                    }
                    previousCharfound = false;
                }
            } else {
                if (currentChar == beginParam) {
                    previousCharfound = true; //next round
                } else {
                    if (buffer != null) {
                        buffer.append(currentChar);
                    }
                }
            }
        }
        if (buffer != null) {
            return buffer.toString();
        } else {
            return message;
        }
    }

}
