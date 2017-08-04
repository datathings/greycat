package greycat;

public interface Graph {

    Node newNode(long time);

    void freeNode(Node n);
}
