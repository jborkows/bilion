package pl.jborkows.bilion.runners;


import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
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

    private final ThreadPoolExecutor linesProcessors = new ThreadPoolExecutor(
            6,
            10,
            10,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(5000),
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
            byte[] buffer = new byte[bufferSize + 1 * 1024];
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
                while (true) {
                    try {
                        linesProcessors.execute(() -> processNewLines(lines, count));
                        break;
                    } catch (RejectedExecutionException e) {
                        TimeUnit.MICROSECONDS.sleep(10);
                    }
                }
                System.arraycopy(buffer, 0, lines, 0, lastNewLineIndex);
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

        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", k.name(), (1.0 * v.all / v.count) / 100, v.max / 100.0, v.min / 100.0);
        });
    }

    private void processNewLines(byte[] lines, int totalNumberOfLines) {
        int i = 0;
        int beginWord = 0;
        int endWord = 0;
        int beginNumber = 0;
        List<LineOperator> operators = new ArrayList<>(totalNumberOfLines);
        for (byte b : lines) {
            if (b == ';') {
                endWord = i - 1;
                beginNumber = i + 1;
            } else if (b == '\n') {
                var endNumber = i - 1;
                if (beginWord > endWord) {
                    System.out.println(Thread.currentThread().getName() + " At index failure " + i);
                    System.out.println(beginWord + "->" + endWord);
                    System.out.println(new String(Arrays.copyOfRange(lines, beginWord, beginWord + 30), StandardCharsets.UTF_8));
                    System.out.println("#####");
                    System.out.println(new String(Arrays.copyOfRange(lines, endNumber, endNumber + 30), StandardCharsets.UTF_8));
                }else {
                    operators.add(new LineOperator(lines, beginWord, endWord, beginNumber, endNumber));
                }

                beginWord = i + 1;
            }
            i++;
        }
        processors.execute(() -> processNamedStations(operators));
    }


    record LineOperator(byte[] data, int beginWord, int endWord, int beginNumber, int endNumber) {
        public NumericNamed asNumericNamed() {
            var number = value();
            var hash = hashFor();
            return new NumericNamed(data, beginWord, endWord, number, hash);
        }

        private int hashFor() {
            var sum = 0;
            for (int i = beginWord; i < endWord + 1; i++) {
                sum = (data[i] + sum) * 31;
            }
            return sum;
        }

        private int value() {
            boolean minus = data[beginNumber] == '-';
            int numberIndexStart = minus ? 1 : 0;
            int wholeNumber = 0;
            int i = numberIndexStart + beginNumber;
            for (; i < endNumber + 1; i++) {
                if (data[i] == '.') {
                    break;
                }
                var parsed = parse(data[i]);
                wholeNumber = 10 * wholeNumber + parsed;
            }
            var decimal = parse(data[endNumber]);
            var number = (minus ? -1 : 1) * (wholeNumber * 10 + decimal);
            return number;
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
            if (!stations.containsKey(numericName)) {
                var newValue = numericName.persist();
                stations.put(newValue, stationFor(numericName, new Station()));
            } else {
                stations.computeIfPresent(numericName, ReadBytesSyncFirstToLines::stationFor);
            }
        }
    }

    private static Station stationFor(NumericNamed numericName, Station s) {
        s.all += numericName.value;
        s.count++;
        s.max = Math.max(s.max, numericName.value);
        s.min = Math.min(s.min, numericName.value);
        return s;
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
                    "name=" + name() + ", " +
                    "value=" + value + ", " +
                    "hash=" + hash + ']';
        }

        NumericNamed persist() {
            var name = Arrays.copyOfRange(data, beginWord, endWord + 1);

            var sum = 0;
            for (int i = beginWord; i < endWord + 1; i++) {
                sum = (data[i] + sum) * 31;
            }
            return new NumericNamed(name, 0, name.length - 1, value, sum);
        }

        public String name() {
            return new String(Arrays.copyOfRange(data, beginWord, endWord + 1), StandardCharsets.UTF_8);
        }

    }
}
