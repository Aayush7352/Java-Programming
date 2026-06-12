package phase09.multithreading;

class Counter {
    private int count = 0;
    private static int staticCount = 0;

    // synchronized instance method
    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }

    // synchronized block
    public void incrementBlock() {
        synchronized (this) {
            count++;
        }
    }

    // static synchronized method
    public static synchronized void incrementStatic() {
        staticCount++;
    }

    public static synchronized int getStaticCount() {
        return staticCount;
    }
}

public class SynchronizationExample {

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
                counter.incrementBlock();
                Counter.incrementStatic();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment();
                counter.incrementBlock();
                Counter.incrementStatic();
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Instance count: " + counter.getCount());
        System.out.println("Static count: " + Counter.getStaticCount());
    }
}
