# A simple graph
[Code sample here](https://github.com/datathings/greycat-samples/blob/master/src/main/java/greycat/samples/Minimal.java)

The first step to use GreyCat is create and connect a simple graph using the following code

```java
//Create a minimal graph with the default configuration
Graph g = new GraphBuilder().build();

//Connect the graph
g.connect(isConnected -> {
		//Display that the graph database is connected!
		System.out.println("Connected : " + isConnected);
		//your next code goes here...
});
```

Next, create your first node in the graph and set some attributes:

```java
Node sensor0 = g.newNode(0, 0); //create a new node for world 0 and time 0
sensor0.set("snesorId", Type.INT, 12); //set the id attribute as an integer
sensor0.set("name",Type.STRING, "sensor0"); //set the name attribute as a string

//Display the first node we created
System.out.println(sensor0); //print {"world":0,"time":0,"id":1,"snesorId":12,"name":"sensor0"}
```

Then, let's create a second node and connect the two nodes with a relation

```java
Node room0 = g.newNode(0, 0); //create new node called room for world 0 and time 0
room0.set("name",Type.STRING, "room0"); //set the name attribute
room0.addToRelation("sensors", sensor0); //add the sensor0 to the relation sensors of room0

//Let's display the room0 node to see what's inside
System.out.println(room0); //print {"world":0,"time":0,"id":2,"name":"room0","sensors":[1]}
```

Finally, relations can be traversed as follows

```java
//iterate over the saved sensors relation from room0
room0.relation("sensors", (Node[] sensors) -> {
		System.out.println("Sensors:");
		for (Node sensor : sensors) {
				System.out.println("\t" + sensor.toString()); //print all the sensor in the room
		}
		//your next code goes here...
	});
```


Finally, the last step to do is to disconnect the graph database:
```java
//Disconnect the database
g.disconnect(result -> {
		System.out.println("GoodBye!");
});
```

Note that graph processing and traversing is fully asynchronous. So all operations are done inside of callbacks in order not to block the code execution.
