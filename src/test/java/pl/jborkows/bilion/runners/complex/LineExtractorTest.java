package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class LineExtractorTest {
    private LineExtractor lineExtractor;

    @BeforeEach
    void setUp() {
        lineExtractor = new LineExtractor();
    }


    @Test
    void shouldReadWholeLine() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Some text\n".getBytes()), receiver);
        Assertions.assertEquals(1, receiver.read.size());
    }

    @Test
    void shouldIgnoreEndOfLine() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Some text\n".getBytes()), receiver);
        Assertions.assertEquals(1, receiver.read.size());
    }

    @Test
    void shouldNotWriteNotCompletedLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Part of line".getBytes()), receiver);
        Assertions.assertEquals(0, receiver.read.size());
    }




    private static class Receiver implements WriteChannel<LineByteChunkMessage> {
        private final List<byte[]> read = new ArrayList<>();

        @Override
        public void writeTo(LineByteChunkMessage lineByteChunkMessage) {
            read.add(lineByteChunkMessage.line);
        }
    }
}
