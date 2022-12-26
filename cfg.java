///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import static java.lang.System.*;

/**
 * convert config property foo.bar to FOO_BAR env var string
 * and vice versa
 * e.g.
 *   cfg a.b
 *   cfg A_B
 *   echo a.b | cfg
 *   echo A_B_C | cfg
 *   echo -e "a.b \n c.d \n E_F" | cfg
 *
 *   NATIVE
 *   sdk install java 22.3.r19-grl
 *   sdk use java 22.3.r19-grl
 *
 *   either run (slow first time or after any change in the source file)
 *   jbang run --native cfg.java a.b
 *
 *   or install
 *   jbang app install --fresh --force --native --name=cfg cfg.java
 *   then run (slow first time or after any change in the source file)
 *   cfg a.b
 *
 */
public class cfg {

    public static void main(String... args) throws IOException {
        if (args.length == 1) {
            processLine(args[0]);
            return;
        }

        processStdIn(cfg::processLine);
    }

    private static void processStdIn(Consumer<String> consumer) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = in.readLine()) != null) {
                consumer.accept(line);
            }
        }
        catch (IOException e) {
            System.err.println("IOException reading System.in " + e);
            throw e;
        }
    }

    private static void processLine(String input) {
        if (input.contains("_")) {
            out.println(input.replace("_", ".").toLowerCase());
        }
        else {
            out.println(input.replace(".", "_").toUpperCase());
        }
    }
}
