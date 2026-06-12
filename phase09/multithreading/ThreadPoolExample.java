package phase09.multithreading;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExample {

    public static void main(String[] args) throws InterruptedException {
        // Custom ThreadPoolExecutor
        int corePoolSize = 2;
        int maxPoolSize = 4;
        long keepAliveTime = 1;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(3);

        RejectedExecutionHandler rejectionHandler = (r, executor) ->
                System.out.println("Rejected: " + r.toString());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                Executors.defaultThreadFactory(),
                rejectionHandler
        );

        AtomicInteger taskCounter = new AtomicInteger(1);

        for (int i = 0; i < 10; i++) {
            int taskId = taskCounter.getAndIncrement();
            executor.submit(() -> {
                System.out.println("Task " + taskId + " running on " + Thread.currentThread().getName());
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Virtual threads (Java 21+)
        System.out.println("\n--- Virtual Threads ---");
        try (var virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 5; i++) {
                int taskId = i;
                virtualExecutor.submit(() -> {
                    System.out.println("Virtual task " + taskId + " on " + Thread.currentThread());
                });
            }
        }
        System.out.println("Virtual thread pool shutdown complete.");
    }
}
