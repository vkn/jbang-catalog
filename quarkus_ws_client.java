///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 20+
// Update the Quarkus version to what you want here or run jbang with
// `-Dquarkus.version=<version>` to override it.
//DEPS io.quarkus:quarkus-bom:${quarkus.version:2.16.6.Final}@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-websockets-client
//Q:CONFIG quarkus.banner.enabled=false
//Q:CONFIG quarkus.log.level=WARN

import javax.inject.Inject;
import javax.websocket.*;

import io.quarkus.runtime.Quarkus;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static picocli.CommandLine.*;

@Command
public class quarkus_ws_client implements Runnable {

    private final Client client;
    private static final String UU = UUID.randomUUID().toString();

    @Parameters(index = "0", description = "username", defaultValue = "random")
    String name;


    @Option(names = {"-p", "--port"}, description = "port", defaultValue = "8080")
    Integer port;

    @Option(names = {"-l", "--location"}, description = "host location", defaultValue = "localhost")
    String host;

    @Inject
    IFactory factory;

    public quarkus_ws_client(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        URI uri = URI.create("ws://%s:%s/chat/%s".formatted(host, port, name.equals("random") ? UU : name));
        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(client, uri)) {
            System.out.println("Client sends hello world");
            session.getAsyncRemote().sendText("hello world");
            Quarkus.waitForExit();

        } catch (DeploymentException | IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}

@ClientEndpoint
class Client {

    @OnOpen
    public void open(Session session) {
        System.out.println("Client open");
        // Send a message to indicate that we are ready,
        // as the message handler may not be registered immediately after this callback.
        session.getAsyncRemote().sendText("_ready_");
    }

    @OnMessage
    void message(String msg) {
        System.out.println(">: " + msg);
    }

}
