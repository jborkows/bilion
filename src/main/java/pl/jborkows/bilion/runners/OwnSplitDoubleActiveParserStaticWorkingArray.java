package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OwnSplitDoubleActiveParserStaticWorkingArray implements Runner {
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

    long[] wholeNumber = new long[3];
    long[] partNumber = new long[4];


    private void processLine(String line) {
        var stationName = "";
        var index = 0;
        partNumber[0] = 0;
        partNumber[1] = 0;
        partNumber[2] = 0;
        partNumber[3] = 0;

        var parseNumber = false;
        var minus = false;
        int lastNumberIndex = 0;
        var dot = false;

        char[] charArray = line.toCharArray();
        for (char c : charArray) {
            if (c == ';') {
                stationName = line.substring(0, index);
                parseNumber = true;
                index=0;
                continue;
            }
            if (!parseNumber) {
                index++;
                continue;
            }
            if (dot) {
                afterDot(c, partNumber, index);
            } else {
                if(c == '-'){
                    minus = true;
                    continue;
                }
                dot = beforeDot(c, wholeNumber, index);
                if (dot) {
                    lastNumberIndex = index;
                    index = 0;
                    continue;
                }
            }
            index++;

        }

        var number = switch (lastNumberIndex){
            case 0 -> 0;
            case 1 -> wholeNumber[0];
            case 2 -> wholeNumber[0]*10+wholeNumber[1];
            case 3 -> wholeNumber[0] * 100 + wholeNumber[1] * 10 + wholeNumber[2];
            default -> 0L;
        } * 10000 + partNumber[0] * 1000 + partNumber[1] * 100 + partNumber[2] * 10 + partNumber[3];
        var value = minus ? -1 * number : number;
        var station = stations.computeIfAbsent(stationName, (_a) -> new Station());
        station.all += value;
        station.count += 1;
        station.max = Math.max(station.max, value);
        station.min = Math.min(station.min, value);
    }

    private boolean beforeDot(char c, long[] wholeNumber, int index) {
        switch (c) {
            case '.': {
                return true;
            }
            case '0': {
                wholeNumber[index] = 0;
                break;
            }
            case '1': {
                wholeNumber[index] = 1;
                break;
            }

            case '2': {
                wholeNumber[index] = 2;
                break;
            }
            case '3': {
                wholeNumber[index] = 3;
                break;
            }
            case '4': {
                wholeNumber[index] = 4;
                break;
            }

            case '5': {
                wholeNumber[index] = 5;
                break;
            }
            case '6': {
                wholeNumber[index] = 6;
                break;

            }
            case '7': {
                wholeNumber[index] = 7;
                break;
            }

            case '8': {
                wholeNumber[index] = 8;
                break;
            }
            case '9': {
                wholeNumber[index] = 9;
                break;
            }
        }
        return false;
    }

    private void afterDot(char c, long[] partNumber, int insertIndex) {
        switch (c) {
            case '0': {
                break;
            }
            case '1': {
                partNumber[insertIndex] = 1;
                break;
            }

            case '2': {
                partNumber[insertIndex] = 2;
                break;
            }
            case '3': {
                partNumber[insertIndex] = 3;
                break;
            }
            case '4': {
                partNumber[insertIndex] = 4;
                break;
            }

            case '5': {
                partNumber[insertIndex] = 5;
                break;
            }
            case '6': {
                partNumber[insertIndex] = 6;
                break;

            }
            case '7': {
                partNumber[insertIndex] = 7;
                break;
            }

            case '8': {
                partNumber[insertIndex] = 8;
                break;
            }
            case '9': {
                partNumber[insertIndex] = 9;
                break;
            }
        }

    }

}
