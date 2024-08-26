package pl.jborkows.bilion.runners.complex;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

class StepRunner<MessageIn, MessageOut> extends Thread {
    private final ReadChannel<MessageIn> readChannel;
    private final WriteChannel<MessageOut> writeChannel;
    private final Processor<MessageIn, MessageOut> processor;
    private volatile boolean done;

    StepRunner(
            String name,
            ReadChannel<MessageIn> readChannel,
            WriteChannel<MessageOut> writeChannel,
            Processor<MessageIn, MessageOut> processor
    ) {
        this.readChannel = readChannel;
        this.writeChannel = writeChannel;
        this.processor = processor;
        setName(name);
    }

    @Override
    public void run() {
        while (true) {
            var value = readChannel.readFrom();
            if(value == null) {
                if(done){
                    processor.finish(writeChannel);
                    return;
                }
                try {
                    TimeUnit.MICROSECONDS.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
             processor.accept(value, writeChannel);
        }
    }

    void done() {
        this.done = true;
    }

    interface Processor<MessageIn, MessageOut> {
       void accept(MessageIn messageIn, WriteChannel<MessageOut> writeChannel);
       void finish(WriteChannel<MessageOut> writeChannel);

       static <In,Out> Processor<In,Out> continuesWork(BiConsumer<In,WriteChannel<Out>> processor) {
           return new Processor<In, Out>() {
               @Override
               public void accept(In in, WriteChannel<Out> writeChannel) {
                   processor.accept(in, writeChannel);
               }

               @Override
               public void finish(WriteChannel<Out> writeChannel) {

               }
           };
       }
    }

}
