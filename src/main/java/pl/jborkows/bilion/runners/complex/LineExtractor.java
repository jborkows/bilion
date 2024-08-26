package pl.jborkows.bilion.runners.complex;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

class LineExtractor implements StepRunner.Processor<ByteChunkMessage, LineByteChunkMessage> {

    @Override
    public void accept(ByteChunkMessage byteChunkMessage, WriteChannel<LineByteChunkMessage> writeChannel) {
        if(byteChunkMessage.chunk[byteChunkMessage.chunk.length - 1] == '\n') {
            writeChannel.writeTo(new LineByteChunkMessage(byteChunkMessage.chunk));
        }
    }

    @Override
    public void finish() {

    }
}
