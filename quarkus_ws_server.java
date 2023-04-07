///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 20+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:2.16.6.Final}@pom
//DEPS io.quarkus:quarkus-websockets
//DEPS io.quarkus:quarkus-scheduler
//DEPS io.quarkus:quarkus-picocli
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=INFO
//Q:CONFIG quarkus.http.port=8181

import com.oracle.svm.core.annotate.Inject;
import io.quarkus.scheduler.Scheduled;
import picocli.CommandLine;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;


@CommandLine.Command(showDefaultValues = true)
public class quarkus_ws_server implements Callable<Integer> {

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Option(names = {"-p", "--port"}, description = "port", defaultValue = "8080")
    Integer port;

    @javax.inject.Inject
    Server server;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new quarkus_ws_server()).execute(args);
        System.exit(exitCode);

    }

    @Override
    public Integer call() throws Exception {
        System.setProperty("quarkus.http.port", String.valueOf(port));
        io.quarkus.runtime.Quarkus.run();
        return 0;
    }

}

@SuppressWarnings("resource")
@ServerEndpoint("/chat/{username}")
@ApplicationScoped
class Server {

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        System.out.println("Server: opened session for " + username);
        sessions.put(username, session);
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        System.out.println("Server: closing session for " + username);
        sessions.remove(username);
        broadcast("User " + username + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        System.out.println("Server: error closing session for " + username + " " + throwable);
        throwable.printStackTrace();
        sessions.remove(username);
        broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("username") String username) {
        System.out.println("Server: received message from " + username + ": " + message);
        if (message.equalsIgnoreCase("_ready_")) {
            broadcast("User " + username + " joined");
        } else {
            broadcast(">> " + username + ": " + message);
        }
    }

    @Scheduled(every="3s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void ping() {
        broadcast("ping");
    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }
}
