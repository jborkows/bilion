package pl.jborkows.bilion.runners.complex;


import pl.jborkows.bilion.runners.Runner;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StagedRunner implements Runner {

    public void process(Path path) throws Exception {
        var fileReaderChannel = new MessageChannel<ByteChunkMessage>("File content");
        var fileReader = new Thread(new FileReader(path, fileReaderChannel));
        var lineChannel = new MessageChannel<LineByteChunkMessage>("Line content");
        var lineExtractor = new StepRunner<>("line extractor", fileReaderChannel, lineChannel, new LineExtractor());
        var store = new Store();
        var lineParsers = IntStream.rangeClosed(1, 8).mapToObj(i -> new StepRunner<>("line parser " + i, lineChannel, WriteChannel.none(), new LineParser(store))).toList();
        lineParsers.forEach(Thread::start);
        lineExtractor.start();
        fileReader.start();
        fileReader.join();
        lineExtractor.done();
        lineExtractor.join();
        System.out.println("waiting for line parsers");
        for (var lineParser : lineParsers) {
            lineParser.done();
            lineParser.join();

        }
        System.out.println("Read everything");
        Store.results.parallelStream().flatMap(e -> e.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, StagedRunner::mergeData))
                .values()
                .parallelStream()
                .forEach(System.out::println);
    }

    private static Store.Data mergeData(Store.Data d1, Store.Data d2) {
        d1.max = Math.max(d2.max, d1.max);
        d1.min = Math.min(d2.min, d1.min);
        d1.count = d1.count + d2.count;
        d1.sum = d1.sum + d2.sum;
        return d1;
    }


}
