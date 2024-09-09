package pl.jborkows.bilion.runners.complex;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;


interface ReadChannel<Message>{
    Message readFrom();
}

interface WriteChannel<Message>{
    void writeTo(Message message);
    static WriteChannel<Void> none(){
        return _a -> {
        };
    }
}
class MessageChannel<Message> implements ReadChannel<Message>, WriteChannel<Message> {

    private final ConcurrentLinkedDeque<Message> queues;
    private final AtomicInteger counter;

    MessageChannel(String channelName) {
        this();
    }

    private static RejectionPolicy retry(String channelName){

    return new RejectionPolicy() {

        @Override
        public RejectionStep onRejection() {
//            System.out.println(channelName + "-> Retrying...");
            return RejectionStep.RETRY;
        }

        @Override
        public void success() {

        }
    };}

    MessageChannel() {
        this.queues = new ConcurrentLinkedDeque<>();
        this.counter = new AtomicInteger(0);
    }

    @Override
    public Message readFrom() {
        return queues.pollFirst();
    }

    @Override
    public void writeTo(Message message) {
        queues.addLast(message);
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
