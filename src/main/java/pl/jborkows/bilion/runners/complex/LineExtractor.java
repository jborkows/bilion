package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

class LineExtractor implements StepRunner.Processor<ByteChunkMessage, LineByteChunkMessage> {

    private byte[] rest;
    private LinesByteChunkMessage lineByteChunkMessages = new LinesByteChunkMessage();

    @Override
    public void accept(ByteChunkMessage byteChunkMessage, WriteChannel<LineByteChunkMessage> writeChannel) {
        var chunk = byteChunkMessage.chunk;
        if (rest == null) {
            int i = 0;
            for (i = chunk.length - 1; i >= 0; i--) {
                if (chunk[i] == '\n') {
                    break;
                }
            }
            if (i < 0) {
                rest = Arrays.copyOfRange(chunk, 0, chunk.length);
            } else if (i == chunk.length - 1) {
                writeChannel.writeTo(new LineByteChunkMessage(chunk, 0, chunk.length - 2));
            } else {
                writeChannel.writeTo(new LineByteChunkMessage(chunk, 0, i-1));
                rest = Arrays.copyOfRange(chunk, i+1, chunk.length);
            }
        } else {
            int i = 0;
            for (i = 0; i < chunk.length; i++) {
                if (chunk[i] == '\n') {
                    break;
                }
            }
            if(i == chunk.length-1){
                byte[] toSend = new byte[rest.length + i];
                System.arraycopy(rest, 0, toSend, 0, rest.length);
                System.arraycopy(chunk, 0, toSend, rest.length, i);
                writeChannel.writeTo(new LineByteChunkMessage(toSend, 0, rest.length + i - 1));
                rest = null;
            }else if(i == 0){
                writeChannel.writeTo(new LineByteChunkMessage(rest, 0, rest.length -1));
                writeChannel.writeTo(new LineByteChunkMessage(chunk, 1, chunk.length - 1));
                rest = null;
            }else{
                var j = 0;
                for (j = chunk.length - 1; j > i; j--) {
                    if (chunk[j] == '\n') {
                        break;
                    }
                }

                byte[] toSend = new byte[rest.length + i];
                System.arraycopy(rest, 0, toSend, 0, rest.length);
                System.arraycopy(chunk, 0, toSend, rest.length, i);
                writeChannel.writeTo(new LineByteChunkMessage(toSend, 0, rest.length + i - 1));
                if(j <= i){
                    rest = Arrays.copyOfRange(chunk, i+1, chunk.length);
                }else if(j == chunk.length - 1){
                    rest = null;
                    writeChannel.writeTo(new LineByteChunkMessage(chunk, i + 1, j-1));
                }else{
                    writeChannel.writeTo(new LineByteChunkMessage(chunk, i + 1, j-1));
                    rest = Arrays.copyOfRange(chunk, j+1, chunk.length);
                }
            }
        }
    }

    @Override
    public void finish(WriteChannel<LineByteChunkMessage> writeChannel) {

        if (rest == null) {
            return;
        }
        writeChannel.writeTo(new LineByteChunkMessage(rest, 0, rest.length - 1));
    }

}
