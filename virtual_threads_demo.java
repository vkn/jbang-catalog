///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 19+

//COMPILE_OPTIONS --enable-preview -source 19
//RUNTIME_OPTIONS --enable-preview

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.lang.System.out;

/**
 * virtual threads demo
 * download websites titles in parallel
 * controlled by concurrency arg
 * e.g.
 * vt 1 //download one by one
 * vt 3 // download with 3 virtual threads
 * vt   //download all in parallel
 */
public class virtual_threads_demo {
    private static final Pattern TITLE = Pattern.compile("<title[^>]*?>(.*?)</title>");

    public static void main(String... args) throws Exception {
        var urls = List.of(
                "http://stackoverflow.com/",
                "http://www.google.com",
                "http://www.cnn.com",
                "http://www.lufthansa.com",
                "http://www.boeing.com",
                "http://www.airbus.com",
                "http://www.apple.com",
                "http://www.coca-cola.com",
                "http://www.red-bull.com",
                "http://www.foo-bar.com",
                "https://weather.com/",
                "https://www.redhat.com/",
                "http://www.github.com",
                "http://www.bbc.com",
                "http://www.quarkus.io",
                "http://www.oracle.com",
                "http://www.amazon.com",
                "https://www.etsy.com",
                "http://www.microsoft.com",
                "https://news.ycombinator.com/",
                "https://www.docker.com/",
                "https://el.wikipedia.org/wiki/%CE%A0%CF%8D%CE%BB%CE%B7:%CE%9A%CF%8D%CF%81%CE%B9%CE%B1",
                "https://ubuntu.com/",
                "http://www.yahoo.com"
        );
//        var fileWriter = new FileWriter("results.txt");
        var consoleWriter = new ConsoleWriter();
        var e = new Export(consoleWriter, urls.size());
        var s = Instant.now();

        int concurrency = args.length > 0 ? Integer.parseInt(args[0]) : urls.size();
        var downloader = new WebSiteDownloader(concurrency);
        for (String url : urls) {
            e.submit(() -> Thread.currentThread() + " " + url + " " + downloader.getTitleAndLengths(url));
        }

        out.printf("All %s submitted....%n", urls.size());
        e.isDone().get(1, TimeUnit.DAYS);
        out.printf("DONE in %s%n", Duration.between(s, Instant.now()).toMillis());
    }


    static class WebSiteDownloader {
        private final HttpClient httpClient;
        private final Semaphore semaphore;

        public WebSiteDownloader(int concurrency) {
            semaphore = new Semaphore(concurrency);
            httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }

        private String getTitleAndLengths(String url) {
            var content = getWebsiteContent(url);
            try {
                var matcher = TITLE.matcher(content);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return "no title matched";
            }
            catch (Exception e) {
                throw new RuntimeException(url + " could not be parsed for content " + content + "\n url " + url);
            }
        }

        private String getWebsiteContent(String url) {
            try {
                semaphore.acquire();
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                        .body();

            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            finally {
                semaphore.release();
            }

        }
    }

    interface Writer {
        default void start() {
        }

        void writeLine(String header);
    }

    static class Export {

        private final int numOfTasks;
        private final ExecutorService pool;
        private final ExecutorCompletionService<String> completionService;
        private final Writer writer;
        private String header;
        private final Future<Boolean> fileCompleted;

        Export(Writer writer, int numOfTasks) {
            this.writer = writer;
            this.numOfTasks = numOfTasks;
            pool = Executors.newVirtualThreadPerTaskExecutor();
            completionService = new ExecutorCompletionService<>(pool);
            fileCompleted = pool.submit(this::writeResults);
        }

        void submit(Callable<String> task) {
            completionService.submit(task);
        }

        Future<Boolean> isDone() {
            return fileCompleted;
        }

        private boolean writeResults() {
            try {
                writer.start();
                if (header != null) {
                    writer.writeLine(header + "\n");
                }
                int completed = 0;
                while (completed++ < numOfTasks) {
                    writer.writeLine(completionService.take().get() + "\n");
                }
            }
            catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            finally {
                pool.shutdownNow();
            }
            return true;
        }

        public Export withHeader(String header) {
            this.header = header;
            return this;
        }
    }


    static class FileWriter implements Writer {

        private final String fileName;
        private BufferedWriter writer;

        FileWriter(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void start() {
            try {
                writer = Files.newBufferedWriter(Paths.get(fileName));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeLine(String content) {
            try {
                writer.write(content + "\n");
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ConsoleWriter implements Writer {

        @Override
        public void writeLine(String content) {
            out.print(content);
        }

    }

}
