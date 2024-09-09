package pl.jborkows.bilion.runners.complex;


class LineParser implements StepRunner.Processor<LineByteChunkMessage, Void> {
    private final Store store;

    LineParser(Store store) {
        this.store = store;
    }

    @Override
    public void accept(LineByteChunkMessage lineByteChunkMessage, WriteChannel<Void> writeChannel) {
        int endName = 0;
        int beginNumber = 0;
        int endNumber = 0;
        int beginDecimal = 0;
        int endDecimal = 0;
        boolean minus = false;
        int i = lineByteChunkMessage.beginIndex();
        int beginName = lineByteChunkMessage.beginIndex();

        int length = lineByteChunkMessage.endIndex();
        for (; i <= length; i++) {
            switch (lineByteChunkMessage.chunk()[i]) {
                case '\n' -> {
                    endDecimal = i - 1;
                     parse(beginName, endName, beginNumber, endNumber, minus, lineByteChunkMessage.chunk(), beginDecimal, endDecimal);
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
             parse(beginName, endName, beginNumber, endNumber, minus, lineByteChunkMessage.chunk(), beginDecimal, i - 1);
        }

        lineByteChunkMessage.release();
    }


    @Override
    public void finish(WriteChannel<Void> writeChannel) {
        store.gather();
    }

    private void parse(int beginName, int endName, int beginNumber, int endNumber, boolean minus, byte[] lines, int beginDecimal, int endDecimal) {
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
        store.register(lines,beginName,endName-beginName+1, (minus?-1:1)*(number*1000+decimal));
    }

    private static int parseDigit(byte digit) {
        return digit - '0';
    }
}
