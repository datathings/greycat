# Task Action: LoopPar

The loopPar action is similar than [loop](loop.md) action with the ability to leverage multi-core infrastructure.
Therefore the subTask of the loopPar action will be executed in CONCURRENCE.

> WARNING: to avoid variable conflict during parallel execution, we encourage users to use defineVar methods to ensure variable scopes.



