package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.List;

class ParsedLinesMessage {
    private List<ParsedLine> parsedLines = new ArrayList<>(10);
}

record ParsedLine(byte[] name, int lentgh, boolean minus, short integerPart, int decimalPart){}
