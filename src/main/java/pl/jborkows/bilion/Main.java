package pl.jborkows.bilion;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException {
        Path path;
        if(args.length == 0) {
            path = Paths.get(Thread.currentThread().
                    getContextClassLoader().getResource("weather_stations.csv").toURI());
        }else{
            path = Paths.get(args[0]);
        }
        Files.lines(path).forEach(System.out::println);
    }
}
