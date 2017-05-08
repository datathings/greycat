# Task Action: As Global Var

The asGlobalVar action allows to store the current result in a global variable. If a variable of this name was already existing, then the variable is overwritten.

This action can be used after any task, and will work on any current result.

> The use of *asGlobalVar* doesn't impact the current result.

> The defined variable will be accessible made accessible to all tasks.

The following expression applied after the running example :

```java
inject(0)
.asGlobalVar("counter")
.fromIndexAll("rooms")
.foreach(
    traverse("sensors")
    .foreach(
        math("counter+1")
        .asGlobalVar("counter")
    )
)
.print("{{counter}}")
.execute(g, null);
```

will print the following result:

```
6.0
```

Related action are [asVar](asVar.md) that will store the current result in the upper possible context containing a variable with this name, and [defineVar](defineVar.md) that will add the variable to the current context only without modifying the father context .
