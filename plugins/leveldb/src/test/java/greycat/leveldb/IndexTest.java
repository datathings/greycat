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
package greycat.leveldb;

import greycat.*;
import org.junit.Test;
import greycat.scheduler.NoopScheduler;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IndexTest {

    @Test
    public void testIndex() {
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            return;
        }

        Graph graph = new GraphBuilder().withScheduler(new NoopScheduler()).withStorage(new LevelDBStorage("db")).build();
        Graph graph2 = new GraphBuilder().withStorage(new LevelDBStorage("db")).build();

        final String index = "indexName";
        graph.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                graph.declareIndex(0, index, new Callback<NodeIndex>() {
                    @Override
                    public void on(NodeIndex result) {
                        graph.save(new Callback<Boolean>() {
                            @Override
                            public void on(Boolean result) {
                                graph.disconnect(new Callback<Boolean>() {
                                    @Override
                                    public void on(Boolean result) {
                                        graph2.connect(new Callback<Boolean>() {
                                            @Override
                                            public void on(Boolean result) {
                                                graph2.index(0, 0, index, new Callback<NodeIndex>() {
                                                    @Override
                                                    public void on(NodeIndex result) {
                                                        result.find(new Callback<Node[]>() {
                                                            @Override
                                                            public void on(Node[] result) {

                                                            }
                                                        }, result.world(), result.time());
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });


        try {
            Path path = Paths.get("db");
            if (Files.exists(path)) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (Exception e) {

        }

    }
}
