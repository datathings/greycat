#Email plugin


##Registration

```java
Graph graph = GraphBuilder.newBuilder()
    (....)
    .withPlugin(new EmailPlugin(<SMTP_SERVER>, <PORT>, <LOGIN>, <PASSWORD>, <NEED_AUTHENTICATION>,<USE_SARTTLS>))
```
##Usage
```java
newTask().action("sendEmail", <FROM>, <TO>, <SUBJECT>, <MESSAGE>);
```
