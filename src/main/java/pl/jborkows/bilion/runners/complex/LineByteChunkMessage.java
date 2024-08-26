package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

class LineByteChunkMessage {
    final byte[] line;

    LineByteChunkMessage(byte[] line ) {
        this.line = line;
    }

    @Override
    public String toString() {
        return new String(line);
    }
}
