# Task execution methods

A task is a prototype of execution that can be defined statically and independently to any graph instances.
A task is executed ON a graph.
The simplest API to do so is the following:

``` java
task.execute(graph, null);
```

If the task aims at returning a result, then the last task context result will be returned in a result callback passed as parameter.
This callback should be of the type of Callback<TaskResult>.

Such as:

``` java
task.execute(graph, (result)-> {
	//DO something
	if(result != null){
		//you are responsible to free the result set!
		result.free();
	}
});
```

> WARNING: if the callback is not null, you are responsible to free the result, cache full error will occurs otherwise.
