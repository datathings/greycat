#Secured web socket


##Server side
Register as

```java
SecWSServer wsServer = new SecWSServer(<Graph>, <TCP_PORT>, <USERS_GLOBAL_INDEX>, <LOGIN_FIELD_NAME>, <PASSWD_FIELD_NAME>);
```

The server automatically registers:
* **host:ip/ws** as the WebSocket connection address for the Graph
* **host:ip/greycat.auth** to authenticate users
* **host:ip/renewpasswd** to change passwords

**If using a proxy, make sure to proxy these paths before sending to your application.**

##Client side

###Authenticate   
Send login and password in a POST request, as form fields with ids: `login` and `pass`    
On success, th server will send you back in plain text `"<sessionUUID>#<user_node_id>"`   

###Connect the graph   
You can then connect the graph with the secured WebSocket client with    
```java
SecWSClient wsStorage = new SecWSClient("wss://<ip>:<port>/ws", "<sessionUUID>");
```   
This will call the url `wss://<ip>:<port>/ws?gc-greycat.auth-key=<sessionUUID>`    

###Password renewal    
From an authenticated user connection (admin or user himself), call a remote task:   
`newTask().action("resetPassword")`    
The task expects to have the user as initial result, and puts a `<UUID>` in the result. 
This `<UUID>` has to be sent in the final password change request to authenticate the request.
The `<UUID>` is only valid for a short period of time.   
To actually renew, send a POST request with form fields `pass` and `uuid` to `https://<ip>:<port>/renewpasswd`   
Possible answers, in plain text in the response are:   
- *200* 1:Password successfully changed.
- *401* -1:Password change token expired. Ask again for an email.
- *401* -2:Password token not found. Ask again for an email.
     
