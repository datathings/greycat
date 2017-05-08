# Indexing
[Code sample here](https://github.com/datathings/greycat-samples/blob/master/src/main/java/greycat/samples/Index.java)

Indexing data enables fast and efficient retrieval. Indexing in GreyCat is very straightforward. Basically, a NodeIndex is a graph node, just like all other nodes but with additional methods to add, remove and find indexed nodes.
This also implies that a NodeIndex is also temporal, i.e., what is indexed by a node depends on the time the index was created (and also the world in which the index was created).

```java
Node sensor0 = g.newNode(0, 0);
sensor0.set("sensorId", Type.STRING, "4494F");
sensor0.set("name", Type.STRING, "sensor0");
sensor0.set("value", Type.DOUBLE, 26.2); //set the value of the sensor

Node kitchenNode = g.newNode(0, 0);
kitchenNode.set("name", Type.STRING, "kitchen");
kitchenNode.addToRelation("sensors", sensor0);

Node LivingNode = g.newNode(0, 0);
LivingNode.set("name", Type.STRING, "living");

//Get from the graph an index called roomIndex
g.index(0, 0, "rooms", roomIndex -> {
      //Add a room to the index of rooms to be able to retrieve it fast by its name
      roomIndex.addToIndex(kitchenNode, "name");
      roomIndex.addToIndex(LivingNode, "name");                
      //your next code goes here...
});
```

Now in order to retrieve all the nodes that are indexed as rooms, we do the following:

```java
roomIndex.find(rooms -> {
  for (Node room : rooms) {
    System.out.println(room);
  }
});

```

In order to retrieve a node with a certain name, we just need to append the attribute type and value to the find method.
```java
roomIndex.find(rooms -> {
    System.out.println("found: "+rooms.length+" node!");
    for (Node room : rooms) {
        System.out.println(room);
    }
},"name","kitchen"); //here we ask to filter only the node with the name = kitchen.
```
