package pl.jborkows.bilion.runners;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadBytesSyncFirstToLines implements Runner {

    public static final int NAMED_STATIONS_BUFFER = 1000;

    private static class Station {
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        long all = 0;
        int count = 0;
    }

    private final Map<NumericNamed, Station> stations = new HashMap<>();
    private final AtomicInteger count = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "Processor-" + count.incrementAndGet())
    );
    private final ExecutorService joiner = Executors.newSingleThreadExecutor(r -> new Thread(r, "Joiner"));

    @Override
    public void process(Path path) throws Exception {

        final int bufferSize = 16*1024 ;
        Parser parser = textParser;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            byte[] buffer = new byte[bufferSize+1*1024];
            int bytesRead;
            int offset = 0;
            byte[] offsetBytes = new byte[1024];
            var linesCount = 0;
            while ((bytesRead = bis.read(buffer, offset, bufferSize)) != -1) {
                var countInsideBuffer = 0;
                var lastNewLineRead=0;
                var index = 0;
                if(offset > 0){
                    System.arraycopy(offsetBytes, 0, buffer, 0, offset);
                }


                System.out.println(linesCount + ": buffer -> " + new String(buffer, StandardCharsets.UTF_8).substring(0,50));

                for (byte b : buffer) {
                    index++;
                    if(b == '\n'){
                        lastNewLineRead=index;
                        linesCount++;
                        countInsideBuffer++;
                    }
                    if(index >= bytesRead+offset){
                        break;
                    }
                }
                var lines = new byte[lastNewLineRead];
                System.arraycopy(buffer, 0, lines, 0, lastNewLineRead);
//                if(index == bytesRead+offset){
//                    continue;
//                }
                var newoffset = (bytesRead + offset) - lastNewLineRead;
                if(newoffset > 0){
                    System.arraycopy(buffer, lastNewLineRead, offsetBytes, 0, newoffset);
                    System.out.println("newoffset -> " + new String(Arrays.copyOf(offsetBytes,newoffset), StandardCharsets.UTF_8));
                }
                System.out.println("$$$$$$$$$$$$$$$$$$");
//                System.out.println(new String(lines, StandardCharsets.UTF_8));
                System.out.println("$$$$$$$$$$$$$$$$$$");
                offset=newoffset;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Read everything");
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);
        joiner.shutdown();
        joiner.awaitTermination(2, TimeUnit.MINUTES);

        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", new String(k.name, StandardCharsets.UTF_8), (1.0 * v.all / v.count) / 100, v.max / 100.0, v.min / 100.0);
        });
    }


    public interface Parser {
        Parser parse(byte b);
    }

    private byte[] temporalName;
    private List<NamedStation> namedStations;

    private void addName(byte[] name) {
        temporalName = name;
    }

    private void addNumber(byte[] name) {
        if (namedStations == null) {
            namedStations = new ArrayList<>(NAMED_STATIONS_BUFFER);
        }
        namedStations.add(new NamedStation(temporalName, name));
        if (namedStations.size() == NAMED_STATIONS_BUFFER) {
            var temp = namedStations;
            namedStations = null;
            executor.submit(() -> {
                processNamedStations(temp);
            });
        }
    }

    private void processNamedStations(List<NamedStation> stations) {
        List<NumericNamed> numericNameds = new ArrayList<>(stations.size());
        for (NamedStation station : stations) {
            numericNameds.add(station.asNumericNamed());
        }
        joiner.submit(() -> joinValues(numericNameds));
    }

    private void joinValues(List<NumericNamed> numericNames) {
        for (NumericNamed numericName : numericNames) {
            stations.compute(numericName, (n, s) -> stationFor(numericName, s));
        }
    }

    private static Station stationFor(NumericNamed numericName, Station s) {
        if (s == null) {
            s = new Station();
        }
        s.all += numericName.value;
        s.count++;
        s.max = Math.max(s.max, numericName.value);
        s.min = Math.min(s.min, numericName.value);
        return s;
    }


    private final byte[] name = new byte[4096];
    private final TextParser textParser = new TextParser();
    private final NumberParser numberParser = new NumberParser();

    public class TextParser implements Parser {
        private int nameLength = 0;

        @Override
        public Parser parse(byte b) {
            if (b == 0) {
                return this;
            } else if (b == ';') {
                prepareSwitching();
                return numberParser.fresh();
            } else {
                appendName(b);
                return this;
            }
        }

        private void prepareSwitching() {
            byte[] copied = new byte[nameLength];
            System.arraycopy(name, 0, copied, 0, nameLength);
            addName(copied);
        }

        private void appendName(byte b) {
            name[nameLength++] = b;
        }

        public Parser fresh() {
            nameLength = 0;
            return this;
        }
    }

    public class NumberParser implements Parser {
        private int nameLength = 0;

        @Override
        public Parser parse(byte b) {
            if (b == 0) {
                return this;
            } else if (b == '\n') {
                byte[] copied = new byte[nameLength];
                System.arraycopy(name, 0, copied, 0, nameLength);
                addNumber(copied);
                return textParser.fresh();
            } else {
                name[nameLength++] = b;
                return this;
            }
        }

        public Parser fresh() {
            nameLength = 0;
            return this;
        }
    }

    private static int parse(byte c) {
        return c - '0';
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }

    private static final class NamedStation {
        final byte[] name;
        final byte[] value;
        private final int hash;

        private NamedStation(byte[] name, byte[] value) {
            this.name = name;
            this.value = value;
            this.hash = Arrays.hashCode(name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NamedStation that = (NamedStation) o;
            return Objects.equals(hash, that.hash);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(hash);
        }

        @Override
        public String toString() {
            return "NamedStation{" +
                    "name=" + new String(name, StandardCharsets.UTF_8) +
                    ", value=" + new String(value, StandardCharsets.UTF_8) +
                    ", hash=" + hash +
                    '}';
        }

        NumericNamed asNumericNamed() {
            boolean minus = value[0] == '-';
            int numberIndexStart = minus ? 1 : 0;
            int wholeNumber = 0;
            int i = numberIndexStart;
            for (; i < value.length; i++) {
                if (value[i] == '.') {
                    break;
                }
                var parsed = parse(value[i]);
                wholeNumber = 10 * wholeNumber + parsed;
            }
            var decimal = parse(value[value.length - 1]);
            var number = (minus ? -1 : 1) * (wholeNumber * 10 + decimal);
            return new NumericNamed(name, number, hash);
        }
    }


    private static final class NumericNamed {
        private final byte[] name;
        private final int value;
        private final int hash;

        private NumericNamed(byte[] name, int value, int hash) {
            this.name = name;
            this.value = value;
            this.hash = hash;
        }

        public byte[] name() {
            return name;
        }

        public long value() {
            return value;
        }

        public int hash() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (NumericNamed) obj;
            return this.hash == that.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "NumericNamed[" +
                    "name=" + new String(name, StandardCharsets.UTF_8) + ", " +
                    "value=" + value + ", " +
                    "hash=" + hash + ']';
        }

    }
}
