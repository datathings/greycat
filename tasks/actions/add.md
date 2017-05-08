# Task Action: Add

The add action allows to add a unidirectional relation from node(s) in the result to node(s) stored in a variable.

This action should only be used after a task returning node(s) as result.

> The use of *add* doesn't impact the current result.

Therefore, we could modify the running example to add a relation from sensors nodes to rooms nodes:

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
		.add("room","parentRoom") //Change here !!!
	)
).execute(g, null);
```

The opposite operation (from variable node(s) to result node(s)) can be done using the [addTo](addTo.md) task.



> Similar to [setProperty](setProperty.md), when several nodes are present in the result, the add task is applied to all of them.

> If the relation wasn't existing before, a new one is created, otherwise node(s) are just added to the relation.