# Task Action: Set Property

The get action allows to set the value(s) of a node property. The property can be of any type, a relation, a map or a primitive Type.
See also [get](get.md) to discover how to get the value of a node property using Tasks.

This action should only be used after a task returning node(s) as result.

> The use of *setProperty* doesn't impact the current result, thus several setProperty can be chained.

setProperty requires 3 parameters:

* name of the property 
* type of the property
* value to set (as String) or name of the variable containing the value

## Property Type

Types can be found in org.mwg.Type,
5 different types can be used

* BOOL
* STRING
* LONG
* INT
* DOUBLE


Therefore, the following expression:

```java
newNode()
.setProperty("name",Type.String,"mynode")
.setProperty("level",Type.Int,"1")
.setProperty("restriction",Type.BOOL,"true")
.execute(g,null);
```

Will create a node with the following properties:

* name = mynode
* level = 1
* restriction = true


> Note that it is possible to apply some operation to variable directly within the third parameters:

```java
inject(1)
.asVar("myvar")
.newNode()
.setProperty("name", Type.INT, "{{= myvar + 1}}")
.get("name")
.print("{{result}}")
.execute(g,null);
                  
```

Will print the following result:

```
2
```

## Multiple Nodes set

When several nodes are present in the result, the set task is apply to all of them.
Thus, when applying the following task to the running example

```java
fromIndexAll("rooms")
.setProperty("locked",Type.BOOL,"true")
.execute(g,null)
```

a property locked will be added to all the rooms with an initial value of true.