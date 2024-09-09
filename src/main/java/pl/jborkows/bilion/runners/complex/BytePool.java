package pl.jborkows.bilion.runners.complex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class BytePool {

    public static final int INITIAL_VALUE = 2000;
    public static final int FILE_BUFFER_SIZE = 32 * 1024;
    private final String name;
    private Map<Integer,byte[]> buffer;
    private Map<Integer,byte[]> used;
    private List<Integer> keys;
    private AtomicInteger capacity;
    private volatile boolean waiting = false;

    static BytePool INSTANCE = new BytePool(INITIAL_VALUE * 5, FILE_BUFFER_SIZE, "File reader");
    static BytePool WORKING = new BytePool(INITIAL_VALUE * 8, FILE_BUFFER_SIZE * 2, "New line");

    private BytePool(int poolSize, int size, String name) {
        capacity = new AtomicInteger(poolSize);
        buffer = new HashMap<>(poolSize);
        used = new HashMap<>(poolSize);
        keys = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            byte[] bytes = new byte[size];
            int key = System.identityHashCode(bytes);
            buffer.put(key,bytes);
            keys.add(key);
        }
        this.name = name;
    }

    public static void release(byte[] oldChunk) {
        if (!INSTANCE.releaseChunk(oldChunk)) {
            WORKING.releaseChunk(oldChunk);
        }
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
        var array = keys.removeLast();
        var chunk = buffer.remove(array);
        used.put(array,chunk);
        capacity.decrementAndGet();
        return chunk;
    }

    private synchronized boolean releaseChunk(byte[] array) {
        int key = System.identityHashCode(array);
        if (used.remove(key)!=null) {
            buffer.put(key,array);
            keys.add(key);
            capacity.incrementAndGet();
            if (waiting) {
                notifyAll();
            }
            return true;
        }
        return false;

    }
}

