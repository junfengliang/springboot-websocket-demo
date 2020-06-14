# springboot-websocket-demo

This article show you a simple way to implement a websocket demo via springboot.

## Step 1: Dependency import
Add spring-boot-starter-websocket dependency in pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
## Step 2: Implement the websocket controller

```java
@Component
@ServerEndpoint(value = "/ws")
public class WebSocketController {
    static Map<String,Session> activeMap = new ConcurrentHashMap<>();

    static Logger log = Logger.getLogger("WebSocketController");
    /**
     * when websocket connected.
     * @param session
     */
    @OnOpen
    public void onOpen(Session session) {
        Map<String, List<String>> map = session.getRequestParameterMap();
        List<String> list = map.get("username");
        if(list==null || list.size()==0){
            sendMessage(session,"no username! authentication failed.");
            close(session);
            return;
        }
        String username = list.get(0);
        String message = "[" + username + "] join in ！";
        session.getUserProperties().put("username",username);
        addSession(session);
        sendMessageForAll(message);
    }

    /**
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        log.info("onClose, " + session.getUserProperties().get("username"));
        String message = "Close...";
        close(session);
    }

    /**
     * @param session
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        log.info("onMessage, " + message);
        String username =  (String)session.getUserProperties().get("username");
        String msg = username + " said: " + message;
        sendMessageForAll(msg);
    }

    /**
     * @param session
     * @param throwable
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        close(session);
        throwable.printStackTrace();
    }

    public void addSession(Session session) {
        activeMap.put(session.getId(),session);
    }

    public void sendMessageForAll(String message) {
        activeMap.forEach((sessionId, session) -> sendMessage(session, message));
    }

    private void sendMessage(Session session, String message) {
        if(session==null){
            return;
        }
        try {
            RemoteEndpoint.Async async = session.getAsyncRemote();
            async.sendText(message);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void close(Session session) {
        try {
            activeMap.remove(session.getId());
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## Step 3: add application class to make it runnable
```java
@SpringBootApplication
public class DemoApplication {
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
	@Bean
	public ServerEndpointExporter serverEndpointExporter() {
		return new ServerEndpointExporter();
	}
}
```
## Step 4: implement the html5 websocket client
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>HTML5 WebSocket Demo</title>
    <script type="text/javascript">
    var webSocket;
    function connect() {
        document.getElementById('messages').innerHTML ='';
        url = document.getElementById('url').value
        webSocket = new WebSocket(url);
        webSocket.onerror = function(event) {
            alert(event.data);
        };
        webSocket.onopen = function(event) {
            document.getElementById('messages').innerHTML = 'Connected';
        };
        webSocket.onmessage = function(event) {
            document.getElementById('messages').innerHTML += '<br />Receive:'+ event.data;
        };

    }
    function send() {
        var msg = document.getElementById('msg').value;
        document.getElementById('messages').innerHTML += '<br />Send:'+ msg;
        webSocket.send(msg);
    }
    console.log('yyyy')
</script>
    <style>
button{
    width: 100px;
    height: 30px;
}
input{
    width: 300px;
    height:20px;
}
div{
    margin-top: 10px;
}
</style>
</head>
<body>
<div>
    <input id="url" value="ws://localhost:8080/ws?username=junfeng" />
    <button onclick="connect()">  Connect  </button>
</div>
<div>
    <input id="msg" />
    <button onclick="send()">  Send  </button>
</div>
<div id="messages"></div>
</body>
</html>
```

## Tips
- you can get the demo code in github. https://github.com/junfengliang/springboot-websocket-demo
- @Autowired is not supported in websocket for @ServerEndpoint. If you use @Autowired in WebSocketController, NPE will raise. If you need to use beans in spring, you need to get them via application context.
