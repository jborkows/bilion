package pl.jborkows.bilion.runners.complex;


import pl.jborkows.bilion.runners.Runner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static pl.jborkows.bilion.runners.complex.StepRunner.Processor.continuesWork;

public class StagedRunner implements Runner {

    public void process(Path path) throws Exception {
        var fileReaderChannel = new MessageChannel<ByteChunkMessage>("File content", 8000);
        var fileReader = new Thread(new FileReader(path, fileReaderChannel));

        var lineChannel = new MessageChannel<LineByteChunkMessage>("Line content", 8000);
        var lineExtractor = new StepRunner<>("line extractor",fileReaderChannel, lineChannel, new LineExtractor());

        var linesChannel = new MessageChannel<ParsedLineMessage>("parsed lines", 80_000);
        var lineParsers = IntStream.rangeClosed(1,6).mapToObj(i-> new StepRunner<>("line parser " + i, lineChannel,linesChannel, new LineParser())).toList();
        final var result = new ConcurrentHashMap<Data, List<Integer>>();
        var finisher = new StepRunner<>("finisher",linesChannel, WriteChannel.none(), continuesWork((chunk, channel) -> {
//            chunk.parsedLineItems().parallelStream().forEach(line -> {
//                result.compute(new Data(line.name(),line.begin(),line.offsetName()), (d,v) -> {
//                    if (v == null) {
//                        v = new ArrayList<>();
//                    }
//                    v.add(line.value());
//                    return v;
//                });
//            });
        }));
        finisher.start();
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
        finisher.done();
        finisher.join();
        System.out.println("Read everything");
    }

    private static final class Data {
        private final byte[] name;
        private final int begin;
        private final int offsetName;

        private Data(byte[] name, int begin, int offsetName  ) {
            this.name = name;
            this.begin = begin;
            this.offsetName = offsetName;
        }


        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Data) obj;
            return Arrays.equals(this.name, this.begin, this.begin + this.offsetName, that.name, that.begin, that.begin + that.offsetName);
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for(int i = begin;i<begin+offsetName;i++) {
                hash = 31*hash + name[i];
            }
            return hash;
        }
    }


}
