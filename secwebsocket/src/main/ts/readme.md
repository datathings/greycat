# GreyCat WebSocket plugin

Allow remote Read/Write of GreyCat graph.

# Usage

```js
var greycat = require('greycat');
var greycatWS = require('greycat-websocket');
var graph = greycat.GraphBuilder.newBuilder()
    .withStorage(greycatWS.WSClient("ws://127.0.0.1:4000"))
    .build();
graph.connect(function(){
    var node = graph.newNode(0,0);
    node.set("name", greycat.Type.STRING, "myFirstNode");
    graph.save(null);
});
```
more to come...