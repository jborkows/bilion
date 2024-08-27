package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size());
    }

    @Test
    void shouldIgnoreEndOfLine() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Some text\n".getBytes()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size(), "String received " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
    }

    @Test
    void shouldNotWriteNotCompletedLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Part of line".getBytes()), receiver);
        assertEquals(0, receiver.read.size());
    }

    @Test
    void shouldWriteLineEvenIfNotCompletedWhenFinished() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Part of line".getBytes()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size());
        assertEquals("Part of line", receiver.read.get(0));
    }

    @Test
    void shouldBeAbleToReadMultipleLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nLine 2\nLine 3\n some text".getBytes()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(4, receiver.read.size());
        assertEquals("Line 1", receiver.read.get(0));
        assertEquals("Line 2", receiver.read.get(1));
        assertEquals("Line 3", receiver.read.get(2));
        assertEquals(" some text", receiver.read.get(3));
    }

    @Test
    void shouldReadExactlyLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nLine 2\nLine 3\n".getBytes()), receiver);
        lineExtractor.accept(new ByteChunkMessage("Line 4\nLine 5\n".getBytes()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(5, receiver.read.size(), "Got " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
        assertEquals("Line 1", receiver.read.get(0));
        assertEquals("Line 2", receiver.read.get(1));
        assertEquals("Line 3", receiver.read.get(2));
        assertEquals("Line 4", receiver.read.get(3));
        assertEquals("Line 5", receiver.read.get(4));
    }

    @Test
    void shouldBeAbleToJoinFromParts() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nPart of line 2".getBytes()), receiver);
        lineExtractor.accept(new ByteChunkMessage(" continued 2\nsome other".getBytes()), receiver);
        lineExtractor.accept(new ByteChunkMessage(" continued \nsome text at the end".getBytes()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(4, receiver.read.size());
        assertEquals("Line 1", receiver.read.get(0));
        assertEquals("Part of line 2 continued 2", receiver.read.get(1));
        assertEquals("some other continued ", receiver.read.get(2));
        assertEquals("some text at the end", receiver.read.get(3));
    }


    private static class Receiver implements WriteChannel<LineByteChunkMessages> {
        private final List<String> read = new ArrayList<>();

        @Override
        public void writeTo(LineByteChunkMessages lineByteChunkMessages) {
            lineByteChunkMessages.forEach(line->read.add(line.toString()));
        }
    }
}
