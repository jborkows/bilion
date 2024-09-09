package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.List;

class LineParser implements StepRunner.Processor<LineByteChunkMessage, ParsedLineMessage> {
    private final int batchSize = 1000;
    private List<ParsedLineItem> items = new ArrayList<>(batchSize);
    @Override
    public void accept(LineByteChunkMessage lineByteChunkMessage, WriteChannel<ParsedLineMessage> writeChannel) {
        var lines = lines(lineByteChunkMessage);
        int endName = 0;
        int beginNumber = 0;
        int endNumber = 0;
        int beginDecimal = 0;
        int endDecimal = 0;
        boolean minus = false;
        int i = 0;
        int beginName = i;
        for (; i < lines.length; i++) {
            switch (lines[i]) {
                case '\n' -> {
                    endDecimal = i - 1;
                    var message = parse(beginName, endName, beginNumber, endNumber, minus, lines, beginDecimal, endDecimal);
                    items.add(message);
                    if(items.size() >= batchSize) {
                        writeChannel.writeTo(new ParsedLineMessage(items));
                        items = new ArrayList<>(batchSize);
                    }
                    beginName = i + 1;
                    minus = false;
                }
                case '.' -> {
                    if (i > beginNumber) {
                        endNumber = i - 1;
                        beginDecimal = i + 1;
                    }
                }
                case ';' -> {
                    endName = i - 1;
                    beginNumber = i + 1;
                }
                case '-' -> {
                    if (i > endName) {
                        minus = true;
                        beginNumber = i + 1;
                    }
                }
            }
        }
        if (i > beginName) {
            var message = parse(beginName, endName, beginNumber, endNumber, minus, lines, beginDecimal, i - 1);
            items.add(message);
        }
    }
    private byte[] lines(LineByteChunkMessage lineByteChunkMessage) {
        byte[] lines = lineByteChunkMessage.copy();
        lineByteChunkMessage.release();
        return lines;
    }

    @Override
    public void finish(WriteChannel<ParsedLineMessage> writeChannel) {
        if(!items.isEmpty()) {
            writeChannel.writeTo(new ParsedLineMessage(items));
        }
    }

    private static ParsedLineItem parse(int beginName, int endName, int beginNumber, int endNumber, boolean minus, byte[] lines, int beginDecimal, int endDecimal) {
        int number = switch (endNumber-beginNumber){
            case 0 -> parseDigit(lines[beginNumber]);
            case 1 -> parseDigit(lines[beginNumber])*10+parseDigit(lines[beginNumber+1]);
            default -> 0;
        };
        int decimal =switch (endDecimal - beginDecimal) {
            case 0 ->  parseDigit(lines[beginDecimal]) * 100;
            case 1 ->  parseDigit(lines[beginDecimal]) * 100 + parseDigit(lines[endDecimal]) * 10;
            case 2 ->
                     parseDigit(lines[beginDecimal]) * 100 + parseDigit(lines[beginDecimal + 1]) * 10 + parseDigit(lines[endDecimal]);
            default -> 0;
        };
        return new ParsedLineItem(lines, beginName, endName - beginName+1, (minus?-1:1)*(number*1000+decimal));
    }

    private static int parseDigit(byte digit) {
        return digit - '0';
    }
}
