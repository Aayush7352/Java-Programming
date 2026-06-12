package phase09.multithreading;

class MyThread extends Thread {
    private final String name;
    private final int iterations;

    public MyThread(String name, int priority, int iterations) {
        this.name = name;
        this.iterations = iterations;
        setPriority(priority);
    }

    @Override
    public void run() {
        for (int i = 1; i <= iterations; i++) {
            System.out.println(name + " (priority " + getPriority() + ") - iteration " + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(name + " interrupted");
                return;
            }
        }
    }
}

class ThreadClassExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Thread class example (81) ===");

        MyThread t1 = new MyThread("Thread-A", Thread.MIN_PRIORITY, 3);
        MyThread t2 = new MyThread("Thread-B", Thread.MAX_PRIORITY, 3);

        System.out.println("Starting threads...");
        t1.start();
        t2.start();

        System.out.println("Waiting for threads to finish (join)...");
        t1.join();
        t2.join();

        System.out.println("Both threads completed.");
    }
}
