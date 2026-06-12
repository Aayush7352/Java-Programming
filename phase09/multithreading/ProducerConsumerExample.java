package phase09.multithreading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProducerConsumerExample {

    public static void main(String[] args) throws InterruptedException {
        // Approach 1: wait/notify
        BoundedBuffer buffer = new BoundedBuffer(3);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try { buffer.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "Producer-wait");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try { buffer.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "Consumer-wait");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // Approach 2: BlockingQueue
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);

        Thread producer2 = new Thread(() -> {
            for (int i = 10; i <= 15; i++) {
                try {
                    queue.put(i);
                    System.out.println("BQ Produced: " + i);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "Producer-bq");

        Thread consumer2 = new Thread(() -> {
            for (int i = 10; i <= 15; i++) {
                try {
                    Integer val = queue.take();
                    System.out.println("BQ Consumed: " + val);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "Consumer-bq");

        producer2.start();
        consumer2.start();
        producer2.join();
        consumer2.join();
    }
}

class BoundedBuffer {
    private final int[] buffer;
    private int count, putIdx, takeIdx;

    BoundedBuffer(int capacity) {
        buffer = new int[capacity];
    }

    public synchronized void put(int v) throws InterruptedException {
        while (count == buffer.length) wait();
        buffer[putIdx] = v;
        putIdx = (putIdx + 1) % buffer.length;
        count++;
        System.out.println("WN Produced: " + v);
        notifyAll();
    }

    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();
        int v = buffer[takeIdx];
        takeIdx = (takeIdx + 1) % buffer.length;
        count--;
        System.out.println("WN Consumed: " + v);
        notifyAll();
        return v;
    }
}
