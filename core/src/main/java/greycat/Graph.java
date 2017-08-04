package greycat;

public interface Graph {

    Node newNode(long world, long time);

    void freeNode(Node n);



}
