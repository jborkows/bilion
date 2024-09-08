package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

record LineByteChunkMessage(byte[] chunk, int beginIndex, int offset) {

    @Override
    public String toString() {
        return new String(Arrays.copyOfRange(chunk, beginIndex, beginIndex + offset));
    }

    static LineByteChunkMessage fromString(String line) {
        return new LineByteChunkMessage(line.getBytes(), 0, line.length());
    }

    public byte[] copy() {
        return Arrays.copyOfRange(chunk,beginIndex, beginIndex + offset);
    }

    public int endIndex() {
        return beginIndex + offset -1;
    }

    public void release() {
        BytePool.release(chunk);
    }
}
