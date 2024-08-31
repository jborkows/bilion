package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static pl.jborkows.bilion.runners.complex.StepRunner.Processor.continuesWork;

public class LineReadingIT {
    @Test
   void readLines() throws URISyntaxException, InterruptedException, IOException {
        String name = "test_reading.csv";
        extracted(name);
    }

    @Test
    void readingVerySmall() throws URISyntaxException, InterruptedException, IOException {
        String name = "test_reading_very_small.csv";
        extracted(name);
    }

    @Test
    void readSmallLines() throws URISyntaxException, InterruptedException, IOException {
        String name = "test_reading_small.csv";
        extracted(name);
    }

    private static void extracted(String name) throws URISyntaxException, InterruptedException, IOException {
        var path= Paths.get(Thread.currentThread().
               getContextClassLoader().getResource(name).toURI());

        var fileReaderChannel = new MessageChannel<ByteChunkMessage>("File content", 1);
        var fileReader = new Thread(new FileReader(path, fileReaderChannel));

        var lineChannel = new MessageChannel<LineByteChunkMessage>("Line content", 1);
        var lineExtractor = new StepRunner<>("line extractor",fileReaderChannel, lineChannel, new LineExtractor());

        var temporaryFile = Files.createTempFile("extractor_"+name,"data");
        var finisher = new StepRunner<>("finisher",lineChannel, WriteChannel.none(), continuesWork((chunk, channel) -> {
            try {
                Files.write(temporaryFile, Arrays.copyOfRange(chunk.chunk(),chunk.beginIndex(), chunk.endIndex()), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BytePool.INSTANCE.release(chunk.chunk());
        }));
        finisher.start();
        lineExtractor.start();
        fileReader.start();
        fileReader.join();
        lineExtractor.done();
        lineExtractor.join();
        finisher.done();
        finisher.join();

        var allReadLines = Files.readAllLines(path);
        var temporalLines =Files.readAllLines(temporaryFile);

        for (int i = 0; i < allReadLines.size(); i++) {
            Assertions.assertEquals(  allReadLines.get(i), temporalLines.get(i), "Not found " + allReadLines.get(i) + " at line " + (i + 1));
        }
//        Files.deleteIfExists(temporaryFile);
    }
}
