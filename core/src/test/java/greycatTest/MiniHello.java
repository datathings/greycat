package greycatTest;

import greycat.Graph;
import greycat.Node;
import greycat.core.CoreGraph;

public class MiniHello {

    public static void main(String[] args) {
        Graph g = new CoreGraph();
        long before = System.currentTimeMillis();
        int nb = 10000000;
        for (int i = 0; i < nb; i++) {
            //Object o = new Object();
            Node n = g.newNode(0);
            g.freeNode(n);
        }
        long after = System.currentTimeMillis();
        double seconds = ((double) after - before) / 1000d;
        System.out.println(seconds);
        System.out.println(nb / seconds);
    }
}
