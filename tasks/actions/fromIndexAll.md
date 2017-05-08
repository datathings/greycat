# Task Action: From Index All

The fromIndexAll action retrieves all nodes that were indexed globally, i.e., accessible as an entry point of the graph,
using a specified indexing name.

> See the [indexNode action](indexNode.md) to get information on how to index nodes in such a way.

> The result of the *from Index all* will be put in the result and made accessible to the next Task.

This action can be executed at any moment and doesn't require previous result.


Therefore, the following code run after the running example:

``` java
fromIndexAll("rooms")
    .get("name")
    .print("{{result}}")
.execute(g, null);
```

will return:

```
[room_1,room_2]
```

> See [fromIndex action](fromIndex.md), to get information on how to retrieve a specific globally indexed node.