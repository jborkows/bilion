package pl.jborkows.bilion.runners;


import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadBytesSyncFirstToLinesVectors implements Runner {
    private final static VectorSpecies<Integer> SPECIES = IntVector.SPECIES_512;
    private final static VectorSpecies<Integer> SPECIES_SMALLER = IntVector.SPECIES_256;
    private final static IntVector hashMultipliers = multipliers();

    private static IntVector multipliers() {
        int[] values = new int[SPECIES_SMALLER.elementSize()];
        for (int i = 0; i < values.length; i++) {
            values[i] = 31*i;
        }
        var mask = SPECIES_SMALLER.indexInRange(0,values.length);
        return IntVector.fromArray(SPECIES_SMALLER,values,0,mask);
    }

    private static class Station {
        public String name;
        public int hash;
        List<Integer> values = new ArrayList<>(32);
    }

    private static class DisplayableStation {
        public String name;
        float max;
        float min;
        float average;
    }

    private final Map<Integer, Station> stations = new HashMap<>();
    private final AtomicInteger count = new AtomicInteger();

    private final ThreadPoolExecutor linesProcessors = new ThreadPoolExecutor(
            6,
            10,
            10,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(4000),
            r -> new Thread(r, "Lines-" + count.incrementAndGet())
    );
    private final ExecutorService processors = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "Processor-" + count.incrementAndGet())
    );
    private final ExecutorService joiner = Executors.newSingleThreadExecutor(r -> new Thread(r, "Joiner"));

    @Override
    public void process(Path path) throws Exception {

        final int bufferSize = 16 * 1024;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));) {
            byte[] buffer = new byte[bufferSize + 1024];
            int bytesRead;
            int offset = 0;
            byte[] offsetBytes = new byte[1024];
            while ((bytesRead = bis.read(buffer, offset, bufferSize)) != -1) {
                var lastNewLineIndex = 0;
                var index = 0;
                if (offset > 0) {
                    System.arraycopy(offsetBytes, 0, buffer, 0, offset);
                }
                var countLines = 0;
                for (byte b : buffer) {
                    index++;
                    if (b == '\n') {
                        lastNewLineIndex = index;
                        countLines++;
                    }
                    if (index >= bytesRead + offset) {
                        break;
                    }
                }
                var lines = new byte[lastNewLineIndex];
                var count = countLines;
                System.arraycopy(buffer, 0, lines, 0, lastNewLineIndex);
                while (true) {
                    try {
                        linesProcessors.execute(() -> processNewLines(lines, count));
                        break;
                    } catch (RejectedExecutionException e) {
                        TimeUnit.MICROSECONDS.sleep(10);
                    }
                }
                var newoffset = (bytesRead + offset) - lastNewLineIndex;
                if (newoffset > 0) {
                    System.arraycopy(buffer, lastNewLineIndex, offsetBytes, 0, newoffset);
                }
                offset = newoffset;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Read everything");
        linesProcessors.shutdown();
        linesProcessors.awaitTermination(1, TimeUnit.MINUTES);
        processors.shutdown();
        processors.awaitTermination(2, TimeUnit.MINUTES);
        joiner.shutdown();
        joiner.awaitTermination(2, TimeUnit.MINUTES);

        System.out.println("Preparing sum up");
        stations.entrySet().parallelStream().map(entry->convertToDisplay(entry.getValue())).forEach( v -> {
            System.out.printf("%s;%s;%s;%s;\n", v.name, v.average, v.max , v.min );
        });
    }

    private DisplayableStation convertToDisplay(Station v) {
        int[] values = new int[v.values.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = v.values.get(i);
        }
        var vector = IntVector.fromArray(SPECIES, values, 0);
        var displayable = new DisplayableStation();

        displayable.name=v.name;
        displayable.max = vector.reduceLanes(VectorOperators.MAX) /10.0f;
        displayable.min = vector.reduceLanes(VectorOperators.MIN) / 10.0f;
        displayable.average= (float) vector.reduceLanes(VectorOperators.ADD) / values.length /10.0f;
        return displayable;
    }

    private void processNewLines(byte[] lines, int totalNumberOfLines) {
        List<LineOperator> operators = lineOperatorsFrom(lines, totalNumberOfLines);
        processors.execute(() -> processNamedStations(operators));
    }

    static List<LineOperator> lineOperatorsFrom(byte[] lines, int totalNumberOfLines) {
        int beginWord = 0;
        int endWord = 0;
        int beginNumber = 0;
        List<LineOperator> operators = new ArrayList<>(totalNumberOfLines);
        for (int i = 0, linesLength = lines.length; i < linesLength; i++) {
            byte b = lines[i];
            switch (b) {
                case ';' -> {
                    endWord = i - 1;
                    beginNumber = i + 1;
                }
                case '\n' -> {
                    var endNumber = i - 1;
                    operators.add(new LineOperator(lines, beginWord, endWord, beginNumber, endNumber));
                    beginWord = i + 1;
                }
            }
        }
        return operators;
    }


    record LineOperator(byte[] data, int beginWord, int endWord, int beginNumber, int endNumber) {
        public NumericNamed asNumericNamed() {
            var number = value();
            var hash = hashFor();
            return new NumericNamed(data, beginWord, endWord, number, hash);
        }

        private int hashFor() {
            var mask = SPECIES_SMALLER.indexInRange(0, endNumber-beginNumber);
            var temp = tempArray(data, beginNumber, endNumber);
            var toCalculate = IntVector.fromArray(SPECIES_SMALLER, temp, 0,mask);
            return toCalculate.mul(hashMultipliers)
                    .reduceLanes(VectorOperators.ADD);
        }

        private static int[] tempArray(byte[] data,int beginNumber, int endNumber) {
            var temp = new int[endNumber-beginNumber];
            for(var i = 0; i < temp.length; i++) {
                temp[i]=data[i+beginNumber];
            }
            return temp;
        }

        private int value() {
            boolean minus = data[beginNumber] == '-';
            var third = parse(data[endNumber-2])*10;
            var second = parse(data[endNumber-3])*100;
            var first = parse(data[endNumber-4])*1000;
            var decimal = parse(data[endNumber]);
            var wholeNumber = third+second+first+decimal;
            return (minus ? -1 : 1) * (wholeNumber * 10 + decimal);
        }
    }


    private void processNamedStations(List<LineOperator> stations) {
        List<NumericNamed> numericNameds = new ArrayList<>(stations.size());
        for (var station : stations) {
            numericNameds.add(station.asNumericNamed());
        }
        joiner.submit(() -> joinValues(numericNameds));
    }

    private void joinValues(List<NumericNamed> numericNames) {
        for (NumericNamed numericName : numericNames) {
            Station station = stations.get(numericName.hash);
            if (station == null) {
                station = new Station();
                station.name = numericName.name();
                station.hash = numericName.hash;
                stations.put(numericName.hash, station);
            }
            stationFor(numericName, station);
        }
    }

    private static void stationFor(NumericNamed numericName, Station s) {
        s.values.add(numericName.value);
    }

    private static int parse(byte c) {
        return c - '0';
    }

    @Override
    public String name() {
        return this.getClass().getName();
    }

    private static final class NumericNamed {
        private final byte[] data;
        private final int beginWord;
        private final int endWord;
        private final int value;
        private final int hash;

        private NumericNamed(byte[] data, int beginWord, int endWord, int value, int hash) {
            this.data = data;
            this.beginWord = beginWord;
            this.endWord = endWord;
            this.value = value;
            this.hash = hash;
        }


        @Override
        public boolean equals(Object obj) {
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
                    "name=" + name() + ", " +
                    "value=" + value + ", " +
                    "hash=" + hash + ']';
        }

        public String name() {
            return new String(Arrays.copyOfRange(data, beginWord, endWord + 1), StandardCharsets.UTF_8);
        }

    }
}
