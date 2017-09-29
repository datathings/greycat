package greycat.raft;

import greycat.*;
import greycat.plugin.Resolver;
import greycat.scheduler.TrampolineScheduler;
import greycat.websocket.WSClient;

import static greycat.Config.expSize;
import static greycat.Config.saveEvery;
import static greycat.Tasks.newTask;

public class RaftClient {

    public static void main(String[] args) {

        RaftGraph g = new RaftGraph(new WSClient("ws://" + Config.master + ":8090/ws"), Config.cacheSize, Config.cacheSize, new TrampolineScheduler(), null, false);
        Resolver r = g.resolver();
        g.connect(new Callback<Boolean>() {
            @Override
            public void on(Boolean result) {
                System.out.println("Starting experiment");
                long begin = System.currentTimeMillis();
                newTask().loop("0", expSize, newTask().thenDo(new ActionFunction() {
                    @Override
                    public void eval(TaskContext ctx) {
                        Node n = ctx.graph().newNode(0, 0);
                        r.externalLock(n);
                        n.set("name", Type.STRING, "node_" + ctx.template("{{i}}"));
                        r.externalUnlock(n);
                        n.free();
                        final int i = ctx.intVar("i");
                        if ((i % saveEvery) == 0) {
                            ctx.graph().save(result1 -> {
                                System.out.println(i + "=" + (System.currentTimeMillis() - begin));
                                ctx.continueTask();
                            });
                        } else {
                            ctx.continueTask();
                        }
                    }
                })).save().execute(g, result1 -> {
                    long after = System.currentTimeMillis();
                    System.out.println((after - begin) + "ms");
                    g.disconnect(null);
                });
            }
        });
    }

}
