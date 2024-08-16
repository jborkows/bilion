package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OwnSplitDoubleActiveParserIndexBasedLimitedHashFun implements Runner {
    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }


    private static class Container {
        final String name;
        final int funHash;
        final Station station;

        public Container(String name, int funHash) {
            this.name = name;
            this.funHash = funHash;
            this.station = new Station();
        }

    }

    private final Map<Integer, Container> stations = new HashMap<>();

    @Override
    public void process(Path path) throws Exception {
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.filter(line -> !line.startsWith("#")).forEach(this::processLine);
        }
        stations.forEach((_k, pair) -> {
            var k = pair.name;
            var v = pair.station;
            System.out.printf("%s;%s;%s;%s;\n", k, (1.0 * v.all / v.count) / 10000, 1.0 * v.max / 10000, 1.0* v.min / 10000);
        });
    }

    long number = 0;
    boolean parseNumber = false;
    boolean minus = false;
    boolean dot = false;

    private void processLine(String line) {
        var stationName = "";
        number = 0;
        parseNumber = false;
        minus = false;
        dot = false;
        int firstNumberIndex = 0, lastNumberIndex = 0, decimalIndex = 0, decimalIndexEnd = 0;

        char[] charArray = line.toCharArray();
        int runningIndex = 0;
        for (char c : charArray) {
            if (c == ';') {
                stationName = line.substring(0, runningIndex);
                firstNumberIndex = runningIndex + 1;
                parseNumber = true;
                runningIndex++;
                continue;
            }
            if (!parseNumber) {
                runningIndex++;
                continue;
            }
            switch (c) {
                case '.' -> {
                    lastNumberIndex = runningIndex - 1;
                    decimalIndex = runningIndex + 1;
                }
                case '-' -> {
                    minus = true;
                    firstNumberIndex++;
                }
                default -> {
                }
            }

            runningIndex++;
        }
        decimalIndexEnd = runningIndex - 1;

        var wholeNumber = switch (lastNumberIndex - firstNumberIndex) {
            case 0 -> parse(charArray[firstNumberIndex]);
            case 1 -> parse(charArray[firstNumberIndex]) * 10 + parse(charArray[lastNumberIndex]);
            case 2 ->
                    parse(charArray[firstNumberIndex]) * 100 + parse(charArray[firstNumberIndex + 1]) * 10 + parse(charArray[lastNumberIndex]);
            default -> 0L;
        } * 10000;

        var decimalNumber = switch (decimalIndexEnd - decimalIndex) {
            case 0 -> parse(charArray[decimalIndex]) * 1000;
            case 1 -> parse(charArray[decimalIndex]) * 1000 + parse(charArray[decimalIndexEnd]) * 100;
            case 2 ->
                    parse(charArray[decimalIndex]) * 1000 + parse(charArray[decimalIndex + 1]) * 100 + parse(charArray[decimalIndexEnd]) * 10;
            case 3 ->
                    parse(charArray[decimalIndex]) * 1000 + parse(charArray[decimalIndex + 1]) * 100 + parse(charArray[decimalIndex + 2]) * 10 + parse(charArray[decimalIndexEnd]);
            default -> 0L;
        };
        var number = wholeNumber + decimalNumber;
        var value = minus ? -1 * number : number;
        var station = getStation(stationName);
        station.all += value;
        station.count += 1;
        station.max = Math.max(station.max, value);
        station.min = Math.min(station.min, value);
    }

    private static class NameWithHash {
        final String name;
        final int hash;

        private NameWithHash(String name, int hash) {
            this.name = name;
            this.hash = hash;
        }
    }

    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private final Map<Integer, List<NameWithHash>> complexHashing = new HashMap<>();

    private Station getStation(String stationName) {
        var letters = stationName.toCharArray();
        var secondLevel = levelCache(letters);
        int funHash = complexHash(secondLevel, stationName);

        if (!stations.containsKey(funHash)) {
            stations.put(funHash, new Container(stationName, funHash));
        }
        var container = stations.get(funHash);
        if (!stationName.equals(container.name)) {
            throw new IllegalStateException("Station name mismatch was " + container.name + " got " + stationName + " " + container.funHash + "->" + funHash);
        }
        return container.station;
    }

    private List<NameWithHash> levelCache(char[] letters) {
        var key = keyFrom(letters);
        var result = complexHashing.get(key);
        if (result != null) {
            return result;
        }
        var created = new ArrayList<NameWithHash>(2);
        complexHashing.put(key, created);
        return created;
    }

    private static int keyFrom(char[] letters) {
        return letters[0] * 31 + letters[1] * 15;
    }

    private int complexHash(List<NameWithHash> secondLevel, String stationName) {
        for (NameWithHash nameWithHash : secondLevel) {
            if (nameWithHash.name.equals(stationName)) {
                return nameWithHash.hash;
            }
        }
        var hash = atomicInteger.incrementAndGet();
        secondLevel.add(new NameWithHash(stationName, hash));
        return hash;
    }

    private static long parse(char c) {
        return c - '0';
    }
}
