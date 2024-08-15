package pl.jborkows.bilion;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;


public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException {
        LocalDateTime now = LocalDateTime.now();
        Path path = extractPath(args);
        meat(path);

        LocalDateTime end = LocalDateTime.now();
        var between = Duration.between(now, end);
        System.out.println(between.getSeconds());
    }

    private static void meat(Path path) throws IOException {
        Files.lines(path).forEach(System.out::println);
    }

    private static Path extractPath(String[] args) throws URISyntaxException {
        Path path;
        if(args.length == 0) {
            path = Paths.get(Thread.currentThread().
                    getContextClassLoader().getResource("weather_stations.csv").toURI());
        }else{
            path = Paths.get(args[0]);
        }
        return path;
    }
}
