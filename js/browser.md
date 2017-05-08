# Browser usage

To use MWG in your browser, you simply have to add the needed ```.js``` files to your DOM, for example:

``` html
<script src="js/mwg.js"></script>
```

In addition, plugins can be loaded later, for example:

``` html
<script src="js/mwg.structure.js"></script>
<script src="js/mwg.ml.js"></script>
<script src="js/mwg.ws.js"></script>
```

## Usage with manual download

JS files can be downloaded via the maven central repository, e.g., ```https://repo.maven.apache.org/maven2/org/kevoree/mwg/core/7/core-7-js.zip```

The name 'core' and the version number '7' must be replaced by the desired plugin name and version number.  In every MWG zip bundle one can find the following files:

- XYZ.js: standard file usable for debug and development environments 
- XYZ.min.js: compressed file for production usage
- XYZ.ts: TypeScript source for MWG extension
- XYZ.d.ts TypeScript header file for simple inclusion in TypeScript development environment.

For lightweight development, we advise you to use ```.min.js``` files.

Then, the JS API is practically identical to the Java API and can be used like in the following code snipped:

```js
var mwg = new org.mwg.GraphBuilder()
	.withStorage(new org.mwg.plugin.WSClient("ws://localhost:8081"))
	.build();
```
The line ```.withStorage(new org.mwg.plugin.WSClient("ws://localhost:8081"))``` is optional but allows to use a remote graph (client/server mode). Please read the distributed graph section to get more details on this topic.

## Usage with Bower

Bower is a very handy tool to manage dependencies for font-end development.

For instance, it allows to describe *(in ```bower.json```)* dependencies for MWG related development, for example:

```json
{
  "name": "mwg-sample",
  "version": "0.0.1",
  "dependencies": {
    "mwg-core": "https://repo.maven.apache.org/maven2/org/kevoree/mwg/core/7/core-7-js.zip",
    "mwg-ws": "https://repo.maven.apache.org/maven2/org/kevoree/mwg/plugins/websocket/7/websocket-7-js.zip",
    "mwg-structure": "https://repo.maven.apache.org/maven2/org/kevoree/mwg/plugins/structure/7/structure-7-js.zip",
    "mwg-ml": "https://repo.maven.apache.org/maven2/org/kevoree/mwg/plugins/ml/7/ml-7-js.zip"
  }
}
```

Then a simple command: ```bower install``` will download and extract the necessary dependencies in a directory ```bower_components```.

Then, these dependencies can be used like in the following code snipped:

``` html
<html>
<head>
    <title>MWG Sample</title>
    <script type="text/javascript" src="bower_components/mwg-core/mwg.min.js"></script>
    <script type="text/javascript" src="bower_components/mwg-ws/mwg.ws.min.js"></script>
    <script type="text/javascript" src="bower_components/mwg-structure/mwg.structure.min.js"></script>
    <script type="text/javascript" src="bower_components/mwg-ml/mwg.ml.min.js"></script>
</head>
<body>
<script>
var mwg = new org.mwg.GraphBuilder()
    //.withStorage(new org.mwg.plugin.WSClient("ws://localhost:8081"))
    .build();
mwg.connect(function(){
  console.log("Ready to work...");
});
</script>
</body>
</html>
```

A sample project can be downloaded [here](bower_mwg_sample.zip).