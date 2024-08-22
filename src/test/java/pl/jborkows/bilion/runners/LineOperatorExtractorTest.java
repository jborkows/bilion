package pl.jborkows.bilion.runners;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LineOperatorExtractorTest {
    @Test
    void shouldRead() throws URISyntaxException, IOException {

        Path path = Paths.get(Thread.currentThread().
                getContextClassLoader().getResource("aaa.out").toURI());
        var totalLines = linesInFile(path);
        var bytes = Files.readAllBytes(path);
        var read = ReadBytesSyncFirstToLines.lineOperatorsFrom(bytes, totalLines);
        Assertions.assertEquals(totalLines, read.size());
    }

    private int linesInFile(Path path) throws IOException {

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", "wc -l " + path.toAbsolutePath());
            Process process = pb.start();
            try (var input = new InputStreamReader(process.getInputStream());
                 BufferedReader reader = new BufferedReader(input);
            ) {

                String line;
                while ((line = reader.readLine()) != null) {
                    var count = line.split(" ")[0];
                    return Integer.parseInt(count);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        throw new RuntimeException("Nope");
    }
}
