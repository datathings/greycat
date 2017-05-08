# Task hook and Debug modes

The task execution engine offers various hooks to intercept the execution flow for debugging or printing purposes. Task hooks can be injected either locally (for a particular task) or globally for the graph (for debugging purposes).

A sample plugin is present in the Core as an example of a global task Hook.
Therefore it is possible to execute all tasks in verbose mode using the following Graph Builder configuration.

``` java
Graph g = new GraphBuilder()
	.withMemorySize(10000)
 	.withPlugin(new org.mwg.utility.VerbosePlugin())
	.build();
```
