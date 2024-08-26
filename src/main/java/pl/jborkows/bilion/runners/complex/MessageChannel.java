package pl.jborkows.bilion.runners.complex;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;


interface ReadChannel<Message>{
    Message readFrom();
}

interface WriteChannel<Message>{
    void writeTo(Message message);
    static WriteChannel<Void> none(){
        return _ -> {
        };
    }
}
class MessageChannel<Message> implements ReadChannel<Message>, WriteChannel<Message> {

    private final RejectionPolicy rejectionPolicy;
    private final ConcurrentLinkedDeque<Message> queues;
    private final AtomicInteger counter;
    private final int maxSize;

    MessageChannel(String channelName,  int maxSize) {
        this(retry(channelName), maxSize);
    }

    private static RejectionPolicy retry(String channelName){

    return new RejectionPolicy() {

        @Override
        public RejectionStep onRejection() {
            System.out.println(channelName + "-> Retrying...");
            return RejectionStep.RETRY;
        }

        @Override
        public void success() {

        }
    };}

    MessageChannel(RejectionPolicy rejectionPolicy,  int maxSize) {
        this.rejectionPolicy = rejectionPolicy;
        this.queues = new ConcurrentLinkedDeque<>();
        this.counter = new AtomicInteger(0);
        this.maxSize = maxSize;
    }

    @Override
    public Message readFrom() {
        var value =  queues.pollFirst();
        if(value != null){
            counter.decrementAndGet();
        }
        return value;
    }

    @Override
    public void writeTo(Message message) {
        while (true) {
            if (counter.get() > maxSize) {
                switch (rejectionPolicy.onRejection()){
                    case QUIT -> {
                        System.exit(-1);
                    }
                    case RETRY -> {
                        try {
                            TimeUnit.MICROSECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                continue;
            }
            queues.addLast(message);
            counter.incrementAndGet();
            break;
        }
    }
}

interface RejectionPolicy {
    RejectionStep onRejection();
    void success();
}
enum RejectionStep {
    QUIT,
    RETRY
}
