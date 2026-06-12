package phase09.multithreading;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

class BoundedBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final int[] buffer;
    private int putIndex, takeIndex, count;

    public BoundedBuffer(int capacity) {
        buffer = new int[capacity];
    }

    public void put(int value) throws InterruptedException {
        lock.lock();
        try {
            while (count == buffer.length) {
                notFull.await();
            }
            buffer[putIndex] = value;
            putIndex = (putIndex + 1) % buffer.length;
            count++;
            System.out.println("Put: " + value + " (count=" + count + ")");
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public int take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            int value = buffer[takeIndex];
            takeIndex = (takeIndex + 1) % buffer.length;
            count--;
            System.out.println("Take: " + value + " (count=" + count + ")");
            notFull.signal();
            return value;
        } finally {
            lock.unlock();
        }
    }

    public boolean tryPut(int value, long timeoutMs) throws InterruptedException {
        if (lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            try {
                if (count == buffer.length) return false;
                buffer[putIndex] = value;
                putIndex = (putIndex + 1) % buffer.length;
                count++;
                notEmpty.signal();
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}

class LocksExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Locks example (87) ===");

        BoundedBuffer buffer = new BoundedBuffer(5);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.put(i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    buffer.take();
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        boolean success = buffer.tryPut(99, 100);
        System.out.println("tryPut (should succeed): " + success);
        System.out.println("Done.");
    }
}
