# Task Action: Traverse

The traverse action allows to traverse relations of one or more nodes. 

This action should only be used after a task returning node(s) as a result.

> traverse action will put all the nodes from the given relation of a node in the results

> If several nodes are in the result, the result is the aggregation of the traverse action results on each one of the nodes (in the order of the nodes). 
It should be noted that the information of which nodes are coming from which nodes relation is lost. 


> No deduplication is done, thus nodes present several times in a relation or in several node relations in the result will appear several times in the result.



Therefore, using the running example as a base, the execution of the following code:

``` java

fromIndexAll("rooms")
.traverse("sensors")
.get("sensor")
.print("{{result}}")
.execute(g, null);

```

will print the following:

```
[sensor_1,sensor_2,sensor_3,sensor_1,sensor_2,sensor_3]
```

Which corresponds to all the sensors names that were declared.


> See [get Action](get.md), to get information on the behavior of the get action.

