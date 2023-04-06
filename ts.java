///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17+
//DEPS info.picocli:picocli:4.7.1
import picocli.CommandLine;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.*;
import static picocli.CommandLine.*;

/**
 * Converts first arg millis to date-time or first arg local date to millis
 * or -c for clipboard or -n for now
 * If no args given system in is taken so that input can be piped in
 * e.g.
 *  ts 686095200000
 *  ts 686095200000 586095200000
 *  ts 2022-12-04
 *  ts 2022-12-04 2023-12-04
 *  ts -c or ts --clipboard
 *  ts -n or ts --now
 *  echo 686095200000 | ts
 *  echo -e "-1 \n 0 \n 1" | ts
 */
@Command(name = "ts", mixinStandardHelpOptions = true, version = "ts 0.1", showDefaultValues = true,
        description = "Convert millis to datetime instant string")
public class ts implements Callable<Integer> {
    private static final Pattern TS = Pattern.compile("(-?[0-9]+)");
    private static final Pattern DATE = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})");
    private static final String usage = """
              Converts first arg millis to date-time or first arg local date to millis
              or -c for clipboard or -n for now
              If no args given system in is taken so that input can be piped in
              e.g.
               ts 686095200000
               ts 686095200000 586095200000
               ts 2022-12-04
               ts 2022-12-04 2023-12-04
               ts -c or ts --clipboard
               ts -n or ts --now
               echo 686095200000 | ts
               echo -e "-1 \\n 0 \\n 1" | ts
            """;


    @Option(names = { "-h", "--help", "-?", "-help"}, usageHelp = true,
            description = usage)
    private boolean help;

    @CommandLine.Option(
            names = {"-c", "--clipboard"},
            description = "Captures the input from clipboard",
            required = false)
    private boolean isClipBoard;

    @CommandLine.Option(
            names = {"-n", "--now"},
            description = "Prints now",
            required = false)
    private boolean isNow;

    @Parameters(
            index = "0",
            description = "0..n millis or dates input to convert. stdin is parsed if omitted",
            arity = "0..*"
    )
    private String[] input;


    @Override
    public Integer call() throws Exception {

        if (isClipBoard) {
            processClipboard();
        } else if (isNow) {
            var now = Instant.now();
            out.println(now + " " + now.toEpochMilli());
        } else if (input != null) {
            Arrays.stream(input).forEach(this::processLine);
        } else {
            processStdIn();
        }
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new ts()).execute(args);
        System.exit(exitCode);
    }


    static Instant getInstantFromTs(String tsString) {
        return Instant.ofEpochMilli(Long.parseLong(tsString));
    }

    static long getMillis(String dateString) {
        return LocalDate.parse(dateString).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }

    static String clipboard() {
        try {
            return (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException("cannot get content of clipboard", e);
        }
    }

    private void processStdIn() throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            if (!in.ready()) {
                return;
            }
            String line;
            while ((line = in.readLine()) != null) {
                processLine(line);
            }
        }
        catch (IOException e) {
            System.err.println("IOException reading System.in " + e);
            throw e;
        }
    }

    private void processClipboard() {
        var clipboard = clipboard();
        if (clipboard.length() > 1024 * 1024 * 1024) {
            err.println("Clipboard content too big");
        }
        processLine(clipboard);
    }


    private void processLine(String line) {
        Matcher tsMatcher = TS.matcher(line);
        Matcher dateMatcher = DATE.matcher(line);
        var tsFound = tsMatcher.find();
        var dtFound = dateMatcher.find();
        if (!tsFound && !dtFound) {
            out.println(line);
        }
        else if(dtFound){
            var millis = getMillis(dateMatcher.group(1));
            out.println(millis);
        }
        else {
            var instant = getInstantFromTs(tsMatcher.group(1));
            out.println(instant == null ? "not parsed" : instant.toString());
        }
    }
}
