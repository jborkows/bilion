package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class BytePool {

    public static final int INITIAL_VALUE = 2000;
    public static final int FILE_BUFFER_SIZE = 32 * 1024;
    private final String name;
    private List<byte[]> buffer;
    private List<byte[]> used;
    private AtomicInteger capacity;
    private  volatile boolean waiting = false;

    static BytePool INSTANCE = new BytePool(INITIAL_VALUE*5,FILE_BUFFER_SIZE, "File reader");
    static BytePool WORKING = new BytePool(INITIAL_VALUE*8,FILE_BUFFER_SIZE*2, "New line");

    private BytePool(int poolSize,int size, String name) {
        capacity = new AtomicInteger(poolSize);
        buffer = new ArrayList<>(poolSize);
        used = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            buffer.add(new byte[size]);
        }
        this.name = name;
    }

    public static void release(byte[] oldChunk) {
        if(!INSTANCE.releaseChunk(oldChunk))  WORKING.releaseChunk(oldChunk);
    }

    synchronized byte[] chunk() {
        if (capacity.get() == 0) {
            try {
                System.out.println("Waiting in pool -> " + name);
                waiting = true;
                wait();
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

    private synchronized boolean releaseChunk(byte[] array) {
        if (used.remove(array)) {
            buffer.add(array);
            capacity.incrementAndGet();
            if(waiting) {
                notifyAll();
            }
            return true;
        }
        return false;

    }
}

