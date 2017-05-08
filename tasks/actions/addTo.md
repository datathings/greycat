# Task Action: Add To

The addTo action allows to add a unidirectional relation from node(s) stored in variable to the result node(s).

This action should only be used after a task returning node(s) as result.

> The use of *addTo* doesn't impact the current result.

The addTo action is used in the running example to add a relation from rooms to the newly created sensors.

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
	)
).execute(g, null);
```

In this example, "sensors" is the name of the relation, and "parentRoom" the variable in which node(s) from which the relation will start are stored.

The opposite operation (from result node(s) to variable node(s)) can be done using the [add](add.md) task.

> Similar to [setProperty](setProperty.md), when several nodes are present in the result, the add task is applied to all of them.

> If the relation wasn't existing before, a new one is created, otherwise node(s) are just added to the relation.