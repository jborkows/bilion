package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

class ByteChunkMessage {
    final byte[] chunk;
    final int length;

    ByteChunkMessage(byte[] chunk, int length) {
        this.chunk = chunk;
        this.length = length;
    }

    ByteChunkMessage(String text) {
        this.chunk = text.getBytes();
        this.length = this.chunk.length;
    }
}
