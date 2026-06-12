package phase09.multithreading;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class SharedBuffer {
    private int value;
    private boolean produced;
    private final ReentrantLock lock = new ReentrantLock(true); // fair
    private final Condition canProduce = lock.newCondition();
    private final Condition canConsume = lock.newCondition();

    public void put(int v) throws InterruptedException {
        lock.lock();
        try {
            while (produced) canProduce.await();
            value = v;
            produced = true;
            System.out.println("Produced: " + v);
            canConsume.signal();
        } finally {
            lock.unlock();
        }
    }

    public int get() throws InterruptedException {
        lock.lock();
        try {
            while (!produced) canConsume.await();
            produced = false;
            System.out.println("Consumed: " + value);
            canProduce.signal();
            return value;
        } finally {
            lock.unlock();
        }
    }
}

public class LocksExample {

    public static void main(String[] args) throws InterruptedException {
        SharedBuffer buf = new SharedBuffer();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try { buf.put(i); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                try { buf.get(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // tryLock example
        ReentrantLock testLock = new ReentrantLock();
        boolean acquired = testLock.tryLock();
        System.out.println("tryLock acquired: " + acquired);
        if (acquired) testLock.unlock();

        System.out.println("Done");
    }
}
