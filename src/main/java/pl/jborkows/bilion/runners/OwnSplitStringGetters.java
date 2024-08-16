package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OwnSplitStringGetters implements Runner{
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
            stream.filter(line -> !line.startsWith("#")).forEach(this::processLine);
        }
        stations.forEach((k, v) -> {
           System.out.printf("%s;%s;%s;%s;\n",k, v.all/v.count, v.max, v.min);
        });
    }

    private void processLine(String line) {
        var stationName="";
        var number="";
        var index = 0;
        for(index=0; index<line.length(); index++) {
            if(line.charAt(index)==';') {
                stationName=line.substring(0,index);
                number=line.substring(index+1);
                break;
            }
        }
        if(index == line.length()){
            return;
        }
        var value = Double.parseDouble(number);
        var station = stations.computeIfAbsent(stationName, (_a)->new Station());
        station.all += value;
        station.count += 1;
        station.max = Math.max(station.max, value);
        station.min = Math.min(station.max, value);
    }

}
