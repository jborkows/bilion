package pl.jborkows.bilion;

import pl.jborkows.bilion.runners.*;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;


public class Main {
    public static void main(String[] args) throws Exception {
        var path = extractPath(args);
        var prefix = (args.length > 1) ? args[1] : "JVM";
        List<Runner> runners = List.of(
                new Simple(), //-->base
                new OwnSplit(),
                new OwnSplitStringGetters(),
                new OwnSplitDoubleParser(),
                new OwnSplitDoubleActiveParser(),
                new OwnSplitDoubleActiveParserStaticWorkingArray(),
                new OwnSplitDoubleActiveParserIndexBased(),
                new OwnSplitDoubleActiveParserIndexBasedHashFun(),
                new OwnSplitDoubleActiveParserIndexBasedLimitedHashFun(),
                new OwnSplitDoubleActiveParserIndexBasedTwoThreads(),
                new OwnSplitDoubleActiveParserIndexBasedMultipleThreads(1),
                new OwnSplitDoubleActiveParserIndexBasedMultipleThreads(2),
                new OwnSplitDoubleActiveParserIndexBasedMultipleThreads(3),
                new ReadBytesSync(256),
                new ReadBytesSync2nd(),
                new ReadBytesSyncFirstToLines()
        );

        var mapping = new LinkedHashMap<String, Long>(runners.size());

        var temp = Files.createTempFile("test", "results");
        for (Runner runner : runners) {
            var miliseconds = meat(runner, path);
            String message = prefix + ": Meet " + miliseconds / 1000.0 + " seconds for " + runner.name();
            System.out.println(message);
            Files.writeString(temp, message);
            mapping.put(runner.name(), miliseconds);
        }
        System.out.println("##################");
        mapping.forEach((k, v) -> System.out.println(k + "-> " + v / 60 / 1000 + "m" + ((v / 1000) % 60) + "s " + v % 1000 + "ms"));
        System.out.println("###################");
        try (var stream = Files.lines(temp)) {
            stream.forEach(System.out::println);
        }

    }

    private static long meat(Runner runner, Path path) throws Exception {
        LocalDateTime now = LocalDateTime.now();
        runner.process(path);
        LocalDateTime end = LocalDateTime.now();
        var between = Duration.between(now, end);
        return between.toMillis();
    }

    private static Path extractPath(String[] args) throws URISyntaxException {
        if (args.length == 0) {
            try {
                return Paths.get(Thread.currentThread().
                        getContextClassLoader().getResource("big_data.txt").toURI());
            } catch (Exception e) {
                return Paths.get(Thread.currentThread().
                        getContextClassLoader().getResource("small_data.txt" /**"weather_stations.csv"*/).toURI());
            }
        } else {
            return Paths.get(args[0]);
        }
    }
}
