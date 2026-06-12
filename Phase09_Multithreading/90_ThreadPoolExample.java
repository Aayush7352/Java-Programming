package phase09.multithreading;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class CustomRejectionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("Task rejected: " + r.toString()
                + " (pool size=" + executor.getPoolSize()
                + ", queue size=" + executor.getQueue().size() + ")");
    }
}

class ThreadPoolExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ThreadPoolExecutor example (90) ===");

        // ThreadPoolExecutor with rejection handler
        int corePoolSize = 2;
        int maxPoolSize = 4;
        long keepAliveTime = 1;
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(2);
        ThreadFactory threadFactory = Thread.ofVirtual().factory();
        RejectedExecutionHandler rejectionHandler = new CustomRejectionHandler();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                rejectionHandler
        );

        executor.allowCoreThreadTimeOut(true);

        AtomicInteger taskCounter = new AtomicInteger(0);

        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            Runnable task = () -> {
                System.out.println("Task " + taskId + " running on " + Thread.currentThread().getName()
                        + " (isVirtual=" + Thread.currentThread().isVirtual() + ")");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " finished");
                taskCounter.incrementAndGet();
            };
            executor.submit(task);
            Thread.sleep(50);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Completed tasks: " + taskCounter.get());
        System.out.println("Done.");
    }
}
