package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class BytePool {

    public static final int INITIAL_VALUE = 5;
    public static final int FILE_BUFFER_SIZE = 32 * 1024;
    private List<byte[]> buffer;
    private List<byte[]> used;
    private AtomicInteger capacity = new AtomicInteger(INITIAL_VALUE);
    private final Object lock = new Object();
    private  volatile boolean waiting = false;

    static BytePool INSTANCE = new BytePool();

    BytePool() {

        buffer = new ArrayList<>(INITIAL_VALUE);
        used = new ArrayList<>(INITIAL_VALUE);
        for (int i = 0; i < INITIAL_VALUE; i++) {
            buffer.add(new byte[FILE_BUFFER_SIZE]);
        }
    }

    synchronized byte[] chunk() {
        if (capacity.get() == 0) {
            try {
                System.out.println("Waiting in pool");
                waiting = true;
                lock.wait();
                waiting = false;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        var array = buffer.removeLast();
        used.add(array);
        capacity.decrementAndGet();
        return array;
    }

    synchronized void release(byte[] array) {
        if (used.remove(array)) {
            buffer.add(array);
            capacity.incrementAndGet();
            if(waiting) {
                lock.notifyAll();
            }
        }

    }
}

