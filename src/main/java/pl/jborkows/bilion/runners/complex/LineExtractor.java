package pl.jborkows.bilion.runners.complex;

class LineExtractor implements StepRunner.Processor<ByteChunkMessage, LineByteChunkMessage> {

    private byte[] rest;
    private int restLength;

    @Override
    public void accept(ByteChunkMessage byteChunkMessage, WriteChannel<LineByteChunkMessage> writeChannel) {
        var chunk = byteChunkMessage.chunk;
        int length = byteChunkMessage.length;

        if (rest != null) {
            System.arraycopy(chunk, 0, rest, restLength, length);
            var oldChunk = chunk;
            BytePool.release(oldChunk);
            chunk = rest;
            length = length + restLength;
            rest = null;
        }
        if (chunk[length - 1] == '\n') {
            writeChannel.writeTo(new LineByteChunkMessage(chunk, 0, length));
            return;
        }
        int i = lastElementIndex(chunk, length);
        for (; i >= firstElementIndex(chunk, length); i--) {
            if (chunk[i] == '\n') {
                rest = BytePool.WORKING.chunk();
                restLength = lastElementIndex(chunk, length) - i;
                System.arraycopy(chunk, i + 1, rest, 0, restLength);
                writeChannel.writeTo(new LineByteChunkMessage(chunk, firstElementIndex(chunk, length), i + 1));
                return;
            }
        }

        rest = BytePool.WORKING.chunk();
        System.arraycopy(chunk, 0, rest, 0, length);
        restLength = length;
        BytePool.release(chunk);

    }

    private int firstElementIndex(byte[] chunk, int length) {
        return 0;
    }

    private int lastElementIndex(byte[] chunk, int length) {
        return length - 1;
    }

    @Override
    public void finish(WriteChannel<LineByteChunkMessage> writeChannel) {

        if (rest == null) {
            return;
        }
        writeChannel.writeTo(new LineByteChunkMessage(rest, 0, restLength));
    }

}
