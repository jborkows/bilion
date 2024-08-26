package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

class LineByteChunkMessage {
    final byte[] chunk;
    final int beginIndex;
    final int endIndex;

    LineByteChunkMessage(byte[] chunk, int beginIndex, int endIndex) {
        this.chunk = chunk;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }


    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(chunk, beginIndex, endIndex+1));
    }
}
