package pl.jborkows.bilion.runners.complex;


import pl.jborkows.bilion.runners.Runner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static pl.jborkows.bilion.runners.complex.StepRunner.Processor.continuesWork;

public class StagedRunner implements Runner {

    public void process(Path path) throws Exception {
        var fileReaderChannel = new MessageChannel<ByteChunkMessage>("File content", 8000);
        var fileReader = new Thread(new FileReader(path, fileReaderChannel));

        var lineChannel = new MessageChannel<LineByteChunkMessage>("Line content", 8000);
        var lineParser = new StepRunner<>("line parser",fileReaderChannel, lineChannel, new LineExtractor());

        var finisher = new StepRunner<>("finisher",lineChannel, WriteChannel.none(), continuesWork((chunk, channel) -> {
            BytePool.INSTANCE.release(chunk.chunk());
//            var message = chunk.toString().replace('\n','x');
//            System.out.println(message);
            //NOP
        }));
        finisher.start();
        lineParser.start();
        fileReader.start();
        fileReader.join();
        lineParser.done();
        lineParser.join();
        finisher.done();
        finisher.join();
        System.out.println("Read everything");
    }

}
