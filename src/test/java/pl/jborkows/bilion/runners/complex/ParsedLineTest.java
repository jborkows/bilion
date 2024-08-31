package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ParsedLineTest {
    private LineParser lineParser;

    @BeforeEach
    void setUp() {
        lineParser = new LineParser();
    }


    @Test
    void shouldParseName() {
        var receiver = new Receiver();
        lineParser.accept(LineByteChunkMessage.fromString("AAąAA;12.34\nB C;12.34"), receiver);
        assertFalse(receiver.read.isEmpty());
        assertEquals("AAąAA", receiver.read.getFirst().stationName());
        assertEquals("B C", receiver.read.get(1).stationName());
    }

    @Test
    void shouldParseNumber() {
        var receiver = new Receiver();
        lineParser.accept(LineByteChunkMessage.fromString("B;-12.34\nC;9.5"), receiver);
        assertFalse(receiver.read.isEmpty());
        assertEquals(-12, receiver.read.getFirst().integerPart());
        assertEquals(340, receiver.read.getFirst().decimalPart());
        assertEquals(9, receiver.read.get(1).integerPart());
        assertEquals(500, receiver.read.get(1).decimalPart());
    }


    private static class Receiver implements WriteChannel<ParsedLineMessage> {
        private final List<ParsedLineMessage> read = new ArrayList<>();

        @Override
        public void writeTo(ParsedLineMessage message) {
            read.add(message);
        }
    }
}
