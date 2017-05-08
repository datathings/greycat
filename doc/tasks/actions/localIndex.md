## Task Action: LocalIndex

The `localIndex` action allows to create an/add to an existing indexed relation from node(s) in the result to node(s) stored in a variable.

This action should only be used after a task returning node(s) as result.

> The use of *localIndex* doesn't impact the current result.

Therefore, we could modify the running example to add an indexed relation from sensor nodes to room nodes:

``` java
loop("1","2",
	newNode()
	.setProperty("name", Type.STRING, "room_{{i}}")
	.indexNode("rooms", "name")
	.asVar("parentRoom")
	.loop("1","3",
		newNode()
		.setProperty("sensor", Type.STRING, "sensor_{{i}}")
		.addTo("sensors", "parentRoom")
		.localIndex("indexedRoom","room","parentRoom") //Change here !!!
	)
).execute(g, null);
```

To undo this operation, use [localUnindex](localUnindex.md) task.

> Similar to [add](add.md), but with an indexing concept.

> If the relation wasn't existing before, a new one is created, otherwise node(s) are just added to the relation.