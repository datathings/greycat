#Access Control Manager
The basic AccessControlManager provides base security mechanisms to filter node by node.

## Usage
ACM has to be started before anything else. When using ACM, the start process on server side must be the following:

```java
Storage rootAccessibleStorage = new Storage();
GraphBuilder builder = GraphBuilder.newBuilder().withStorage(rootAccessibleStorage);
Graph graphForACM = builder.build();
AccessControlManager acm = new BaseAccessControlManager(graphForACM);
acm.start(acmReady -> {
    
    //Once the ACM is ready, update the builder to filter the gets and puts on storage
    builder.withStorage(new BaseStorageAccessController(storage, acm));
    
    //Then start a secured WebSocket Server for instance
    SecWSServer wsServer = new SecWSServer(builder, 7071, acm);
    wsServer.start();
});
```
The ACM will first look for its own index, in which it stores ots configuration.
By default, this index is named `$acm` and is set in world `-1`.
You can change the name of this index by calling `setAcIndexName(String)` on the ACM, before calling `start()`.   
If the index does not exists, it will create and initialize some base access control elements listed hereunder.

## Default configuration

| Users index | Login attribute | Password attribute | Two-factor Authentication|
|---|---|---|:---:|
| `Users` | `login` | `pass` | DISABLED |

## Users list
It automatically adds an Administrator user.

| world | time | id | group | login | pass |   
|---:|---:|---:|---:|:---|---|    
| 0   | `now` | 3 | 3 | `admin` | `admin` |  

## Security Groups
It automatically creates several security groups.   

| GID | Name | Path |
|---:|:---|:---|
| 0 | Public | [0] |
| 1 | Admin Root | [1] |
| 2 | Users Admin | [1, 1] |
| 3 | Admin User Admin | [1, 1, 1] |
| 4 | Access Control Admin | [1, 2] |
| 5 | Security Groups Admin | [1, 2, 1] |
| 6 | Permissions Admin | [1, 2, 2] |
| 7 | Business Security Root | [1, 3] |

##  Permissions
And finally adds default permissions to Admin user.   

| UID | Roots | Read | Write | !Read | !Write |
|---:|:---|:---|:---|:---|:---|
| 3 | [] | [1] | [0, 1] | [] | [] |
