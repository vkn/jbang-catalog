///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+
//DEPS org.springframework.boot:spring-boot-starter-webflux:3.0.5
package foo;//do not remove, required by spring

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static foo.spring_ws_server.print;
import static java.lang.System.out;
import static java.time.ZoneOffset.UTC;

@SpringBootApplication
@RestController
public class spring_ws_server implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(foo.spring_ws_server.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Ready >>>>");
    }


    @GetMapping("/time")
    public Mono<String> getTime() {
        return Mono.just("UTC local: " + LocalDateTime.now(UTC) + " instant: " + Instant.now(Clock.systemUTC()));
    }


    static void print(String s) {
        out.println(s);
    }
}

@Component
class AppWsStreamHandler implements WebSocketHandler {

    Flux<String> stream = Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
            .map(l -> "ping " + Instant.now());

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        print("Starting websocket session: %s".formatted(session.getId()));
        Flux<WebSocketMessage> received = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .mapNotNull(m -> {
                    print("Client says: %s".formatted(m));
                    return "Hello from server!";
                })
                .map(session::textMessage);
        var responseStream = stream.map(session::textMessage);
        return session.send(received.mergeWith(responseStream));
    }
}

@Configuration
class WebFluxConfig implements WebFluxConfigurer {

    @Bean
    HandlerMapping handlerMapping(AppWsStreamHandler appWsStreamHandler) {
        return new SimpleUrlHandlerMapping(Map.of("/stream", appWsStreamHandler), 1);
    }
}
