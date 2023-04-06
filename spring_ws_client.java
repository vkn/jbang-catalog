///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS info.picocli:picocli:4.6.3
//DEPS org.springframework.boot:spring-boot-starter-webflux:3.0.0


import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Option;

@Command(name = "spring_ws_client", mixinStandardHelpOptions = true, version = "spring_ws_client 0.1",
        description = "ws_client made with jbang")
class spring_ws_client implements Callable<Integer> {

    @Parameters(index = "0", description = "username", defaultValue = "random")
    String name;

    @Option(names = {"-p", "--port"}, description = "port", defaultValue = "8080")
    Integer port;

    @Option(names = {"-l", "--location"}, description = "host location", defaultValue = "localhost")
    String host;

    public static void main(String... args) {
        int exitCode = new CommandLine(new spring_ws_client()).execute(args);
        new Scanner(System.in).nextLine(); // Don't close immediately.
        System.exit(exitCode);
    }

    /**
     * client executes given uri and has a callback to session
     * session callback first sends a hi message and then
     * listens to all incoming messages
     */
    @Override
    public Integer call() throws Exception { // your business logic goes here...
        client()
                .execute(uri(), session -> session
                    .send(Mono.just(send(session)))
                    .thenMany(session.receive().map(this::mapMsg).log())
                    .then()
                )//end execute
                .subscribe();
        return 0;
    }

    private static WebSocketMessage send(WebSocketSession session) {
        return session.textMessage("hi");
    }

    private String mapMsg(WebSocketMessage webSocketMessage) {
        return webSocketMessage.getPayloadAsText();
    }

    private static WebSocketClient client() {
        return new ReactorNettyWebSocketClient();
    }

    private URI uri() {
        var n = name.equals("random") ? UUID.randomUUID() : name;;
        return URI.create("ws://%s:%s/stream"
                .formatted(host, port));
    }
}
