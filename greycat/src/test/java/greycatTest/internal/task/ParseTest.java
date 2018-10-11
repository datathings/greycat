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
package greycatTest.internal.task;

import greycat.*;
import greycat.internal.task.TaskHelper;
import greycat.plugin.ActionFactory;
import greycat.struct.Buffer;
import org.junit.Assert;
import org.junit.Test;

import static greycat.Tasks.newTask;

public class ParseTest extends AbstractActionTest {

    @Test
    public void test() {
        initGraph();
        Task t = newTask();
        t.toString();
        String toParse = "travelInTime(0).travelInWorld(0).createNode().addToVar(root).createNode().addToVar(root).createNode().addToVar(root).readVar(root).println('{{result}} avec echap \\'')";
        t.parse(toParse, graph);
        Assert.assertEquals("travelInTime(0).travelInTime(0).createNode().addToVar('root').createNode().addToVar('root').createNode().addToVar('root').readVar('root').println('{{result}} avec echap \\'')", t.toString());
        // t.execute(graph, null);
        removeGraph();
    }
/*
    @Test
    public void testAtomic() {
        initGraph();
        Task t = newTask();
        t.parse("readIndex(nodes).atomic({println(\"toto\")},\"param\")", graph);
        Assert.assertEquals();
        System.out.println(t.toString());
    }*/

    @Test
    public void testSubTask() {
        initGraph();
        Task t = newTask().parse("travelInTime(0).travelInWorld(0).readIndex(nodes).loop(0,3,{ println('->{{i}}') })", graph);
        Assert.assertEquals("travelInTime(0).travelInTime(0).readIndex('nodes').loop('0','3',{println('->{{i}}')})", t.toString());

        //t.execute(graph, null);

        Task t2 = newTask().parse("travelInTime(0).travelInWorld(0).readIndex(nodes).loopPar(0,3,{ println('->{{i}}') })", graph);
        Assert.assertEquals("travelInTime(0).travelInTime(0).readIndex('nodes').loopPar('0','3',{println('->{{i}}')})", t2.toString());

        Task t3 = newTask().parse("travelInTime(0).travelInWorld(0).readIndex(nodes).ifThen('ctx.result().size() > 0', {println('{{result[0]}}')})", graph);
        //t3.execute(graph,null);
        Assert.assertEquals("travelInTime(0).travelInTime(0).readIndex('nodes').ifThen('ctx.result().size() > 0',{println('{{result[0]}}')})", t3.toString());

        removeGraph();
    }

    @Test
    public void testDAG() {
        initGraph();
        Task mainTask = newTask();
        Task deepR = newTask().println("{{result}}");
        mainTask.ifThenElseScript("ctx.result.size() == 0", deepR, deepR);
        Assert.assertEquals("ifThenElse('ctx.result.size() == 0',0,0)#0{println('{{result}}')}", mainTask.toString());

        Task parsed = newTask().parse("ifThenElse('ctx.result.size() == 0',0,0)#0{println('{{result}}')}", graph);
        Assert.assertEquals("ifThenElse('ctx.result.size() == 0',0,0)#0{println('{{result}}')}", parsed.toString());

        removeGraph();
    }

    @Test
    public void testSCRIPT() {
        initGraph();
        Task t = newTask();
        t.parse("readIndex(nodes).script(\"print(ctx);ctx.continueTask();\")", graph);
        Assert.assertEquals("readIndex('nodes').script('print(ctx);ctx.continueTask();')", t.toString());

        Task t2 = newTask();
        t2.parse("readIndex(nodes).select('node.get(\'name\') == \"root\"')", graph);
        Assert.assertEquals("readIndex('nodes').select('node.get(\\'name\\') == \"root\"')", t2.toString());

        removeGraph();
    }


    @Test
    public void testEscape() {
        initGraph();
        Task t = newTask();
        t.parse("readIndex('nodes').select('node.get(\\'name\\')==\"root\"')", graph);
        Assert.assertEquals("readIndex('nodes').select('node.get(\\'name\\')==\"root\"')", t.toString());
        //t.execute(graph, null);
        removeGraph();
    }

    @Test
    public void testForEach() {
        initGraph();
        Task t = newTask();
        t.parse("readIndex('nodes').forEach({println('result')})", graph);
        Task t2 = newTask().readIndex("nodes").forEach(newTask().println("result"));
        Assert.assertEquals(t2.toString(), t.toString());
        Assert.assertEquals("readIndex('nodes').forEach({println('result')})", t.toString());
        // t2.execute(graph, null);

        removeGraph();
    }

    //private static final String stringParam = "a\\b\'";
    //private static final String stringParam = "a\nb\"\'";
    static final String stringParam = "a\nb\"\'";

    private class CheckParamAction implements Action {

        private String _param;

        public CheckParamAction(String param) {
            Assert.assertEquals(stringParam, param);
            this._param = param;
        }

        @Override
        public void eval(TaskContext ctx) {

        }

        @Override
        public void serialize(Buffer builder) {
            builder.writeString("checkParam");
            builder.writeChar(Constants.TASK_PARAM_OPEN);
            TaskHelper.serializeString(this._param, builder, true);
            builder.writeChar(Constants.TASK_PARAM_CLOSE);
        }

        @Override
        public String name() {
            return "checkParam";
        }
    }

    @Test
    public void testTAskParamSerialization() {
        initGraph();
        graph.actionRegistry().getOrCreateDeclaration("checkParam").setParams(Type.STRING).setFactory(new ActionFactory() {
            @Override
            public Action create(Object[] params) {
                return new CheckParamAction((String) params[0]);
            }
        });

        Task t = newTask()
                .action("checkParam", stringParam);
        Buffer buffer = graph.newBuffer();
        t.saveToBuffer(buffer);

        Task loaded = newTask();
        loaded.loadFromBuffer(buffer, graph);

        removeGraph();
    }


}
