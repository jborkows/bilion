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

public class FileReaderIT {
    @Test
    void readLines() throws URISyntaxException, InterruptedException, IOException {
        String name = "test_reading.csv";
        extracted(name);
    }

    @Test
    void readSmallLines() throws URISyntaxException, InterruptedException, IOException {
        String name = "test_reading_small.csv";
        extracted(name);
    }

    private static void extracted(String name) throws URISyntaxException, InterruptedException, IOException {
        var path = Paths.get(Thread.currentThread().
                getContextClassLoader().getResource(name).toURI());

        var fileReaderChannel = new MessageChannel<ByteChunkMessage>("File content");
        var fileReader = new Thread(new FileReader(path, fileReaderChannel));

        var tempFile = Files.createTempFile("reading","txt");
        var finisher = new StepRunner<>("finisher", fileReaderChannel, WriteChannel.none(), continuesWork((chunk, channel) -> {
            try {
                Files.write(tempFile, Arrays.copyOf(chunk.chunk, chunk.length), StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            BytePool.release(chunk.chunk);
        }));
        finisher.start();
        fileReader.start();
        fileReader.join();
        finisher.done();
        finisher.join();

        var allReadLines = Files.readAllLines(path);
        var temporalLines =Files.readAllLines(tempFile);
        for (int i = 0; i < allReadLines.size(); i++) {
            Assertions.assertEquals(  allReadLines.get(i), temporalLines.get(i), "Not found " + allReadLines.get(i) + " at line " + (i + 1));
        }
        Files.deleteIfExists(tempFile);
    }
}
