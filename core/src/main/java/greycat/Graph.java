package greycat;

public interface Graph {

    Node newNode(long world, long time);

    void free(Node n);

    Node alias(Node n);

}
