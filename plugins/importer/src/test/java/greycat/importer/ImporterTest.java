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
package greycat.importer;

import greycat.*;
import org.junit.Assert;
import org.junit.Test;
import greycat.importer.ImporterActions;
import greycat.importer.ImporterPlugin;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static greycat.importer.ImporterActions.readFiles;
import static greycat.importer.ImporterActions.readLines;
import static greycat.internal.task.CoreActions.action;
import static greycat.internal.task.CoreActions.defineAsGlobalVar;
import static greycat.internal.task.CoreActions.inject;
import static greycat.Tasks.newTask;
import static greycat.Tasks.then;

public class ImporterTest {

    @Test
    public void testReadLines() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("d/MM/yyyy|HH:mm");
        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(connectionResult -> {

            Node newNode = g.newNode(0, 0);
            //final Task t = readLines("/Users/duke/dev/mwDB/plugins/importer/src/test/resources/smarthome/smarthome_1.T15.txt")
            final Task t =
                    then(readLines("smarthome/smarthome_mini_1.T15.csv"))
                            .forEach(
                                    newTask().ifThen(ctx -> !ctx.result().get(0).toString().startsWith("1:Date"),
                                            newTask().thenDo(context -> {
                                                String[] line = context.result().get(0).toString().split(" ");
                                                try {
                                                    long time = dateFormat.parse(line[0] + "|" + line[1]).getTime();
                                                    double value = Double.parseDouble(line[2]);
                                                    newNode.travelInTime(time, timedNode -> {
                                                        timedNode.set("value", Type.DOUBLE, value);
                                                        context.continueWith(context.wrap(timedNode));
                                                    });
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                    context.continueWith(null);
                                                }
                                            })
                                    ));
            t.execute(g, null);
        });
    }

    @Test
    public void testReadFilesStaticMethod() {
        File fileChecked = new File(this.getClass().getClassLoader().getResource("smarthome").getPath());
        final File[] subFiles = fileChecked.listFiles();

        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                final int[] nbFile = new int[1];
                Task t = newTask().then(readFiles("smarthome")).forEach(newTask().thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        String filePath = (String) ctx.result().get(0);
                        Assert.assertEquals(subFiles[nbFile[0]].getAbsolutePath(), filePath);
                        nbFile[0]++;
                        ctx.continueWith(null);
                    }
                }));
                t.execute(g, null);
                Assert.assertEquals(subFiles.length, nbFile[0]);
            }
        });
    }

    @Test
    public void testReadFilesActionWithTemplate() {
        File fileChecked = new File(this.getClass().getClassLoader().getResource("smarthome").getPath());
        final File[] subFiles = fileChecked.listFiles();

        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                final int[] nbFile = new int[1];
                Task t = newTask()
                        .then(inject("smarthome"))
                        .then(defineAsGlobalVar("fileName"))
                        .then(action(ImporterActions.READFILES, "{{fileName}}"))
                        .forEach(newTask().thenDo(new ActionFunction() {
                            @Override
                            public void eval(TaskContext ctx) {
                                String file = (String) ctx.result().get(0);
                                Assert.assertEquals(subFiles[nbFile[0]].getAbsolutePath(), file);
                                nbFile[0]++;
                                ctx.continueWith(null);
                            }
                        }));
                t.execute(g, null);

                Assert.assertEquals(subFiles.length, nbFile[0]);
            }
        });
    }

    @Test
    public void testReadFilesOnFile() throws UnsupportedEncodingException, MalformedURLException, URISyntaxException {
        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        URL urlFIle = this.getClass().getClassLoader().getResource("smarthome/readme.md");
        URL urlFIle2 = this.getClass().getClassLoader().getResource(URLDecoder.decode("folder with spaces in name/aFile.csv", "UTF-8"));

        File expectecFile = new File(urlFIle.toURI());
        File expectedFile2 = new File(urlFIle2.toURI());

        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                final int[] nbFile = new int[1];
                Task t = newTask()
                        .then(readFiles(urlFIle.getPath()))
                        .forEach(
                                newTask().thenDo(new ActionFunction() {
                                    @Override
                                    public void eval(TaskContext ctx) {
                                        String file = (String) ctx.result().get(0);
                                        Assert.assertEquals(expectecFile.getAbsolutePath(), file);
                                        nbFile[0]++;
                                        ctx.continueWith(null);
                                    }
                                })
                        )
                        .then(action(ImporterActions.READFILES, urlFIle2.getPath()))
                        .forEach(
                                newTask().thenDo(new ActionFunction() {
                                    @Override
                                    public void eval(TaskContext ctx) {
                                        String file = (String) ctx.result().get(0);
                                        try {
                                            Assert.assertEquals(URLDecoder.decode(expectedFile2.getAbsolutePath(), "UTF-8"), file);
                                        } catch (UnsupportedEncodingException ex) {
                                            Assert.fail(ex.getMessage());
                                        }
                                        nbFile[0]++;
                                        ctx.continueWith(null);
                                    }
                                })
                        );
                t.execute(g, null);

                Assert.assertEquals(2, nbFile[0]);
            }
        });
    }

    //@Test
    public void testReadFileOnUnknowFile() {
        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(connectionResult -> {
            Task t = newTask().then(readFiles("nonexistent-file.csv"));
            boolean exceptionCaught = false;
            try {
                t.execute(g, null);
            } catch (RuntimeException exception) {
                exceptionCaught = true;
            }

            Assert.assertTrue(exceptionCaught);
        });
    }


    //@Test
    public void testReadFileOnIncorrectVar() {
        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean connectionResult) {
                Task t = newTask().then(action(ImporterActions.READFILES, "{{incorrectVarName}}"));

                boolean exceptionCaught = false;
                try {
                    t.execute(g, null);
                } catch (RuntimeException ex) {
                    exceptionCaught = true;
                }
                Assert.assertTrue(exceptionCaught);

            }
        });
    }


    /*
    @Test
    public void testV2() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("d/MM/yyyy|HH:mm");
        final Graph g = new GraphBuilder().withPlugin(new ImporterPlugin()).build();
        g.connect(connectionResult -> {
            Node newNode = g.newNode(0, 0);
            final Task t = task().then(readLines("smarthome/smarthome_mini_1.T15.txt"))
                    .forEach(
                            task().ifThen(ctx -> !ctx.result().get(0).toString().startsWith("1:Date"),
                                    split(" ")
                                            .then(context -> {
                                                TaskResult<String> line = context.result();
                                                try {
                                                    context.setGlobalVariable("time", context.wrap(dateFormat.parse(line.get(0) + "|" + line.get(1)).getTime()));
                                                    context.setGlobalVariable("value", context.wrap(Double.parseDouble(line.get(2))));
                                                    context.continueWith(null);
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                    context.continueWith(null);
                                                }
                                            })
                                            .setTime("{{time}}")
                                            .lookup("" + newNode.id())
                                            .setProperty("value", Type.DOUBLE, "{{value}}")
                                    //.print("insertedNode: {{result}} {{value}}")
                            ));
            t.execute(g, null);
            //t.executeWith(g, null, null, true, null); //with debug
        });
    }*/

}
