package greycatTest;

import greycat.Graph;
import greycat.Helper;
import greycat.Node;
import greycat.Type;
import greycat.core.CoreGraph;

public class MiniHello {

    public static void main(String[] args) {
        Graph g = new CoreGraph();
        long before = System.currentTimeMillis();
        int nb = 10000000;
        int nameHash = Helper.hash("name");
        for (int i = 0; i < nb; i++) {
            Node n = g.newNode(0, 0);
            n.setAt(nameHash, Type.STRING, "sensor_" + i);
            g.freeNode(n);
        }
        long after = System.currentTimeMillis();
        double seconds = ((double) after - before) / 1000d;
        System.out.println(seconds);
        System.out.println(nb / seconds);
    }
}
