package phase09.multithreading;

class Counter {
    private int count = 0;
    private static int staticCount = 0;

    public synchronized void incrementInstance() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }

    public static synchronized void incrementStatic() {
        staticCount++;
    }

    public static synchronized int getStaticCount() {
        return staticCount;
    }

    public void incrementBlock() {
        synchronized (this) {
            count++;
        }
    }
}

class SynchronizationExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Synchronization example (86) ===");

        Counter counter = new Counter();
        int threads = 10;
        int iterations = 1000;

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    counter.incrementInstance();
                    Counter.incrementStatic();
                    counter.incrementBlock();
                }
            });
            workers[i].start();
        }

        for (Thread t : workers) {
            t.join();
        }

        int expected = threads * iterations;
        System.out.println("Expected: " + expected);
        System.out.println("Instance count (sync method): " + counter.getCount());
        System.out.println("Instance count (sync block): " + counter.getCount());
        System.out.println("Static count (static sync): " + Counter.getStaticCount());
    }
}
