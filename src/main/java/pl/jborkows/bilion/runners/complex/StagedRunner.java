package pl.jborkows.bilion.runners.complex;


import pl.jborkows.bilion.runners.Runner;

import java.nio.file.Path;
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
        var finisher = new StepRunner<>("finisher",linesChannel, WriteChannel.none(), continuesWork((chunk, channel) -> {
//            var message = chunk.toString().replace('\n','x');
//            System.out.println(message);
            //NOP
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

}
