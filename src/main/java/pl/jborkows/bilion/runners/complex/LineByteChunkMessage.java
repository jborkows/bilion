package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

record LineByteChunkMessage(
        byte[] chunk,
        int beginIndex,
        int endIndex
) {
    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(chunk, beginIndex, endIndex + 1));
    }
}
