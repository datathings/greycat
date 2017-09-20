# Task Action: From Index

The fromIndex action retrieves a globally indexed node, i.e., accessible as an entry point of the graph, given its indexing property and the name of the indexing relation.

> See the [indexNode action](indexNode.md) to get information on how to index nodes in such a way.

> The result of the *from Index* will be put in the result and made accessible to the next Task.

This action can be executed at any moment and doesn't require previous result.

The first argument of this action is the name of the indexing relation, and the second is the query on the indexing property to make.

> See the [query](../Query.md) to get information on how to build queries.

Therefore, the following code run after the running example:

``` java
fromIndex("rooms","name=room_1")
    .get("name")
    .print("{{result}}")
.execute(g, null);
```

will return:

```
room_1
```


> See [fromIndexAll action](fromIndexAll.md), to get information on how to retrieve all nodes indexed with a given relation name without filter.