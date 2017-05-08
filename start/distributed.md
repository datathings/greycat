# Distribution
 allows to seamlessly distribute the graph across computational nodes. 
Therefore, let's first setup a server node, which exposes the many-world graph to clients via websocket. 

```java
Graph g = new GraphBuilder()
	.withMemorySize(10000) //cache size before sync to disk
	.withStorage(new LevelDBStorage("_db"))
	.build();
	
 g.connect(isConnected -> {
	new WSServer(g, 8050).start();
		System.out.println(" Server listener :8050");
	});
```

This code starts a websocket server on localhost port 8050. Next, we connect a client to the websocket server. 

```java
Graph g = new GraphBuilder()
	.withMemorySize(10000) //cache size before sync to disk
	.withStorage(new WSClient("ws://127.0.0.1:8050"))
	.build();

g.connect(isConnected -> {
	//create new node for world 0 and time 0
	Node sensor = g.newNode(0, System.currentTimeMillis()); 
	sensor.set("id", Math.abs(rand.nextInt()));
	sensor.set("value", rand.nextInt());
            
	g.index("sensors", sensor, "id", res -> {
		g.save(saveResult -> {
			g.findAll(0, System.currentTimeMillis(), "sensors", 
				allSensorsNow -> {
				System.out.println("All sensors indexed:");
				for (Node sensorNow : allSensorsNow) {
					System.out.println("\t" + sensorNow.toString());
				}
				g.disconnect(result -> System.out.println("GoodBye!"));
			});
		});
	});
});
```

This allows clients to use the graph hosted on the server. The above code creates a new sensor graph node, a new index, and saves these changes to the server. 