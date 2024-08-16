package pl.jborkows.bilion.runners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OwnSplitDoubleActiveParserIndexBasedHashFun implements Runner {
    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }

    private static class ContainerWrapper {
        private Container container;

        public ContainerWrapper(String stationName) {
            this.container = new SingleElement(stationName);
        }

        public Station getStation(String name) {
            return container.findStation(name);
        }

        public Station addStation(String stationName) {
            container = container.addStation(stationName);
            return container.findStation(stationName);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContainerWrapper that = (ContainerWrapper) o;
            return Objects.equals(container, that.container);
        }

        @Override
        public int hashCode() {
            return container.hashCode();
        }
    }

    private interface Container {
        Container addStation(String name);

        Station findStation(String name);

        List<Pair> stations();
    }

    private static class SingleElement implements Container {
        private final String name;
        private final Station station;
        private final int hash;

        public SingleElement(String name) {
            this.name = name;
            this.station = new Station();
            this.hash = name.length();
        }

        @Override
        public Container addStation(String name) {
            if (name.equals(this.name)) {
                return this;
            }else{
                return new MultiElement(this.name, station, name);
            }

        }

        @Override
        public Station findStation(String name) {
            return station;
        }

        @Override
        public List<Pair> stations() {
            return List.of(new Pair(this.name, this.station));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SingleElement that = (SingleElement) o;
            return hash == that.hash && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class Pair {
        private final String name;
        private final Station station;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return Objects.equals(name, pair.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name);
        }

        private Pair(String name, Station station) {
            this.name = name;
            this.station = station;
        }
    }

    private static class MultiElement implements Container {
        private final List<Pair> pairs;
        private final int hash;

        public MultiElement(String name, Station station, String otherName) {
        pairs = new ArrayList<>();
        pairs.add(new Pair(name, station));
        pairs.add(new Pair(otherName, new Station()));
        hash = name.length();
        }

        @Override
        public Container addStation(String name) {
            if(pairs.stream().anyMatch(pair -> pair.name.equals(name))){
                return this;
            }
            pairs.add(new Pair(name, new Station()));
            return this;
        }

        @Override
        public Station findStation(String name) {
            for(Pair pair : pairs){
                if(pair.name.equals(name)){
                    return pair.station;
                }
            }
            return null;
        }

        @Override
        public List<Pair> stations() {
            return pairs;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Don't use");
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hash);
        }
    }

    private final Map<Integer, ContainerWrapper> stations = new HashMap<>();

    @Override
    public void process(Path path) throws Exception {
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.filter(line -> !line.startsWith("#")).forEach(this::processLine);
        }
        stations.forEach((_k, vs) -> {
            vs.container.stations().forEach(pair->{
               var k = pair.name;
               var v = pair.station;
                System.out.printf("%s;%s;%s;%s;\n", k, (1.0 * v.all / v.count) / 1000, v.max / 1000, v.min / 1000);

            });
        });
    }

    long number = 0;
    boolean parseNumber = false;
    boolean minus = false;
    long scaler = 1;
    boolean dot = false;

    private void processLine(String line) {
        var stationName = "";
        var index = 0;
        number = 0;
        parseNumber = false;
        minus = false;
        scaler = 1;
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

    private Station getStation(String stationName) {
        return stations.computeIfAbsent(stationName.length(), (_a) -> new ContainerWrapper(stationName)).addStation(stationName);
    }

    private static long parse(char c) {
        return c - '0';
    }

}
