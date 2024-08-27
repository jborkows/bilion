package pl.jborkows.bilion.runners.complex;

import java.util.Arrays;

class LineExtractor implements StepRunner.Processor<ByteChunkMessage, LineByteChunkMessages> {

    private byte[] rest;
    private LineByteChunkMessages lineByteChunkMessages = new LineByteChunkMessages();

    @Override
    public void accept(ByteChunkMessage byteChunkMessage, WriteChannel<LineByteChunkMessages> writeChannel) {
        var chunk = byteChunkMessage.chunk;
        var begin = 0;
        for (int i = 0; i < chunk.length; i++) {
            if (chunk[i] != '\n') {
                continue;
            }
            byte[] toSend;
            if(begin == 0 && rest != null) {
                toSend = new byte[rest.length + i];
                System.arraycopy(rest, 0, toSend, 0, rest.length);
                System.arraycopy(chunk, 0, toSend, rest.length, i);
                lineByteChunkMessages.add( new LineByteChunkMessage(toSend,0,rest.length+i-1));
            }else {
                lineByteChunkMessages.add(new LineByteChunkMessage(chunk, begin, i-1));
            }
            begin = i + 1;
            if(lineByteChunkMessages.full()){
                writeChannel.writeTo(lineByteChunkMessages);
                lineByteChunkMessages = new LineByteChunkMessages();
            }
        }
        rest = Arrays.copyOfRange(chunk, begin, chunk.length);
    }

    @Override
    public void finish(WriteChannel<LineByteChunkMessages> writeChannel) {
        fillTail();
        if(lineByteChunkMessages.isEmpty()){
            return;
        }
        writeChannel.writeTo(lineByteChunkMessages);
    }

    private void fillTail() {
        if (rest == null) {
            return ;
        }
        if(rest.length == 0) {
            return;
        }
        lineByteChunkMessages.add(new LineByteChunkMessage(rest,0,rest.length-1));
    }
}
