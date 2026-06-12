package phase09.multithreading;

import java.util.concurrent.Semaphore;

class ConnectionPool {
    private final Semaphore semaphore;
    private int activeConnections = 0;

    public ConnectionPool(int maxConnections) {
        this.semaphore = new Semaphore(maxConnections);
    }

    public void acquire() throws InterruptedException {
        semaphore.acquire();
        synchronized (this) {
            activeConnections++;
            System.out.println("Connection acquired. Active: " + activeConnections
                    + " (available permits: " + semaphore.availablePermits() + ")");
        }
    }

    public void release() {
        synchronized (this) {
            activeConnections--;
            System.out.println("Connection released. Active: " + activeConnections
                    + " (available permits: " + semaphore.availablePermits() + ")");
        }
        semaphore.release();
    }
}

class SemaphoresExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Semaphore example (88) ===");

        ConnectionPool pool = new ConnectionPool(3);
        int numThreads = 10;

        Thread[] workers = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            workers[i] = new Thread(() -> {
                try {
                    pool.acquire();
                    System.out.println("  Worker " + id + " using connection...");
                    Thread.sleep((long) (Math.random() * 300));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    pool.release();
                }
            });
            workers[i].start();
        }

        for (Thread t : workers) {
            t.join();
        }

        System.out.println("All workers done.");
    }
}
