package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

class LinesByteChunkMessage {
    private static final int SIZE = 10;
    final List<LineByteChunkMessage> chunks = new ArrayList<>(SIZE);

    void add(LineByteChunkMessage chunk) {
        chunks.add(chunk);
    }

    void forEach(Consumer<LineByteChunkMessage> consumer) {
        chunks.forEach(consumer);
    }

    boolean isEmpty() {
        return chunks.isEmpty();
    }

    boolean full() {
        return SIZE == chunks.size();
    }
}

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
