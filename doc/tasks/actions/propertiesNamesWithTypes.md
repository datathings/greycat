# Task Action: propertiesNamesWithTypes
The `propertiesNamesWithTypes` action allows to get all the propertiesNamesWithTypes names of node(s) in the result, and filter the result with the given types.
This action should only be used after a task returning node(s) as result.

The next result will be an array of all the properties names.

The parameter is a flat string with the types values, separated by a comma. 
The available mwg types are present in `org.mwg.Type`.

If you want to get all the properties, please see [propertiesNames](propertiesNames.md)