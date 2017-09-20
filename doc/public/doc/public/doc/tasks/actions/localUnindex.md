## Task Action: LocalUnindex

The `localUnindex` action allows to remove node(s) in an existing indexed relation of node(s) in the result.

This action should only be used after a task returning node(s) as result.

> The use of *localUnindex* doesn't impact the current result.

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
		.localIndex("idxParentRoom","room","parentRoom") //Change here !!!
	)
).execute(g, null);

[...]

fromIndexAll("rooms")
.localUnindex("idxParentRoom","room","parentRoom")
.execute(g, null);
```


The opposite operation is to create or modify a local index with [localIndex](localIndex.md) task.

> Similar to [remove](remove.md), but with an indexing concept.