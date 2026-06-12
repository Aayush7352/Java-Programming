package phase09.multithreading;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

class SharedResource {
    private final Semaphore semaphore;

    public SharedResource(int permits) {
        this.semaphore = new Semaphore(permits, true);
    }

    public void use(String name) {
        try {
            System.out.println(name + " waiting...");
            semaphore.acquire();
            System.out.println(name + " acquired permit (" + semaphore.availablePermits() + " left)");
            TimeUnit.MILLISECONDS.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(name + " releasing permit");
            semaphore.release();
        }
    }
}

public class SemaphoresExample {

    public static void main(String[] args) throws InterruptedException {
        // Counting semaphore — 2 permits
        SharedResource resource = new SharedResource(2);

        Runnable task = () -> resource.use(Thread.currentThread().getName());

        Thread t1 = new Thread(task, "Thread-A");
        Thread t2 = new Thread(task, "Thread-B");
        Thread t3 = new Thread(task, "Thread-C");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // Mutex pattern — 1 permit
        Semaphore mutex = new Semaphore(1);
        mutex.acquire();
        System.out.println("Mutex acquired (critical section)");
        mutex.release();
        System.out.println("Mutex released");
    }
}
