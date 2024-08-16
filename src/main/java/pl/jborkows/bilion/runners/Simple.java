package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Simple implements Runner{
    private static class Station {
        double max = 0;
        double min = 0;
        double all = 0;
        int count = 0;
    }

    private final Map<String, Station> stations = new HashMap<>();

    @Override
    public void process(Path path) throws Exception {
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.filter(line -> !line.startsWith("#")).forEach(line -> {
                var parts = line.split(";");
                String stationName = parts[0].trim();
                var value = Double.parseDouble(parts[1].trim());
                var station = stations.computeIfAbsent(stationName, (_a)->new Station());
                station.all += value;
                station.count += 1;
                station.max = Math.max(station.max, value);
                station.min = Math.min(station.max, value);
            });
        }
        stations.forEach((k, v) -> {
           System.out.printf("%s;%s;%s;%s;\n",k, v.all/v.count, v.max, v.min);
        });
    }

}
