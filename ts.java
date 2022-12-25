///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 17+
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.*;

/**
 * convert first arg millis to date-time
 * or first arg local date to millis
 * or if first args is 'clipboard' or 'c', tries to convert from clipboard
 * If no args given system in is taken so that input can be piped in
 * e.g.
 *  ts 686095200000
 *  ts 2022-12-04
 *  ts clipboard
 *  ts c
 *  echo 686095200000 | ts
 *
 */
public class ts {
    private static final Pattern TS = Pattern.compile("(-?[0-9]+)");
    private static final Pattern DATE = Pattern.compile("([0-9]{4}-[0-9]{2}-[0-9]{2})");

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

    public static void main(String... args) throws IOException {
        if (args.length > 0) {
            if (args.length == 1 && "c".equals(args[0]) || "clipboard".equals(args[0])) {
                processClipboard();
                return;
            }
            for (String arg : args) {
                processLine(arg);
            }
            return;
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
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

    private static void processClipboard() {
        var clipboard = clipboard();
        if (clipboard.length() > 1024 * 1024 * 1024) {
            err.println("Clipboard content too big");
        }
        processLine(clipboard);
    }


    private static void processLine(String line) {
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
