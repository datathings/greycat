# Task Action: As Var

The asVar action allows to store the current result in a variable. 
If a variable of this name already exists in the parent context, it will be updated and thus any task that were using this variable will use the new value.
if the variable wasn't existing, the variable is created and only task from the same scope will be able to access the variable

This action can be used after any task, and will work on any current result.

> The use of *asVar* doesn't impact the current result.

> The variable will be accessible


The asVar action can be used to exit a while loop for example:


``` java
 inject(1)
 .asVar("it")
 .print("{{it}}")
 .whileDo( cond("it<4"), 
    math("it+1")
    .asVar("it")
    .print("{{it}}"))
 .execute(g, null);
```

This code will produce the following output:

```
1
2.0
3.0
4.0
```

Related action are [asGlobalVar](asGlobalVar.md) that will store the current result in a global Variable and [defineVariable](defineVariable.md) that define a variable in the current scope and variable with the same name in parent context will not be impacted.
