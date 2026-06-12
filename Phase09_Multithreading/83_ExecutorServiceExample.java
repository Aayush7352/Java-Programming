package phase09.multithreading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class ExecutorServiceExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ExecutorService example (83) ===");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 1; i <= 6; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " running on " + Thread.currentThread().getName());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("Task " + taskId + " completed");
            });
        }

        executor.shutdown();
        System.out.println("Submitted all tasks, awaiting termination...");
        boolean finished = executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("All tasks finished: " + finished);
    }
}
