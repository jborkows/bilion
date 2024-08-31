package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

record LineByteChunkMessage(byte[] chunk, int beginIndex, int endIndex) {

    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(chunk, beginIndex, endIndex + 1));
    }

    static LineByteChunkMessage fromString(String line) {
        return new LineByteChunkMessage(line.getBytes(), 0, line.length());
    }

}
