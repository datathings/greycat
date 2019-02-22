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
package greycat.importer.util;

import greycat.base.BaseTaskResult;
import greycat.TaskResult;
import greycat.TaskResultIterator;
import greycat.utility.Tuple;

import java.io.*;

public class IterableLines extends BaseTaskResult<String> {

    private final String _path;

    private Reader reader = null;
    private BufferedReader bufferedReader = null;

    public IterableLines(String p_path) {
        super(null, false);
        this._path = p_path;
    }

    @Override
    public TaskResultIterator iterator() {
        bufferedReader = null;
        try {
            File openFile = new File(_path);
            if (openFile.exists()) {
                reader = new FileReader(openFile);
            } else {
                reader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(_path));
            }
            bufferedReader = new BufferedReader(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final BufferedReader final_buffer = bufferedReader;
        return new TaskResultIterator() {

            private int _i = 0;

            @Override
            public synchronized Object next() {
                _i++;
                try {
                    String line = final_buffer.readLine();
                    if (line == null) {
                        final_buffer.close();
                    }
                    return line;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public final synchronized boolean hasNext() {
                try {
                    return final_buffer.ready();
                } catch (Throwable e) {
                    //e.printStackTrace();
                    return false;
                }
            }

            @Override
            public synchronized Tuple nextWithIndex() {
                final int c_i = _i;
                _i++;
                try {
                    String line = final_buffer.readLine();
                    if (line == null) {
                        final_buffer.close();
                        return null;
                    } else {
                        return new Tuple(c_i, line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    @Override
    public String get(int index) {
        return null;
    }

    @Override
    public TaskResult<String> set(int index, String input) {
        return null;
    }

    @Override
    public TaskResult<String> allocate(int index) {
        return null;
    }

    @Override
    public TaskResult<String> add(String input) {
        return null;
    }

    @Override
    public TaskResult<String> clear() {
        return null;
    }

    @Override
    public TaskResult<String> clone() {
        return null;
    }

    @Override
    public void free() {
        if(bufferedReader != null){
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(reader != null){
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int size() {
        return -1;
    }

    @Override
    public Object[] asArray() {
        return new Object[0];
    }

    @Override
    public TaskResult<String> fillWith(TaskResult<String> source) {
        return null;
    }

}
