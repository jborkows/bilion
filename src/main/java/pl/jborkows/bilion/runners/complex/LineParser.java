package pl.jborkows.bilion.runners.complex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

class LineParser  implements StepRunner.Processor<LineByteChunkMessage, ParsedLineMessage> {
    @Override
    public  void accept(LineByteChunkMessage lineByteChunkMessage, WriteChannel<ParsedLineMessage> writeChannel) {
        byte[] lines = lineByteChunkMessage.chunk();
        try {
            int endName = 0;
            int beginNumber = 0;
            int endNumber = 0;
            int beginDecimal = 0;
            int endDecimal = 0;
            boolean minus = false;
            int i= lineByteChunkMessage.beginIndex();
            int beginName = i;
            for (; i < lineByteChunkMessage.endIndex(); i++) {
                switch (lines[i]) {
                    case '\n' ->{
                        endDecimal = i-1;
                        var message = parse(beginName,endName, beginNumber,endNumber, minus, lines,beginDecimal,endDecimal);
                        writeChannel.writeTo(message);
                        beginName = i + 1;
                        minus = false;
                    }
                    case '.' -> {
                        if(i > beginNumber){
                            endNumber = i-1;
                            beginDecimal = i+1;
                        }
                    }
                    case ';' -> {
                        endName = i - 1;
                        beginNumber = i + 1;
                    }
                    case '-' -> {
                        if(i > endName){
                            minus = true;
                            beginNumber = i+1;
                        }
                    }
                }
            }
            System.out.println("begin " + beginName + " index " + i);
            if(i > beginName){
                var message = parse(beginName,endName, beginNumber,endNumber, minus, lines, beginDecimal, i-1);
                writeChannel.writeTo(message);
            }

        } catch (Exception e){
            e.printStackTrace();
        }finally {
            BytePool.INSTANCE.release(lines);
        }

    }

    @Override
    public void finish(WriteChannel<ParsedLineMessage> writeChannel) {

    }

    private static ParsedLineMessage parse(int beginName, int endName, int beginNumber, int endNumber, boolean minus, byte[] lines, int beginDecimal, int endDecimal) {
        var name = Arrays.copyOfRange(lines, beginName,endName+1);
        int number = 0;
        int multiplier = 1;
        for(int i = beginNumber;i<=endNumber;i++){
            number = parseDigit(lines[i])+number* multiplier;
            multiplier *= 10;
        }
        number = minus ? -number : number;
      int decimal = 0;
      switch (endDecimal-beginDecimal){
          case 0 -> decimal=parseDigit(lines[beginDecimal])*100;
          case 1 -> decimal=parseDigit(lines[beginDecimal])*100 + parseDigit(lines[endDecimal])*10;
          case 2 -> decimal=parseDigit(lines[beginDecimal])*100 + parseDigit(lines[beginDecimal+1])*10+parseDigit(lines[endDecimal]);
      }
        return new ParsedLineMessage(name,endName-beginName+1, number, decimal);
    }

    private static int parseDigit(byte digit) {
        return digit - '0';
    }
}
