package pl.jborkows.bilion.runners;


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class OwnSplitDoubleActiveParserIndexBasedMultipleThreads implements Runner {

    private static class Station {
        long max = 0;
        long min = 0;
        long all = 0;
        int count = 0;
    }

    private final Map<String, Station> stations = new ConcurrentHashMap<>();
    private final BlockingDeque<List<String>> todoLines = new LinkedBlockingDeque<>();
    private volatile boolean doneChannel = false;

    private List<String> buffer;
    private final int threadCount;

    private ExecutorService executors = Executors.newFixedThreadPool(5);
    public OwnSplitDoubleActiveParserIndexBasedMultipleThreads(int threadCount) {
        executors = Executors.newFixedThreadPool(threadCount);
        this.threadCount = threadCount;
    }

    @Override
    public void process(Path path) throws Exception {

        var processor = new Thread(this::doWork);
        processor.start();
        try (var stream = Files.lines(path, StandardCharsets.UTF_8)) {
            stream.forEach(line -> {
                if (line.startsWith("#")) {
                    return;
                }
                addToBuffer(line);
            });
        }
        finish();
        System.out.println("Waiting for finish");
        processor.join();
        stations.forEach((k, v) -> {
            System.out.printf("%s;%s;%s;%s;\n", k, (1.0 * v.all / v.count) / 100, v.max / 100.0, v.min / 100.0);
        });
    }

    private void addToBuffer(String line) {
        int bufferSize = 2000;
        if (buffer == null) {
            buffer = new ArrayList<>(bufferSize);
        }
        buffer.add(line);
        if (buffer.size() == bufferSize) {
            todoLines.push(buffer);
            buffer = null;
        }
    }

    private void finish() throws InterruptedException {
        if (buffer != null) {
            todoLines.push(buffer);
        }
        doneChannel = true;
        executors.shutdown();
        executors.awaitTermination(1, TimeUnit.MINUTES);
    }

    private void doWork() {
        int internalBuffer = 100;
        try {
            while (!doneChannel) {
                var read = todoLines.poll(1, TimeUnit.SECONDS);
                if (read == null) {
                    continue;
                }

                List<String> partProcessing = null;
                for (String line : read) {
                    if (partProcessing == null) {
                        partProcessing = new ArrayList<>(internalBuffer);
                    }
                    partProcessing.add(line);
                    if (partProcessing.size() == internalBuffer) {
                        final var temp = partProcessing;
                        executors.submit(() -> {
                            for (String s : temp) {
                                processLine(s);
                            }
                        });
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
        stations.compute(stationName, (name, station) -> {
            if (station == null) {
                station = new Station();
            }
            station.all += value;
            station.count += 1;
            station.max = Math.max(station.max, value);
            station.min = Math.min(station.min, value);
            return station;
        });
    }

    private static long parse(char c) {
        return c - '0';
    }

    @Override
    public String name() {
        return this.getClass().getName() + "_" + threadCount;
    }
}
