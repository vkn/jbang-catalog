///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 20+
//DEPS org.openjdk.jmh:jmh-generator-annprocess:1.36
//DEPS tools.profiler:async-profiler:2.9
//JAVA_OPTIONS --add-opens java.base/java.lang=ALL-UNNAMED
//RUNTIME_OPTIONS --enable-preview -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints

// to run with javagagent add the following:
// `--javaagent=ap-loader@maxandersen=start,event=cpu,file=profile.html,flamegraph`
// OR
// `--javaagent=ap-loader@jvm-profiling-tools/ap-loader=start,event=cpu,file=profile.html,flamegraph`

//READ https://krzysztofslusarski.github.io/2022/12/12/async-manual.html

package foo;

import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Stream;

import static foo.asyn_prof_demo.Profiler.EventType.*;
import static foo.asyn_prof_demo.Profiler.Type.FLAME;
import static foo.asyn_prof_demo.Profiler.Type.JFR;
import static java.lang.System.out;

/**
 * Demo how to start async profiler programmatically
 * with jbang
 */
public class asyn_prof_demo {

    public static void main(String... args) {
        var test = new SquaredSum();

        long limit = 1000000;
        var ints = Stream.iterate(1, i -> i + 1).limit(limit).mapToInt(i -> i).toArray();
        var dbls = Stream.iterate(1, i -> i + 1).limit(limit).mapToDouble(i -> i).toArray();
        var bdcs = Stream.iterate(1, i -> i + 1).limit(limit).map(BigDecimal::valueOf).toArray(BigDecimal[]::new);

        var profiler = new Profiler(FLAME, CPU, "async_prof_result");
        profiler.start();

        out.println("integers    " + test.sumIntegers(ints));
        out.println("doubles     " + test.sumDoubles(dbls));
        out.println("bigdecimals " + test.sumBigDecimals(bdcs));
        profiler.stop();


    }

    static class Profiler {
        private String file;
        private String command = "";

        enum Type {JFR, FLAME}
        enum EventType {CPU("cpu"), WALL("wall"), ALLOC("alloc"), LOCK("lock"), CACHE_MISSES("cache-misses");

            private final String asString;

            EventType(String s) {
                this.asString = s;
            }

            @Override
            public String toString() {
                return asString;
            }
        }

        AsyncProfiler profiler = AsyncProfiler.getInstance();

        Profiler(Type type, EventType eventType, String filename) {
            command = type == JFR ? "jfr," : "";
            command += "event=%s".formatted(eventType);
            file = "_%s".formatted(eventType);
            file = type == JFR ? filename + file + ".jfr" : filename + file + ".html";

        }

        void start() {
            try {
                profiler.execute(String.format("start,%s,file=%s", command, file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void stop() {
            try {
                profiler.execute(String.format("stop,file=%s", file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * doesn't matter just spend some wall and cpu time
     */
    static class SquaredSum {
        int sumIntegers(int[] arr) {
            sleep(1);
            int sum = 0;
            for (int j : arr) {
                sum += j * j;
            }
            return sum;
        }

        private static void sleep(int i) {

            try {
                Thread.sleep(i * 1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        double sumDoubles(double[] arr) {
            sleep(2);
            double sum = 0;
            for (double v : arr) {
                sum += v * v;
            }
            return sum;
        }

        BigDecimal sumBigDecimals(BigDecimal[] arr) {
            sleep(5);
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal bigDecimal : arr) {
                sum = sum.add(bigDecimal.multiply(bigDecimal));
            }
            return sum;
        }
    }
}
