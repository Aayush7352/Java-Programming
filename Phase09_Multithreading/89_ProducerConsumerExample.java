package phase09.multithreading;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class SharedBuffer {
    private final int[] buffer;
    private int count, putIndex, takeIndex;

    public SharedBuffer(int capacity) {
        buffer = new int[capacity];
    }

    public synchronized void produce(int value) throws InterruptedException {
        while (count == buffer.length) {
            wait();
        }
        buffer[putIndex] = value;
        putIndex = (putIndex + 1) % buffer.length;
        count++;
        System.out.println("Produced: " + value);
        notifyAll();
    }

    public synchronized int consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        int value = buffer[takeIndex];
        takeIndex = (takeIndex + 1) % buffer.length;
        count--;
        System.out.println("Consumed: " + value);
        notifyAll();
        return value;
    }
}

class ProducerConsumerExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Producer-Consumer example (89) ===");

        // Part 1: wait/notify
        System.out.println("--- wait/notify ---");
        SharedBuffer buffer = new SharedBuffer(5);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.produce(i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.consume();
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // Part 2: BlockingQueue
        System.out.println("--- BlockingQueue ---");
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(3);

        Thread qProducer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    queue.put(i);
                    System.out.println("Queue put: " + i + " (size=" + queue.size() + ")");
                    Thread.sleep(80);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread qConsumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    Integer value = queue.poll(1, TimeUnit.SECONDS);
                    System.out.println("Queue take: " + value + " (size=" + queue.size() + ")");
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        qProducer.start();
        qConsumer.start();
        qProducer.join();
        qConsumer.join();

        System.out.println("Done.");
    }
}
