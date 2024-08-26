package pl.jborkows.bilion.runners.complex;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

class LineExtractor implements StepRunner.Processor<ByteChunkMessage, LineByteChunkMessage> {

    private byte[] rest;

    @Override
    public void accept(ByteChunkMessage byteChunkMessage, WriteChannel<LineByteChunkMessage> writeChannel) {
        var chunk = byteChunkMessage.chunk;
        var begin = 0;
        for (int i = 0; i < chunk.length; i++) {
            if (chunk[i] != '\n') {
                continue;
            }
            byte[] toSend;
            if(begin == 0 && rest != null) {
                toSend = new byte[rest.length + i];
                System.arraycopy(rest, 0, toSend, 0, rest.length);
                System.arraycopy(chunk, 0, toSend, rest.length, i);
                writeChannel.writeTo(new LineByteChunkMessage(toSend,0,rest.length+i-1));
            }else {
                writeChannel.writeTo(new LineByteChunkMessage(chunk, begin, i-1));
            }
            begin = i + 1;
        }
        rest = Arrays.copyOfRange(chunk, begin, chunk.length);
    }

    @Override
    public void finish(WriteChannel<LineByteChunkMessage> writeChannel) {
        if (rest == null) {
            return;
        }
        writeChannel.writeTo(new LineByteChunkMessage(rest,0,rest.length-1));
    }
}
