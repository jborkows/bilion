package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.ArrayList;
import java.util.Collection;
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
        lineParser.finish(receiver);
        var flat = flatten(receiver.read);
        assertFalse(flat.isEmpty());
        assertEquals("AAąAA", flat.getFirst().stationName());
        assertEquals("B C", flat.get(1).stationName());
    }
    private List<ParsedLineItem> flatten(List<ParsedLineMessage> messages){
        return messages.stream().map(ParsedLineMessage::parsedLineItems).flatMap(Collection::stream).toList();
    }

//    @Test
    void shouldParseNumber() {
        var receiver = new Receiver();
        lineParser.accept(LineByteChunkMessage.fromString("B;-12.34\nC;9.5"), receiver);
        lineParser.finish(receiver);

        var flat = flatten(receiver.read);
        assertFalse(flat.isEmpty());
        assertEquals(-12, flat.getFirst().integerPart());
        assertEquals(340, flat.getFirst().decimalPart());
        assertEquals(9, flat.get(1).integerPart());
        assertEquals(500, flat.get(1).decimalPart());
    }


    private static class Receiver implements WriteChannel<ParsedLineMessage> {
        private final List<ParsedLineMessage> read = new ArrayList<>();

        @Override
        public void writeTo(ParsedLineMessage message) {
            read.add(message);
        }
    }
}
