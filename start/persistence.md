# Persistence
[Code sample here](https://github.com/datathings/greycat-samples/blob/master/src/main/java/greycat/samples/Persistance.java)

Greycat comes with a built-in persistence mechanism. Nodes and relationships are automatically serialized and unserialized by  and can be stored in different key/value stores. The following example shows how  is configured to use Google's LevelDB as a storage backend.  

```java
Graph g = new GraphBuilder()
				.withMemorySize(10000) //cache size before sync to disk
				.withStorage(new LevelDBStorage("greycat_db")) //location to store on disc
				.build();

g.connect(isConnected -> {

		Node sensor = g.newNode(0, 0); //create new node for world 0 and time 0
		sensor.set("id", Type.INT, Math.abs(rand.nextInt()));
		sensor.set("value", Type.INT, rand.nextInt());

		g.index(0, 0, "sensors", nodeIndex -> {
				nodeIndex.addToIndex(sensor);
				//save the database here.
				g.save(saveResult -> {
						g.index(0, System.currentTimeMillis(), "sensors", nodeIndex2 -> {
								nodeIndex2.find(new Callback<Node[]>() {
										@Override
										public void on(Node[] result) {
											  //Get all the nodes stored on disk, each execution will increase the list
												for (int i = 0; i < result.length; i++) {
														System.out.println("\t" + result[i].toString());
												}
										}
								});

						});
				});
		});
});

```
