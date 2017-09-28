package greycat;

import greycat.internal.BlackHoleStorage;
import greycat.internal.CoreGraph;
import greycat.plugin.Resolver;
import greycat.scheduler.TrampolineScheduler;
import io.atomix.AtomixReplica;
import io.atomix.catalyst.transport.Address;

import static greycat.Tasks.newTask;

public class Tester {

    public static void main(String[] args) {

        Graph g = new PaxosGraph(new BlackHoleStorage(), 1000000, 1000000, new TrampolineScheduler(), null, false);
        Resolver r = g.resolver();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {

                long begin = System.currentTimeMillis();

                newTask().loop("0", "10000", newTask().thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Node n = ctx.graph().newNode(0, 0);
                        r.externalLock(n);
                        n.set("name", Type.STRING, "node_"+ctx.template("{{i}}"));
                        r.externalUnlock(n);
                        ctx.continueTask();
                    }
                })).execute(g, result1 -> {
                    long after = System.currentTimeMillis();
                    System.out.println(after-begin);
                });
                System.out.println("Logged");
            }
        });


    }

}
