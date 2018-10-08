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
package greycat.internal;

import greycat.Graph;

import java.io.*;

/**
 * {@ignore ts }
 */
public class CoreGraphLogFile extends CoreGraphLog {

    private final File dir;
    private final long maxSize;

    private final Object mutex;
    private File file;
    private OutputStream stream;

    public CoreGraphLogFile(final Graph g, final String target, final String maxSize) {
        super(g);
        this.mutex = new Object();
        this.dir = new File(target);
        this.dir.mkdirs();
        if (!this.dir.exists() || !this.dir.isDirectory()) {
            throw new RuntimeException("log bad init, not a directory: " + target);
        }
        try {
            if (maxSize.endsWith("GB")) {
                this.maxSize = Long.parseLong(maxSize.substring(0, maxSize.length() - 2).trim()) * 1024L * 1024L * 1024L;
            } else if (maxSize.endsWith("MB")) {
                this.maxSize = Long.parseLong(maxSize.substring(0, maxSize.length() - 2).trim()) * 1024L * 1024L;
            } else if (maxSize.endsWith("KB")) {
                this.maxSize = Long.parseLong(maxSize.substring(0, maxSize.length() - 2).trim()) * 1024L;
            } else {
                this.maxSize = Long.parseLong(maxSize);
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Size \"" + maxSize + "\" is not numberic");
        }
        if (this.maxSize <= 0L) {
            throw new IllegalArgumentException("Size must be > 0, but is " + this.maxSize);
        }
        this.file = new File(this.dir, "out.log");
        try {
            this.stream = new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeMessage(final StringBuilder builder) {
        byte[] raw = builder.toString().getBytes();
        synchronized (mutex) {
            try {
                if (file.length() > this.maxSize) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!file.renameTo(new File(dir, "out." + System.currentTimeMillis() + ".log"))) {
                        System.err.println("error in logger while renaming rolling out file");
                    }
                    this.file = new File(this.dir, "out.log");
                    this.stream = new FileOutputStream(file);
                }
                stream.write(raw);
                stream.write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
