package pl.jborkows.bilion.runners.complex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class ParsedLineTest {
    private LineParser lineParser;
    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
        lineParser = new LineParser(store);
    }


    @Test
    void shouldParseName() {
        lineParser.accept(LineByteChunkMessage.fromString("AAąAA;12.34\nB C;12.34"), WriteChannel.none());
        lineParser.finish(WriteChannel.none());
        var flat = store.getData();
        assertFalse(flat.isEmpty());

        assertTrue(flat.stream().anyMatch(d-> Objects.equals(d.name(), "AAąAA")));
        assertTrue(flat.stream().anyMatch(d-> Objects.equals(d.name(), "B C")));
    }

    @Test
    void shouldParseNumber() {
        lineParser.accept(LineByteChunkMessage.fromString("B;-12.34\nC;9.5"), WriteChannel.none());
        lineParser.finish(WriteChannel.none());

        var flat = store.getData();
        assertFalse(flat.isEmpty());

        System.out.println(flat);
        assertTrue(flat.stream().anyMatch(d->d.max==-12*1000-340));
        assertTrue(flat.stream().anyMatch(d->d.max==9500));
    }

}
