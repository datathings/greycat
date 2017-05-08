# Task Action: Loop

The loop action allows to express the repetition of a sub Task passed as a parameter. Similarly to **for** loop construction in many imperative language, this action express the iteration over a range, through **INCLUSIVE** boundaries. 

> Additionally the loop action declares automatically a **i** variable containing the current loop index.

Therefore the following expression: 

```java
loop("0", "3",
	newNode()
	.setProperty("name", Type.STRING, "node_{{i}}")
	.print("{{result}}")
)
.execute(g,null);
```

Will print the following output in console:

```
{"world":0,"time":0,"id":1,"name":"node_0"}
{"world":0,"time":0,"id":2,"name":"node_1"}
{"world":0,"time":0,"id":3,"name":"node_2"}
{"world":0,"time":0,"id":4,"name":"node_3"}
```

In addition, loop action parameters are templates and can express math expression such as ```"{{=3-1}}"``` therefore, exclusive bounds can be build this way.

Finally, the loop action is transient and will not modify the current context. Therefore, the task context after the loop action remains unchanged. If a cumulative result has to be built, the best is to use a **addToVariable** action within the repeated sub task.


