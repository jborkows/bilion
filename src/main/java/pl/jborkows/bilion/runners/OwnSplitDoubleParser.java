package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OwnSplitDoubleParser implements Runner {
    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }

    private final Map<String, Station> stations = new HashMap<>();

    @Override
    public void process(Path path) throws Exception {
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.filter(line -> !line.startsWith("#")).forEach(this::processLine);
        }
        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", k, (1.0 * v.all / v.count) / 1000, v.max / 1000, v.min / 1000);
        });
    }

    private void processLine(String line) {
        var stationName = "";
        var index = 0;
        var numberBegin = 0;
        var decimalBegin = 0;

        char[] charArray = line.toCharArray();
        for (char c : charArray) {
            if (c == ';') {
                stationName = line.substring(0, index);
                numberBegin = index + 1;
            }
            if (c == '.' && numberBegin > 0 && index > numberBegin) {
                    decimalBegin = index + 1;
                break;
            }
            index++;
        }
        if (index == line.length()) {
            return;
        }
        var number = line.substring(numberBegin, decimalBegin - 1);
        var decimal = line.substring(decimalBegin);

//        var normalizedDecimal = switch (decimal.length()){
//            case 4 -> decimal;
//            case 3 -> decimal+"0";
//            case 2 -> decimal+"00";
//            case 1 -> decimal+"000";
//            default -> throw new IllegalStateException("Unexpected value (more precision: " + line);
//        };
//        var value = Long.parseLong(number) * 10000 + Long.parseLong(normalizedDecimal);
        var normalizator = switch (decimal.length()){
            case 4 -> 1;
            case 3 -> 10;
            case 2 -> 100;
            case 1 -> 1000;
            default -> throw new IllegalStateException("Unexpected value (more precision: " + line);
        };

        var value = Long.parseLong(number) * 10000 + Long.parseLong(decimal)*normalizator;
        var station = stations.computeIfAbsent(stationName, (_a) -> new Station());
        station.all += value;
        station.count += 1;
        station.max = Math.max(station.max, value);
        station.min = Math.min(station.min, value);
    }

}
