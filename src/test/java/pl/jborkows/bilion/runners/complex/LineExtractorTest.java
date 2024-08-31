package pl.jborkows.bilion.runners.complex;

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
        lineExtractor.accept(new ByteChunkMessage("Some text\n"), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size(), "String received " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
    }

    @Test
    void shouldReadWholeLineEvenIfBufferIsNotFull() {
        var receiver = new Receiver();
        var buffer = new byte[100];
        var text= "Some text\n\n";
        System.arraycopy(text.getBytes(), 0, buffer, 0, text.length());
        lineExtractor.accept(new ByteChunkMessage(buffer, text.length()), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size(), "String received " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
        assertEquals("Some text", receiver.read.getFirst().trim());
    }

    @Test
    void shouldReadWholeLineAndIgnoreEmptyLine() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Some text\n\n"), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size(), "String received " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
        assertEquals("Some text", receiver.read.getFirst().trim());
    }

    @Test
    void shouldIgnoreEndOfLine() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Some text\n"), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size(), "String received " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
    }

    @Test
    void shouldNotWriteNotCompletedLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Part of line"), receiver);
        assertEquals(0, receiver.read.size());
    }

    @Test
    void shouldWriteLineEvenIfNotCompletedWhenFinished() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Part of line"), receiver);
        lineExtractor.finish(receiver);
        assertEquals(1, receiver.read.size());
        assertEquals("Part of line", receiver.read.get(0));
    }

    @Test
    void shouldBeAbleToReadMultipleLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nLine 2\nLine 3\n some text"), receiver);
        lineExtractor.finish(receiver);
        assertEquals(2, receiver.read.size());
        assertEquals("Line 1\nLine 2\nLine 3", receiver.read.get(0));
        assertEquals(" some text", receiver.read.get(1));
    }

    @Test
    void shouldReadExactlyLines() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nLine 2\nLine 3\n"), receiver);
        lineExtractor.accept(new ByteChunkMessage("Line 4\nLine 5\n"), receiver);
        lineExtractor.finish(receiver);
        expectSize(2, receiver);
        assertEquals("Line 1\nLine 2\nLine 3\n", receiver.read.get(0));
        assertEquals("Line 4\nLine 5\n", receiver.read.get(1));
    }

    private static void expectSize(int expectedSize, Receiver receiver) {
        assertEquals(expectedSize, receiver.read.size(), "Got " + receiver.read.stream().map(i->"'"+i+"'").collect(Collectors.joining(",")));
    }

    @Test
    void shouldBeAbleToJoinFromParts() {
        var receiver = new Receiver();
        lineExtractor.accept(new ByteChunkMessage("Line 1\nPart of line 2"), receiver);
        lineExtractor.accept(new ByteChunkMessage(" continued 2\nsome other"), receiver);
        lineExtractor.accept(new ByteChunkMessage(" continued \nsome text at the end"), receiver);
        lineExtractor.finish(receiver);
        expectSize(4,receiver);
        assertEquals("Line 1", receiver.read.get(0));
        assertEquals("Part of line 2 continued 2", receiver.read.get(1));
        assertEquals("some other continued ", receiver.read.get(2));
        assertEquals("some text at the end", receiver.read.get(3));
    }


    private static class Receiver implements WriteChannel<LineByteChunkMessage> {
        private final List<String> read = new ArrayList<>();

        @Override
        public void writeTo(LineByteChunkMessage lineByteChunkMessages) {
            read.add(lineByteChunkMessages.toString());
        }
    }
}
